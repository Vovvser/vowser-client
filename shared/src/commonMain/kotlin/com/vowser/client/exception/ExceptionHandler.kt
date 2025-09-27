package com.vowser.client.exception

import com.vowser.client.logging.VowserLogger
import com.vowser.client.logging.Tags
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlin.math.min
import kotlin.math.pow

/**
 * 예외 처리, 복구
 */
class ExceptionHandler(
    private val coroutineScope: CoroutineScope,
    private val isDevelopmentMode: Boolean = false
) {

    // 다이얼로그 표시 상태
    private val _dialogState = MutableStateFlow<DialogState>(DialogState.Hidden)
    val dialogState: StateFlow<DialogState> = _dialogState.asStateFlow()

    // 현재 처리 중인 복구 작업들
    private val activeRecoveries = mutableMapOf<String, RecoveryJob>()

    /**
     * 예외 처리, 복구 전략 실행
     */
    fun handleException(
        exception: Throwable,
        context: String = "",
        onRecovery: suspend () -> Unit = {}
    ) {
        val vowserException = convertToVowserException(exception, context)

        logException(vowserException, context)

        val strategy = ExceptionRecoveryStrategies.getStrategy(vowserException)

        if (strategy.autoRecovery && vowserException.isRetryable) {
            executeAutoRecovery(vowserException, strategy, onRecovery)
        } else if (strategy.showUserDialog) {
            showUserDialog(vowserException, onRecovery)
        }
    }

    /**
     * 자동 복구
     */
    private fun executeAutoRecovery(
        exception: VowserException,
        strategy: ErrorRecoveryStrategy,
        onRecovery: suspend () -> Unit
    ) {
        val recoveryId = "${exception.errorCode}_${Clock.System.now().toEpochMilliseconds()}"

        coroutineScope.launch {
            var attempt = 0
            var success = false

            while (attempt <= strategy.maxRetries && !success) {
                if (attempt > 0) {
                    val delayMs = calculateDelayMs(attempt - 1, strategy)
                    VowserLogger.info("Retrying ${exception.errorCode} after ${delayMs}ms (attempt $attempt/${strategy.maxRetries})", Tags.EXCEPTION_RECOVERY)
                    delay(delayMs)
                }

                try {
                    onRecovery()
                    success = true
                    VowserLogger.info("Auto recovery succeeded for ${exception.errorCode} after $attempt attempts", Tags.EXCEPTION_RECOVERY)
                } catch (e: Exception) {
                    attempt++
                    VowserLogger.warn("Auto recovery attempt $attempt failed for ${exception.errorCode}: ${e.message}", Tags.EXCEPTION_RECOVERY, e)
                }
            }

            if (!success && strategy.showUserDialog) {
                showUserDialog(exception, onRecovery)
            }

            activeRecoveries.remove(recoveryId)
        }

        activeRecoveries[recoveryId] = RecoveryJob(recoveryId, exception.errorCode)
    }

    /**
     * 사용자 다이얼로그 표시
     */
    private fun showUserDialog(
        exception: VowserException,
        onRecovery: suspend () -> Unit
    ) {
        val dialogState = when (exception) {
            is NetworkException.ConnectionFailed -> {
                DialogState.NetworkError(
                    onRetry = {
                        hideDialog()
                        coroutineScope.launch { onRecovery() }
                    }
                )
            }
            is BrowserException -> {
                DialogState.BrowserError(
                    retryCount = 0,
                    onRetry = {
                        hideDialog()
                        coroutineScope.launch { onRecovery() }
                    },
                    onAlternative = { hideDialog() },
                    onCancel = { hideDialog() }
                )
            }
            is ContributionException.DataTransmissionFailed -> {
                DialogState.ContributionError(
                    onRetry = {
                        hideDialog()
                        coroutineScope.launch { onRecovery() }
                    },
                    onLater = { hideDialog() },
                    onGiveUp = { hideDialog() }
                )
            }
            is BrowserException.BrowserCrash -> {
                DialogState.PlaywrightRestart(
                    onRestart = {
                        hideDialog()
                        coroutineScope.launch { onRecovery() }
                    }
                )
            }
            else -> {
                DialogState.GenericError(
                    title = "오류",
                    message = exception.userMessage,
                    onConfirm = { hideDialog() }
                )
            }
        }

        _dialogState.value = dialogState
    }

    /**
     * 다이얼로그 숨기기
     */
    fun hideDialog() {
        _dialogState.value = DialogState.Hidden
    }

    /**
     * 일반 Exception을 VowserException으로 변환
     */
    private fun convertToVowserException(exception: Throwable, context: String): VowserException {
        return when {
            exception is VowserException -> exception

            exception.message?.contains("connection", ignoreCase = true) == true -> {
                NetworkException.ConnectionFailed(exception)
            }

            exception.message?.contains("timeout", ignoreCase = true) == true -> {
                NetworkException.RequestTimeout(exception)
            }

            exception.message?.contains("playwright", ignoreCase = true) == true -> {
                BrowserException.PlaywrightConnectionLost(exception)
            }

            exception.message?.contains("memory", ignoreCase = true) == true ||
            exception.message?.contains("heap", ignoreCase = true) == true ||
            exception::class.simpleName?.contains("OutOfMemory", ignoreCase = true) == true -> {
                SystemException.OutOfMemory(exception)
            }

            context.contains("contribution", ignoreCase = true) -> {
                ContributionException.DataTransmissionFailed(exception)
            }

            else -> {
                SystemException.FileSystemError(
                    operation = "unknown",
                    cause = exception
                )
            }
        }
    }

    /**
     * 예외 로깅
     */
    private fun logException(exception: VowserException, context: String) {
        val contextInfo = if (context.isNotEmpty()) " [Context: $context]" else ""

        when (exception) {
            is NetworkException -> {
                VowserLogger.warn("Network Exception${contextInfo}: ${exception.message}", Tags.EXCEPTION_HANDLER, exception)
            }
            is BrowserException -> {
                VowserLogger.error("Browser Exception${contextInfo}: ${exception.message}", Tags.EXCEPTION_HANDLER, exception)
            }
            is ContributionException -> {
                VowserLogger.warn("Contribution Exception${contextInfo}: ${exception.message}", Tags.EXCEPTION_HANDLER, exception)
            }
            is SystemException -> {
                VowserLogger.error("System Exception${contextInfo}: ${exception.message}", Tags.EXCEPTION_HANDLER, exception)
            }
        }

        // 개발 모드에서는 더 상세한 정보 로깅
        if (isDevelopmentMode) {
            VowserLogger.debug("Exception details - ErrorCode: ${exception.errorCode}, Retryable: ${exception.isRetryable}", Tags.EXCEPTION_HANDLER)
        }
    }

    /**
     * Exponential backoff로 지연 시간 계산
     */
    private fun calculateDelayMs(attempt: Int, strategy: ErrorRecoveryStrategy): Long {
        val exponentialDelay = (strategy.retryDelayMs * strategy.backoffMultiplier.pow(attempt)).toLong()
        return min(exponentialDelay, 30000L) // 최대 30초
    }
}

/**
 * 다이얼로그 상태
 */
sealed class DialogState {
    object Hidden : DialogState()

    data class NetworkError(
        val onRetry: () -> Unit
    ) : DialogState()

    data class BrowserError(
        val retryCount: Int,
        val onRetry: () -> Unit,
        val onAlternative: () -> Unit,
        val onCancel: () -> Unit
    ) : DialogState()

    data class ContributionError(
        val onRetry: () -> Unit,
        val onLater: () -> Unit,
        val onGiveUp: () -> Unit
    ) : DialogState()

    data class PlaywrightRestart(
        val onRestart: () -> Unit
    ) : DialogState()

    data class GenericError(
        val title: String,
        val message: String,
        val onConfirm: () -> Unit
    ) : DialogState()
}

/**
 * 복구 작업 정보
 */
private data class RecoveryJob(
    val id: String,
    val errorCode: String,
    val startTime: Long = Clock.System.now().toEpochMilliseconds()
)