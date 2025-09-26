package com.vowser.client.exception

import kotlinx.serialization.Serializable

/**
 * 예외 계층 구조
 */
sealed class VowserException(
    message: String,
    cause: Throwable? = null,
    val errorCode: String = "UNKNOWN_ERROR",
    val userMessage: String = message,
    val isRetryable: Boolean = false
) : Exception(message, cause)

/**
 * 네트워크 관련 예외
 */
sealed class NetworkException(
    message: String,
    cause: Throwable? = null,
    errorCode: String,
    userMessage: String,
    isRetryable: Boolean = true
) : VowserException(message, cause, errorCode, userMessage, isRetryable) {

    class ConnectionFailed(cause: Throwable? = null) : NetworkException(
        message = "Network connection failed",
        cause = cause,
        errorCode = "NETWORK_CONNECTION_FAILED",
        userMessage = "인터넷 연결을 확인해주세요"
    )

    class WebSocketDisconnected(cause: Throwable? = null) : NetworkException(
        message = "WebSocket connection lost",
        cause = cause,
        errorCode = "WEBSOCKET_DISCONNECTED",
        userMessage = "서버와 연결이 끊어졌습니다. 자동으로 재연결합니다.",
        isRetryable = true
    )

    class RequestTimeout(cause: Throwable? = null) : NetworkException(
        message = "Network request timeout",
        cause = cause,
        errorCode = "NETWORK_TIMEOUT",
        userMessage = "요청 시간이 초과되었습니다"
    )

    class ServerError(val statusCode: Int, cause: Throwable? = null) : NetworkException(
        message = "Server error: $statusCode",
        cause = cause,
        errorCode = "SERVER_ERROR_$statusCode",
        userMessage = "서버에 일시적인 문제가 발생했습니다"
    )
}

/**
 * 브라우저 자동화 관련 예외
 */
sealed class BrowserException(
    message: String,
    cause: Throwable? = null,
    errorCode: String,
    userMessage: String,
    isRetryable: Boolean = true
) : VowserException(message, cause, errorCode, userMessage, isRetryable) {

    class PlaywrightConnectionLost(cause: Throwable? = null) : BrowserException(
        message = "Playwright connection lost",
        cause = cause,
        errorCode = "PLAYWRIGHT_CONNECTION_LOST",
        userMessage = "브라우저와 연결이 끊어졌습니다"
    )

    class ElementNotFound(val selector: String, cause: Throwable? = null) : BrowserException(
        message = "Element not found: $selector",
        cause = cause,
        errorCode = "ELEMENT_NOT_FOUND",
        userMessage = "페이지에서 요소를 찾을 수 없습니다"
    )

    class PageLoadTimeout(val url: String, cause: Throwable? = null) : BrowserException(
        message = "Page load timeout: $url",
        cause = cause,
        errorCode = "PAGE_LOAD_TIMEOUT",
        userMessage = "페이지 로딩 시간이 초과되었습니다"
    )

    class BrowserCrash(cause: Throwable? = null) : BrowserException(
        message = "Browser process crashed",
        cause = cause,
        errorCode = "BROWSER_CRASH",
        userMessage = "브라우저에 문제가 발생했습니다",
        isRetryable = false
    )
}

/**
 * 기여 모드 관련 예외
 */
sealed class ContributionException(
    message: String,
    cause: Throwable? = null,
    errorCode: String,
    userMessage: String,
    isRetryable: Boolean = true
) : VowserException(message, cause, errorCode, userMessage, isRetryable) {

    class DataTransmissionFailed(cause: Throwable? = null) : ContributionException(
        message = "Contribution data transmission failed",
        cause = cause,
        errorCode = "CONTRIBUTION_TRANSMISSION_FAILED",
        userMessage = "기여 데이터 전송에 실패했습니다"
    )

    class InvalidContributionData(val reason: String, cause: Throwable? = null) : ContributionException(
        message = "Invalid contribution data: $reason",
        cause = cause,
        errorCode = "INVALID_CONTRIBUTION_DATA",
        userMessage = "기여 데이터가 올바르지 않습니다",
        isRetryable = false
    )

    class SessionExpired(cause: Throwable? = null) : ContributionException(
        message = "Contribution session expired",
        cause = cause,
        errorCode = "CONTRIBUTION_SESSION_EXPIRED",
        userMessage = "기여 세션이 만료되었습니다",
        isRetryable = false
    )
}

/**
 * 시스템 리소스 관련 예외
 */
sealed class SystemException(
    message: String,
    cause: Throwable? = null,
    errorCode: String,
    userMessage: String,
    isRetryable: Boolean = false
) : VowserException(message, cause, errorCode, userMessage, isRetryable) {

    class OutOfMemory(cause: Throwable? = null) : SystemException(
        message = "Out of memory",
        cause = cause,
        errorCode = "OUT_OF_MEMORY",
        userMessage = "메모리가 부족합니다. 자동으로 정리합니다."
    )

    class FileSystemError(val operation: String, cause: Throwable? = null) : SystemException(
        message = "File system error: $operation",
        cause = cause,
        errorCode = "FILE_SYSTEM_ERROR",
        userMessage = "파일 시스템 오류가 발생했습니다"
    )
}

/**
 * 에러 복구 전략
 */
@Serializable
data class ErrorRecoveryStrategy(
    val maxRetries: Int = 3,
    val retryDelayMs: Long = 1000L,
    val backoffMultiplier: Double = 2.0,
    val showUserDialog: Boolean = false,
    val autoRecovery: Boolean = true
)

/**
 * 예외별 복구 전략 매핑
 */
object ExceptionRecoveryStrategies {
    private val strategies = mapOf(
        NetworkException.ConnectionFailed::class to ErrorRecoveryStrategy(
            maxRetries = 0,
            showUserDialog = true,
            autoRecovery = false
        ),
        NetworkException.WebSocketDisconnected::class to ErrorRecoveryStrategy(
            maxRetries = Int.MAX_VALUE,
            retryDelayMs = 0L, // 즉시 재연결
            showUserDialog = false,
            autoRecovery = true
        ),
        BrowserException.ElementNotFound::class to ErrorRecoveryStrategy(
            maxRetries = 3,
            retryDelayMs = 1000L,
            backoffMultiplier = 2.0,
            showUserDialog = true,
            autoRecovery = true
        ),
        BrowserException.BrowserCrash::class to ErrorRecoveryStrategy(
            maxRetries = 0,
            showUserDialog = true,
            autoRecovery = false
        ),
        ContributionException.DataTransmissionFailed::class to ErrorRecoveryStrategy(
            maxRetries = 0,
            showUserDialog = true,
            autoRecovery = false
        ),
        SystemException.OutOfMemory::class to ErrorRecoveryStrategy(
            maxRetries = 1,
            retryDelayMs = 2000L,
            showUserDialog = false,
            autoRecovery = true
        )
    )

    fun getStrategy(exception: VowserException): ErrorRecoveryStrategy {
        return strategies[exception::class] ?: ErrorRecoveryStrategy()
    }
}