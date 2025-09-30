package com.vowser.client.contribution

/**
 * 기여모드 관련 상수들
 */
object ContributionConstants {
    // 기본 작업명
    const val DEFAULT_TASK_NAME = "기여모드 작업"
    
    // WebSocket 메시지 타입
    const val MESSAGE_TYPE = "save_contribution_path"

    // 타임아웃 설정
    const val SESSION_TIMEOUT_MINUTES = 30L
    const val RETRY_DELAY_1 = 1000L
    const val RETRY_DELAY_2 = 2000L
    const val RETRY_DELAY_3 = 5000L
    const val MAX_RETRIES = 3
    
    // 전송 전 누적할 최대 단계 수
    const val BATCH_SIZE = 5
    
    // 메모리 관리 설정
    const val MAX_TRACKED_PAGES = 20
    const val PAGE_INACTIVE_TIMEOUT_MS = 5 * 60 * 1000L // 5분
    const val MEMORY_CLEANUP_INTERVAL_MS = 2 * 60 * 1000L // 2분
    const val POLLING_INTERVAL_MS = 500L // 0.5초
    
    // 타이핑 디바운싱 설정
    const val TYPING_DEBOUNCE_TIME_MS = 1500L

    // 중복 제거 설정
    const val DEDUPLICATION_WINDOW_MS = 2000L
    
    // 데이터 검증 제한
    const val MAX_URL_LENGTH = 2048
    const val MAX_TITLE_LENGTH = 200
    const val MAX_SELECTOR_LENGTH = 500
    const val MAX_ATTRIBUTE_VALUE_LENGTH = 1000
    const val MAX_ATTRIBUTES_COUNT = 20
    const val MAX_ELEMENT_TEXT_LENGTH = 50
    const val MAX_ACTION_NAME_LENGTH = 50
    
    // 브라우저 자동화 설정
    const val PAGE_LOAD_WAIT_MS = 3000L
    const val CLICK_WAIT_MS = 2000L
    const val TYPE_WAIT_MS = 1000L
    const val BROWSER_INIT_WAIT_MS = 1000L
    
    // UI 상태 관리 설정
    const val RECORDING_STATUS_RESET_DELAY_MS = 3000L
}