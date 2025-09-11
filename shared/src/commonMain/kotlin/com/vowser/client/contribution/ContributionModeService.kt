package com.vowser.client.contribution

import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
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
            Napier.w("Contribution session already active - sessionId: ${currentSession?.sessionId}, task: ${currentSession?.task}", tag = "ContributionModeService")
            return
        }
        
        val sanitizedTask = ContributionDataValidator.sanitizeString(task, ContributionConstants.MAX_TITLE_LENGTH)
        if (sanitizedTask.isBlank()) {
            Napier.e("Invalid task provided for contribution session", tag = "ContributionModeService")
            return
        }
        
        currentSession = ContributionSession(task = sanitizedTask)
        stepBuffer.clear()
        lastSentIndex = 0
        
        _status.value = ContributionStatus.RECORDING
        _currentTask.value = sanitizedTask
        _currentStepCount.value = 0
        
        startTimeoutTimer()
        
        Napier.i("🚀 Contribution session started - sessionId: ${currentSession?.sessionId}, task: '$sanitizedTask', timeout: $sessionTimeout", tag = "ContributionModeService")
        Napier.d("Session details - bufferSize: ${stepBuffer.size}, lastSentIndex: $lastSentIndex", tag = "ContributionModeService")
    }
    
    fun recordStep(step: ContributionStep) {
        val session = currentSession
        if (session == null || !session.isActive) {
            Napier.w("No active contribution session to record step", tag = "ContributionModeService")
            return
        }
        
        // 데이터 검증 및 정화
        val sanitizedStep = ContributionDataValidator.sanitizeContributionStep(step)
        if (sanitizedStep == null) {
            Napier.w("Invalid contribution step discarded: ${step.action} on ${step.url}", tag = "ContributionModeService")
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
     * 타이핑 스텝을 디바운싱하여 기록
     */
    private fun recordTypingStepWithDebounce(step: ContributionStep) {
        // 이전 타이핑 Job 취소
        typingDebounceJob?.cancel()
        
        // Enter 키가 포함된 경우 즉시 기록
        val isEnterKey = step.htmlAttributes?.get("key")?.lowercase() == "enter" ||
                        step.htmlAttributes?.get("keyCode") == "13" ||
                        step.htmlAttributes?.get("which") == "13"
        
        if (isEnterKey) {
            pendingTypingStep?.let { recordStepImmediately(it) }
            pendingTypingStep = null
            recordStepImmediately(step)
            return
        }

        pendingTypingStep = step

        typingDebounceJob = coroutineScope.launch {
            delay(typingDebounceTimeMs)
            pendingTypingStep?.let { pendingStep ->
                recordStepImmediately(pendingStep)
                pendingTypingStep = null
            }
        }
        
        Napier.d("Typing step debounced: ${step.htmlAttributes?.get("text") ?: step.title}", tag = "ContributionModeService")
    }
    
    /**
     * 대기 중인 타이핑 스텝을 즉시 처리
     */
    private fun flushPendingTypingStep() {
        typingDebounceJob?.cancel()
        pendingTypingStep?.let { pendingStep ->
            recordStepImmediately(pendingStep)
            pendingTypingStep = null
            Napier.d("Flushed pending typing step due to other action", tag = "ContributionModeService")
        }
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
        
        Napier.i("Step ${session.steps.size}: [${step.action}] ${step.selector ?: "N/A"} - ${step.htmlAttributes?.get("text") ?: step.title}", tag = "ContributionModeService")
        Napier.d("Step details - url: ${step.url}, timestamp: ${step.timestamp}, bufferSize: ${stepBuffer.size}", tag = "ContributionModeService")

        if (stepBuffer.size >= ContributionConstants.BATCH_SIZE) {
            coroutineScope.launch {
                sendBufferedSteps(isPartial = true)
            }
        }
    }
    
    fun endSession() {
        val session = currentSession ?: run {
            Napier.w("No active session to end", tag = "ContributionModeService")
            return
        }
        
        if (!session.isActive) {
            Napier.w("Session already ended - sessionId: ${session.sessionId}", tag = "ContributionModeService")
            return
        }

        flushPendingTypingStep()
        
        Napier.i("🏁 Ending contribution session - sessionId: ${session.sessionId}, totalSteps: ${session.steps.size}, bufferSize: ${stepBuffer.size}", tag = "ContributionModeService")
        
        session.isActive = false
        _status.value = ContributionStatus.SENDING
        
        coroutineScope.launch {
            try {
                sendBufferedSteps(isPartial = false, isComplete = true)
                _status.value = ContributionStatus.COMPLETED
                Napier.i("✅ Contribution session completed successfully - sessionId: ${session.sessionId}, duration: ${kotlinx.datetime.Clock.System.now().toEpochMilliseconds() - session.startTime}ms", tag = "ContributionModeService")
            } catch (e: Exception) {
                _status.value = ContributionStatus.ERROR
                Napier.e("❌ Failed to complete contribution session - sessionId: ${session.sessionId}, error: ${e.message}", e, tag = "ContributionModeService")
                Napier.d("Session state - totalSteps: ${session.steps.size}, bufferSize: ${stepBuffer.size}, lastSentIndex: $lastSentIndex", tag = "ContributionModeService")
            }
        }
    }
    
    private suspend fun sendBufferedSteps(isPartial: Boolean, isComplete: Boolean = false) {
        val session = currentSession ?: return
        
        if (stepBuffer.isEmpty()) return
        
        val stepsToSend = stepBuffer.toList()
        
        val message = ContributionMessage(
            sessionId = session.sessionId,
            task = session.task,
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

                // 성공적으로 전송된 경우만 버퍼 클리어
                if (isPartial) {
                    stepBuffer.clear()
                    lastSentIndex = session.steps.size
                }

                Napier.i("✅ Sent ${stepsToSend.size} steps (partial: $isPartial, complete: $isComplete), sessionId: ${message.sessionId}", tag = "ContributionModeService")
                Napier.d("Transmission details - totalSteps: ${message.totalSteps}, attempt: ${retryCount + 1}", tag = "ContributionModeService")
                return

            } catch (e: Exception) {
                retryCount++
                if (retryCount <= maxRetries) {
                    val delayMs = retryDelays.getOrElse(retryCount - 1) { 5000L }
                    Napier.w("Failed to send contribution steps (attempt $retryCount/$maxRetries): ${e.message}. Retrying in ${delayMs}ms...", tag = "ContributionModeService")
                    delay(delayMs)
                } else {
                    Napier.e("Failed to send contribution steps after $maxRetries attempts: ${e.message}", e, tag = "ContributionModeService")
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
                Napier.i("Session timeout reached, auto-ending session", tag = "ContributionModeService")
                endSessionWithTimeout()
            }
        }
    }
    
    private fun endSessionWithTimeout() {
        currentSession?.isActive = false
        _status.value = ContributionStatus.SENDING
        
        coroutineScope.launch {
            try {
                sendBufferedSteps(isPartial = false, isComplete = true)
                _status.value = ContributionStatus.COMPLETED
                Napier.i("Contribution session auto-completed due to timeout", tag = "ContributionModeService")
            } catch (e: Exception) {
                _status.value = ContributionStatus.ERROR
                Napier.e("Failed to auto-complete contribution session: ${e.message}", e, tag = "ContributionModeService")
            }
        }
    }
    
    fun isSessionActive(): Boolean = currentSession?.isActive == true
    
    fun getCurrentSessionId(): String? = currentSession?.sessionId
    
    fun resetSession() {
        timeoutJob?.cancel()
        timeoutJob = null
        val previousSessionId = currentSession?.sessionId
        val previousSteps = currentSession?.steps?.size ?: 0
        
        // 리셋 시에도 대기 중인 타이핑 스텝 정리
        typingDebounceJob?.cancel()
        pendingTypingStep = null
        
        currentSession = null
        stepBuffer.clear()
        lastSentIndex = 0
        _status.value = ContributionStatus.INACTIVE
        _currentStepCount.value = 0
        _currentTask.value = ""
        
        Napier.i("🔄 Session reset - previousSessionId: ${previousSessionId ?: "none"}, previousSteps: $previousSteps", tag = "ContributionModeService")
    }
}