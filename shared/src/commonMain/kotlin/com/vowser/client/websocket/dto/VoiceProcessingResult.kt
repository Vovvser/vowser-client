package com.vowser.client.websocket.dto

import kotlinx.serialization.Serializable

/**
 * 음성 처리 결과 데이터
 * vowser-backend에서 음성 인식 및 처리 완료 후 전송
 */
@Serializable
data class VoiceProcessingResult(
    val sessionId: String,
    val success: Boolean,
    val transcript: String? = null,           // 음성 인식 결과 텍스트
    val command: VoiceCommand? = null,        // 해석된 음성 명령
    val navigationPath: List<String> = emptyList(), // 실행될 탐색 경로
    val error: VoiceProcessingError? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 음성 명령 데이터
 */
@Serializable
data class VoiceCommand(
    val intent: String,                       // 의도 (search, navigate, click 등)
    val target: String? = null,              // 대상 (네이버, 구글 등)
    val parameters: Map<String, String> = emptyMap(), // 추가 파라미터
    val confidence: Double = 0.0             // 신뢰도 (0.0 ~ 1.0)
)

/**
 * 음성 처리 에러
 */
@Serializable
data class VoiceProcessingError(
    val code: VoiceErrorCode,
    val message: String,
    val details: String? = null
)

@Serializable
enum class VoiceErrorCode {
    SPEECH_RECOGNITION_FAILED,    // 음성 인식 실패
    COMMAND_NOT_UNDERSTOOD,       // 명령어 이해 실패
    NAVIGATION_PATH_NOT_FOUND,    // 탐색 경로 찾기 실패
    BROWSER_CONTROL_FAILED,       // 브라우저 제어 실패
    NETWORK_ERROR,                // 네트워크 오류
    UNKNOWN_ERROR                 // 알 수 없는 오류
}