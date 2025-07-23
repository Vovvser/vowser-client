# Package: com.vowser.client.websocket.dto

WebSocket 통신에 사용되는 데이터 전송 객체(DTO)를 정의하는 패키지

## 주요 파일
- `BrowserCommand.kt` - 브라우저 제어 명령 DTO
  - 페이지 네비게이션, 요소 클릭, 텍스트 입력 등

- `CallToolRequest.kt` - 도구 호출 요청 DTO
  - 백엔드 도구 호출을 위한 표준 포맷

## 특징
- **Kotlinx.serialization** 기반 직렬화
- **타입 안전성** 보장
- **JSON 호환** 포맷
- **확장 가능한** 구조