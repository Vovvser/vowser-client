package com.vowser.client.ui.components

enum class StatusType {
    SUCCESS,    // 성공
    ERROR,      // 오류
    PROCESSING, // 처리 중
    INFO        // 정보
}

data class Status(
    val friendlyMessage: String,
    val statusType: StatusType,
    val details: String,
    val isExpanded: Boolean = false
)

/**
 * 원본 상태 메시지를 사용자 친화적인 문구로 변환합니다.
 */
fun parseStatus(statusMessage: String): Status {
    return when {
        // 성공 케이스
        statusMessage.contains("Audio processed successfully") -> 
            Status("음성 처리 완료", StatusType.SUCCESS, statusMessage)
        
        statusMessage.startsWith("Voice processed:") -> {
            val transcript = statusMessage.removePrefix("Voice processed: ")
            Status("음성 인식 완료", StatusType.SUCCESS, transcript)
        }
        
        statusMessage == "Ready to record" ->
            Status("사용자님의 녹음을 대기 중...", StatusType.INFO, "")
            
        // 처리 중 케이스들
        statusMessage.contains("Starting recording") ->
            Status("녹음 시작 중...", StatusType.PROCESSING, statusMessage)
            
        statusMessage.contains("Recording...") ->
            Status("녹음 중...", StatusType.PROCESSING, statusMessage)
            
        statusMessage.contains("Stopping recording") ->
            Status("녹음 종료 중...", StatusType.PROCESSING, statusMessage)
            
        statusMessage.contains("Uploading audio") ->
            Status("음성 업로드 중...", StatusType.PROCESSING, statusMessage)
        
        // 오류 케이스들
        statusMessage.contains("Failed") || statusMessage.contains("Error") -> 
            Status("처리 실패", StatusType.ERROR, statusMessage)
        
        statusMessage.contains("HTTP 4") ->
            Status("요청 오류", StatusType.ERROR, statusMessage)
            
        statusMessage.contains("HTTP 5") ->
            Status("서버 오류", StatusType.ERROR, statusMessage)
            
        statusMessage.contains("Voice processing failed") ->
            Status("음성 처리 실패", StatusType.ERROR, statusMessage)
            
        statusMessage.contains("No audio data recorded") ->
            Status("녹음 데이터 없음", StatusType.ERROR, statusMessage)
        
        // 기본 케이스
        else -> Status(statusMessage, StatusType.INFO, "")
    }
}