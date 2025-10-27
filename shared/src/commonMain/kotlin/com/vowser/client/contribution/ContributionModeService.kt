package com.vowser.client.contribution

import io.github.aakira.napier.Napier
import com.vowser.client.logging.Tags
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlin.math.min
import kotlin.time.Duration.Companion.minutes

class ContributionModeService(
    private val coroutineScope: CoroutineScope,
    private val onSendMessage: suspend (ContributionMessage) -> Unit,
    private val onUILog: ((Int, String, String?, String?) -> Unit)? = null
) {
    private var currentSession: ContributionSession? = null
    private val stepBuffer = mutableListOf<ContributionStep>()
    private var lastSentIndex = 0

    // 타이핑 디바운싱 관련 프로퍼티
    private var typingDebounceJob: Job? = null
    private var pendingTypingStep: ContributionStep? = null
    private val typingDebounceTimeMs = ContributionConstants.TYPING_DEBOUNCE_TIME_MS

    // 중복 제거 관련 프로퍼티
    private val recentActions = mutableMapOf<String, Long>()
    private val deduplicationWindowMs = ContributionConstants.DEDUPLICATION_WINDOW_MS
    
    private val _status = MutableStateFlow(ContributionStatus.INACTIVE)
    val status: StateFlow<ContributionStatus> = _status.asStateFlow()
    
    private val _currentStepCount = MutableStateFlow(0)
    val currentStepCount: StateFlow<Int> = _currentStepCount.asStateFlow()
    
    private val _currentTask = MutableStateFlow("")
    val currentTask: StateFlow<String> = _currentTask.asStateFlow()

    private var timeoutJob : Job? = null
    private val sessionTimeout = ContributionConstants.SESSION_TIMEOUT_MINUTES.minutes
    
    fun startSession(task: String) {
        if (currentSession?.isActive == true) {
            Napier.w("Contribution session already active - sessionId: ${currentSession?.sessionId}, task: ${currentSession?.task}", tag = Tags.BROWSER_AUTOMATION)
            return
        }
        
        val sanitizedTask = ContributionDataValidator.sanitizeString(task, ContributionConstants.MAX_TITLE_LENGTH)
        if (sanitizedTask.isBlank()) {
            Napier.e("Invalid task provided for contribution session", tag = Tags.BROWSER_AUTOMATION)
            return
        }
        
        currentSession = ContributionSession(task = sanitizedTask)
        stepBuffer.clear()
        lastSentIndex = 0
        
        _status.value = ContributionStatus.RECORDING
        _currentTask.value = sanitizedTask
        _currentStepCount.value = 0
        
        startTimeoutTimer()
        
        Napier.i("🚀 Contribution session started - sessionId: ${currentSession?.sessionId}, task: '$sanitizedTask', timeout: $sessionTimeout", tag = Tags.BROWSER_AUTOMATION)
    }
    
    fun recordStep(step: ContributionStep) {
        val session = currentSession
        if (session == null || !session.isActive) {
            Napier.w("No active contribution session to record step", tag = Tags.BROWSER_AUTOMATION)
            return
        }

        // 데이터 검증 및 정화
        val sanitizedStep = ContributionDataValidator.sanitizeContributionStep(step)
        if (sanitizedStep == null) {
            Napier.w("Invalid contribution step discarded: ${step.action} on ${step.url}", tag = Tags.BROWSER_AUTOMATION)
            return
        }

        // 중복 체크
        if (isDuplicateAction(sanitizedStep)) {
            return
        }

        if (sanitizedStep.action == "type") {
            recordTypingStepWithDebounce(sanitizedStep)
        } else {
            // 다른 액션이 들어오면 대기 중인 타이핑 스텝 즉시 완료
            flushPendingTypingStep()
            recordStepImmediately(sanitizedStep)
        }
    }
    
    /**
     * URL fragment, query parameter 제거
     */
    private fun normalizeUrl(url: String): String {
        return url.substringBefore('#')
            .substringBefore('?')
            .lowercase()
    }

    private fun pruneRecentActions(currentTime: Long) {
        val iterator = recentActions.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val key = entry.key
            val window = if (key.startsWith("navigate:")) 5000L else deduplicationWindowMs
            if (currentTime - entry.value > window) {
                iterator.remove()
            }
        }
    }

    /**
     * 중복 액션 체크
     */
    private fun isDuplicateAction(step: ContributionStep): Boolean {
        val normalizedUrl = normalizeUrl(step.url)
        val actionKey = "${step.action}:$normalizedUrl"
        val now = step.timestamp
        val lastTime = recentActions[actionKey] ?: 0L

        pruneRecentActions(now)

        return if (now - lastTime < deduplicationWindowMs) {
            when (step.action) {
                "click" -> {
                    // 클릭 우선
                    recentActions["navigate:$normalizedUrl"] = 0L
                    recentActions["new_tab:$normalizedUrl"] = 0L
                    recentActions[actionKey] = now
                    false
                }
                "navigate" -> {
                    // 최근에 클릭이나 타입 액션이 있었다면 무시
                    val recentClick = recentActions["click:$normalizedUrl"] ?: 0L
                    val recentType = recentActions["type:$normalizedUrl"] ?: 0L
                    val recentNavigate = recentActions["navigate:$normalizedUrl"] ?: 0L

                    when {
                        now - recentClick < deduplicationWindowMs -> true
                        now - recentType < deduplicationWindowMs -> true
                        // 같은 URL로 반복 navigate는 더 길게 억제
                        now - recentNavigate < 5000L -> true
                        else -> {
                            recentActions[actionKey] = now
                            false
                        }
                    }
                }
                "new_tab" -> {
                    // 최근에 클릭 액션이 있었다면 무시
                    val recentClick = recentActions["click:$normalizedUrl"] ?: 0L
                    if (now - recentClick < deduplicationWindowMs) {
                        true
                    } else {
                        recentActions[actionKey] = now
                        false
                    }
                }
                "type" -> {
                    val isAddressBarType = step.selector?.contains("address") == true ||
                                         step.selector?.contains("url") == true ||
                                         step.htmlAttributes?.get("type") == "url"

                    if (isAddressBarType) {
                        if (now - lastTime < 3000L) {
                            true
                        } else {
                            recentActions[actionKey] = now
                            false
                        }
                    } else {
                        recentActions[actionKey] = now
                        false
                    }
                }
                else -> {
                    recentActions[actionKey] = now
                    false
                }
            }
        } else { // 시간 창 밖이므로 기록
            recentActions[actionKey] = now
            false
        }
    }

    /**
     * 타이핑 스텝을 디바운싱하여 기록
     */
    private fun recordTypingStepWithDebounce(step: ContributionStep) {
        // 이전 타이핑 Job 취소
        typingDebounceJob?.cancel()

        if (step.htmlAttributes?.get("key")?.lowercase() == "enter" ||
            step.htmlAttributes?.get("keyCode") == "13" ||
            step.htmlAttributes?.get("which") == "13" ||
            step.htmlAttributes?.get("code")?.lowercase() == "enter") {
            // Enter는 타이핑 완료 시그널로만 사용: 대기 중인 타이핑 스텝만 기록하고 Enter 스텝 자체는 건너뜀
            pendingTypingStep?.let { recordTypeStepMergingPreviousClick(it) }
            pendingTypingStep = null
            return
        }

        pendingTypingStep = step

        val effectiveDebounce = if (isSearchField(step)) typingDebounceTimeMs + 500 else typingDebounceTimeMs

        typingDebounceJob = coroutineScope.launch {
            delay(effectiveDebounce)
            pendingTypingStep?.let { pendingStep ->
                recordTypeStepMergingPreviousClick(pendingStep)
                pendingTypingStep = null
            }
        }
        
        Napier.d("Typing step debounced: ${step.htmlAttributes?.get("text") ?: step.title}", tag = Tags.BROWSER_AUTOMATION)
    }
    
    /**
     * 대기 중인 타이핑 스텝을 즉시 처리
     */
    private fun flushPendingTypingStep() {
        typingDebounceJob?.cancel()
        pendingTypingStep?.let { pendingStep ->
            recordTypeStepMergingPreviousClick(pendingStep)
            pendingTypingStep = null
            Napier.d("Flushed pending typing step due to other action", tag = Tags.BROWSER_AUTOMATION)
        }
    }

    /**
     * 직전 동일 요소 클릭(step.action == "click")을 type과 병합하여 제거
     */
    private fun recordTypeStepMergingPreviousClick(step: ContributionStep) {
        val session = currentSession ?: run {
            recordStepImmediately(step)
            return
        }

        if (session.steps.isNotEmpty()) {
            val last = session.steps.last()
            val sameUrl = normalizeUrl(last.url) == normalizeUrl(step.url)
            val sameTarget = (last.selector?.trim() == step.selector?.trim())
            val closeInTime = (step.timestamp - last.timestamp) in 0..3000
            if (last.action == "click" && sameUrl && sameTarget && closeInTime) {
                // 직전 클릭 스텝 제거 (session + buffer)
                session.steps.removeLast()
                val indexInBuffer = stepBuffer.indexOfLast { it.action == "click" && it.selector?.trim() == step.selector?.trim() && normalizeUrl(it.url) == normalizeUrl(step.url) }
                if (indexInBuffer >= 0) {
                    stepBuffer.removeAt(indexInBuffer)
                }
                _currentStepCount.value = session.steps.size
                Napier.i("Merged previous click into type for selector: ${step.selector}", tag = Tags.BROWSER_AUTOMATION)
            }
        }

        recordStepImmediately(step)
    }

    private fun isSearchField(step: ContributionStep): Boolean {
        val attrs = step.htmlAttributes ?: return false
        val type = attrs["type"]?.lowercase()
        val placeholder = attrs["placeholder"]?.lowercase()
        val ariaLabel = attrs["aria-label"]?.lowercase()
        return type == "search" ||
                placeholder?.contains("검색") == true || placeholder?.contains("search") == true ||
                ariaLabel?.contains("검색") == true || ariaLabel?.contains("search") == true
    }
    
    /**
     * 스텝을 즉시 기록
     */
    private fun recordStepImmediately(step: ContributionStep) {
        val session = currentSession ?: return
        
        session.steps.add(step)
        stepBuffer.add(step)
        _currentStepCount.value = session.steps.size
        
        // UI 로그 업데이트
        val elementName = step.htmlAttributes?.get("text") 
            ?: step.htmlAttributes?.get("value")
            ?: step.htmlAttributes?.get("placeholder")
            ?: step.selector?.split(".")?.lastOrNull()?.replace(Regex("[^\\w가-힣]"), "")
        
        onUILog?.invoke(session.steps.size, step.action, elementName, step.url)
        
        Napier.i("Step ${session.steps.size}: [${step.action}] ${step.selector ?: "N/A"} - ${step.htmlAttributes?.get("text") ?: step.title}", tag = Tags.BROWSER_AUTOMATION)

        if (stepBuffer.size >= ContributionConstants.BATCH_SIZE) {
            coroutineScope.launch {
                sendBufferedSteps(isPartial = true)
            }
        }
    }
    
    suspend fun endSession() {
        val session = currentSession ?: run {
            Napier.w("No active session to end", tag = Tags.BROWSER_AUTOMATION)
            return
        }
        
        if (!session.isActive) {
            Napier.w("Session already ended - sessionId: ${session.sessionId}", tag = Tags.BROWSER_AUTOMATION)
            return
        }

        flushPendingTypingStep()
        
        Napier.i("🏁 Ending contribution session - sessionId: ${session.sessionId}, totalSteps: ${session.steps.size}, bufferSize: ${stepBuffer.size}", tag = Tags.BROWSER_AUTOMATION)
        
        session.isActive = false
        _status.value = ContributionStatus.SENDING
        try {
            sendBufferedSteps(isPartial = false, isComplete = true)
            _status.value = ContributionStatus.COMPLETED
            Napier.i("✅ Contribution session completed successfully - sessionId: ${session.sessionId}, duration: ${kotlinx.datetime.Clock.System.now().toEpochMilliseconds() - session.startTime}ms", tag = Tags.BROWSER_AUTOMATION)
        } catch (e: Exception) {
            _status.value = ContributionStatus.ERROR
            Napier.e("❌ Failed to complete contribution session - sessionId: ${session.sessionId}, error: ${e.message}", e, tag = Tags.BROWSER_AUTOMATION)
            throw e
        }
    }
    
    private suspend fun sendBufferedSteps(isPartial: Boolean, isComplete: Boolean = false) {
        val session = currentSession ?: return
        
        if (stepBuffer.isEmpty()) return
        
        val stepsToSend = stepBuffer.toList()
        
        val message = ContributionMessage(
            sessionId = session.sessionId,
            taskIntent = session.task,
            steps = stepsToSend,
            isPartial = isPartial,
            isComplete = isComplete,
            totalSteps = session.steps.size
        )
        
        var retryCount = 0
        val maxRetries = ContributionConstants.MAX_RETRIES
        val retryDelays = listOf(ContributionConstants.RETRY_DELAY_1, ContributionConstants.RETRY_DELAY_2, ContributionConstants.RETRY_DELAY_3)

        while (retryCount <= maxRetries) {
            try {
                onSendMessage(message)

                val removeCount = min(stepBuffer.size, stepsToSend.size)
                if (removeCount > 0) {
                    stepBuffer.subList(0, removeCount).clear()
                }
                lastSentIndex = session.steps.size

                Napier.i("✅ Sent ${stepsToSend.size} steps (partial: $isPartial, complete: $isComplete), sessionId: ${message.sessionId}", tag = Tags.BROWSER_AUTOMATION)
                return

            } catch (e: Exception) {
                retryCount++
                if (retryCount <= maxRetries) {
                    val delayMs = retryDelays.getOrElse(retryCount - 1) { 5000L }
                    Napier.w("Failed to send contribution steps (attempt $retryCount/$maxRetries): ${e.message}. Retrying in ${delayMs}ms...", tag = Tags.BROWSER_AUTOMATION)
                    delay(delayMs)
                } else {
                    Napier.e("Failed to send contribution steps after $maxRetries attempts: ${e.message}", e, tag = Tags.BROWSER_AUTOMATION)
                    throw e
                }
            }
        }
    }
    
    private fun startTimeoutTimer() {
        timeoutJob?.cancel()
        timeoutJob = coroutineScope.launch {
            delay(sessionTimeout)
            if (currentSession?.isActive == true) {
                Napier.i("Session timeout reached, auto-ending session", tag = Tags.BROWSER_AUTOMATION)
                endSessionWithTimeout()
            }
        }
    }
    
    private fun endSessionWithTimeout() {
        coroutineScope.launch {
            try {
                endSession()
                Napier.i("Contribution session auto-completed due to timeout", tag = Tags.BROWSER_AUTOMATION)
            } catch (e: Exception) {
                Napier.e("Failed to auto-complete contribution session: ${e.message}", e, tag = Tags.BROWSER_AUTOMATION)
            }
        }
    }
    
    fun isSessionActive(): Boolean = currentSession?.isActive == true
    
    fun getCurrentSessionId(): String? = currentSession?.sessionId

    fun getCurrentSession(): ContributionSession? = currentSession

    /**
     * 외부에서 호출 가능한 타이핑 스텝 플러시 메서드
     * 세션 저장 전에 대기 중인 타이핑 스텝을 완료시키기 위해 사용
     */
    fun flushPendingTypingStepForSave() {
        flushPendingTypingStep()
    }

    fun resetSession() {
        timeoutJob?.cancel()
        timeoutJob = null
        val previousSessionId = currentSession?.sessionId
        val previousSteps = currentSession?.steps?.size ?: 0
        typingDebounceJob?.cancel()
        pendingTypingStep = null
        recentActions.clear()
        currentSession = null
        stepBuffer.clear()
        lastSentIndex = 0
        _status.value = ContributionStatus.INACTIVE
        _currentStepCount.value = 0
        _currentTask.value = ""

        Napier.i("🔄 Session reset - previousSessionId: ${previousSessionId ?: "none"}, previousSteps: $previousSteps", tag = Tags.BROWSER_AUTOMATION)
    }

    fun clearPendingMessages() {
        stepBuffer.clear()
        lastSentIndex = 0
    }
}
