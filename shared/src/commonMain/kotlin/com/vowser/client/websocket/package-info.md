# Package: com.vowser.client.websocket

실시간 WebSocket 통신과 브라우저 제어를 담당하는 패키지

## 주요 파일
- `BrowserControlWebSocketClient.kt` - WebSocket 클라이언트 핵심 구현
  - Ktor WebSocket 기반
  - 자동 재연결 로직
  - 메시지 송수신 관리

- `ConnectionStatus.kt` - 연결 상태 열거형
  - Disconnected, Connecting, Connected, Error

- `dto/` - 데이터 전송 객체
  - `BrowserCommand.kt` - 브라우저 제어 명령
  - `CallToolRequest.kt` - 도구 호출 요청

## 주요 기능
- **실시간 양방향 통신**: 클라이언트 ↔ 백엔드
- **자동 재연결**: 네트워크 끊김 시 자동 복구
- **타입 안전한 메시지**: Kotlinx.serialization 기반
- **상태 관리**: Flow 기반 반응형 상태 관리

## 통신 플로우
1. WebSocket 연결 설정
2. 브라우저 제어 명령 전송
3. 백엔드에서 실제 브라우저 조작
4. 결과 수신 및 UI 업데이트