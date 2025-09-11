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
    private val typingDebounceTimeMs = 1500L // 1.5초
    
    private val _status = MutableStateFlow(ContributionStatus.INACTIVE)
    val status: StateFlow<ContributionStatus> = _status.asStateFlow()
    
    private val _currentStepCount = MutableStateFlow(0)
    val currentStepCount: StateFlow<Int> = _currentStepCount.asStateFlow()
    
    private val _currentTask = MutableStateFlow("")
    val currentTask: StateFlow<String> = _currentTask.asStateFlow()
    
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
        
        Napier.i("🚀 Contribution session started - sessionId: ${currentSession?.sessionId}, task: '$sanitizedTask', timeout: ${sessionTimeout}", tag = "ContributionModeService")
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
        
        session.steps.add(sanitizedStep)
        stepBuffer.add(sanitizedStep)
        _currentStepCount.value = session.steps.size
        
        // UI 로그 업데이트
        val elementName = sanitizedStep.htmlAttributes?.get("text") 
            ?: sanitizedStep.htmlAttributes?.get("value")
            ?: sanitizedStep.htmlAttributes?.get("placeholder")
            ?: sanitizedStep.selector?.split(".")?.lastOrNull()?.replace(Regex("[^\\w가-힣]"), "")
            ?: null
        
        onUILog?.invoke(session.steps.size, sanitizedStep.action, elementName, sanitizedStep.url)
        
        Napier.i("📝 Step ${session.steps.size}: [${sanitizedStep.action}] ${sanitizedStep.selector ?: "N/A"} - ${sanitizedStep.htmlAttributes?.get("text") ?: sanitizedStep.title}", tag = "ContributionModeService")
        Napier.d("Step details - url: ${sanitizedStep.url}, timestamp: ${sanitizedStep.timestamp}, bufferSize: ${stepBuffer.size}", tag = "ContributionModeService")
        
        // 배치 단위로 모아서 중간 전송 (메모리 관리)
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
        
        Napier.i("🏁 Ending contribution session - sessionId: ${session.sessionId}, totalSteps: ${session.steps.size}, bufferSize: ${stepBuffer.size}", tag = "ContributionModeService")
        
        session.isActive = false
        _status.value = ContributionStatus.SENDING
        
        coroutineScope.launch {
            try {
                // 최종 일괄 전송
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
                return // 성공 시 함수 종료

            } catch (e: Exception) {
                retryCount++
                if (retryCount <= maxRetries) {
                    val delayMs = retryDelays.getOrElse(retryCount - 1) { 5000L }
                    Napier.w("Failed to send contribution steps (attempt $retryCount/$maxRetries): ${e.message}. Retrying in ${delayMs}ms...", tag = "ContributionModeService")
                    kotlinx.coroutines.delay(delayMs)
                } else {
                    Napier.e("Failed to send contribution steps after $maxRetries attempts: ${e.message}", e, tag = "ContributionModeService")
                    throw e
                }
            }
        }
    }
    
    private fun startTimeoutTimer() {
        coroutineScope.launch {
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
        val previousSessionId = currentSession?.sessionId
        val previousSteps = currentSession?.steps?.size ?: 0
        
        currentSession = null
        stepBuffer.clear()
        lastSentIndex = 0
        _status.value = ContributionStatus.INACTIVE
        _currentStepCount.value = 0
        _currentTask.value = ""
        
        Napier.i("🔄 Session reset - previousSessionId: ${previousSessionId ?: "none"}, previousSteps: $previousSteps", tag = "ContributionModeService")
    }
}