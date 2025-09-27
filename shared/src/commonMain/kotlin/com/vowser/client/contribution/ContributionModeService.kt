package com.vowser.client.contribution

import com.vowser.client.logging.VowserLogger
import com.vowser.client.logging.Tags
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
            VowserLogger.warn("Contribution session already active - sessionId: ${currentSession?.sessionId}, task: ${currentSession?.task}", Tags.BROWSER_AUTOMATION)
            return
        }
        
        val sanitizedTask = ContributionDataValidator.sanitizeString(task, ContributionConstants.MAX_TITLE_LENGTH)
        if (sanitizedTask.isBlank()) {
            VowserLogger.error("Invalid task provided for contribution session", Tags.BROWSER_AUTOMATION)
            return
        }
        
        currentSession = ContributionSession(task = sanitizedTask)
        stepBuffer.clear()
        lastSentIndex = 0
        
        _status.value = ContributionStatus.RECORDING
        _currentTask.value = sanitizedTask
        _currentStepCount.value = 0
        
        startTimeoutTimer()
        
        VowserLogger.info("🚀 Contribution session started - sessionId: ${currentSession?.sessionId}, task: '$sanitizedTask', timeout: $sessionTimeout", Tags.BROWSER_AUTOMATION)
    }
    
    fun recordStep(step: ContributionStep) {
        val session = currentSession
        if (session == null || !session.isActive) {
            VowserLogger.warn("No active contribution session to record step", Tags.BROWSER_AUTOMATION)
            return
        }
        
        // 데이터 검증 및 정화
        val sanitizedStep = ContributionDataValidator.sanitizeContributionStep(step)
        if (sanitizedStep == null) {
            VowserLogger.warn("Invalid contribution step discarded: ${step.action} on ${step.url}", Tags.BROWSER_AUTOMATION)
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

        if (step.htmlAttributes?.get("key")?.lowercase() == "enter" ||
            step.htmlAttributes?.get("keyCode") == "13" ||
            step.htmlAttributes?.get("which") == "13" ||
            step.htmlAttributes?.get("code")?.lowercase() == "enter") {
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
        
        VowserLogger.debug("Typing step debounced: ${step.htmlAttributes?.get("text") ?: step.title}", Tags.BROWSER_AUTOMATION)
    }
    
    /**
     * 대기 중인 타이핑 스텝을 즉시 처리
     */
    private fun flushPendingTypingStep() {
        typingDebounceJob?.cancel()
        pendingTypingStep?.let { pendingStep ->
            recordStepImmediately(pendingStep)
            pendingTypingStep = null
            VowserLogger.debug("Flushed pending typing step due to other action", Tags.BROWSER_AUTOMATION)
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
        
        VowserLogger.info("Step ${session.steps.size}: [${step.action}] ${step.selector ?: "N/A"} - ${step.htmlAttributes?.get("text") ?: step.title}", Tags.BROWSER_AUTOMATION)

        if (stepBuffer.size >= ContributionConstants.BATCH_SIZE) {
            coroutineScope.launch {
                sendBufferedSteps(isPartial = true)
            }
        }
    }
    
    fun endSession() {
        val session = currentSession ?: run {
            VowserLogger.warn("No active session to end", Tags.BROWSER_AUTOMATION)
            return
        }
        
        if (!session.isActive) {
            VowserLogger.warn("Session already ended - sessionId: ${session.sessionId}", Tags.BROWSER_AUTOMATION)
            return
        }

        flushPendingTypingStep()
        
        VowserLogger.info("🏁 Ending contribution session - sessionId: ${session.sessionId}, totalSteps: ${session.steps.size}, bufferSize: ${stepBuffer.size}", Tags.BROWSER_AUTOMATION)
        
        session.isActive = false
        _status.value = ContributionStatus.SENDING
        
        coroutineScope.launch {
            try {
                sendBufferedSteps(isPartial = false, isComplete = true)
                _status.value = ContributionStatus.COMPLETED
                VowserLogger.info("✅ Contribution session completed successfully - sessionId: ${session.sessionId}, duration: ${kotlinx.datetime.Clock.System.now().toEpochMilliseconds() - session.startTime}ms", Tags.BROWSER_AUTOMATION)
            } catch (e: Exception) {
                _status.value = ContributionStatus.ERROR
                VowserLogger.error("❌ Failed to complete contribution session - sessionId: ${session.sessionId}, error: ${e.message}", Tags.BROWSER_AUTOMATION)
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

                VowserLogger.info("✅ Sent ${stepsToSend.size} steps (partial: $isPartial, complete: $isComplete), sessionId: ${message.sessionId}", Tags.BROWSER_AUTOMATION)
                return

            } catch (e: Exception) {
                retryCount++
                if (retryCount <= maxRetries) {
                    val delayMs = retryDelays.getOrElse(retryCount - 1) { 5000L }
                    VowserLogger.warn("Failed to send contribution steps (attempt $retryCount/$maxRetries): ${e.message}. Retrying in ${delayMs}ms...", Tags.BROWSER_AUTOMATION)
                    delay(delayMs)
                } else {
                    VowserLogger.error("Failed to send contribution steps after $maxRetries attempts: ${e.message}", Tags.BROWSER_AUTOMATION)
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
                VowserLogger.info("Session timeout reached, auto-ending session", Tags.BROWSER_AUTOMATION)
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
                VowserLogger.info("Contribution session auto-completed due to timeout", Tags.BROWSER_AUTOMATION)
            } catch (e: Exception) {
                _status.value = ContributionStatus.ERROR
                VowserLogger.error("Failed to auto-complete contribution session: ${e.message}", Tags.BROWSER_AUTOMATION)
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
        
        VowserLogger.info("🔄 Session reset - previousSessionId: ${previousSessionId ?: "none"}, previousSteps: $previousSteps", Tags.BROWSER_AUTOMATION)
    }
}