# Package: com.vowser.client.ui.error

포괄적인 에러 처리 및 사용자 피드백을 담당하는 패키지

## 주요 파일
- `ErrorHandlingComponents.kt` - 에러 처리 UI 컴포넌트 모음
  - `ErrorBoundary` - React 스타일 에러 경계
  - `SmartLoadingIndicator` - 지능형 로딩 인디케이터
  - `NetworkConnectionIndicator` - 네트워크 연결 상태 표시
  - `ToastMessage` - 토스트 메시지 시스템

## 에러 처리 전략
- **LoadingState**: Idle, Loading, Success, Error 상태 관리
- **ErrorState**: None, Network, Server, Unknown 에러 분류
- **ToastType**: INFO, SUCCESS, WARNING, ERROR 메시지 타입

## 주요 기능
- **자동 재시도**: 네트워크 에러 시 자동 재시도 로직
- **사용자 친화적 메시지**: 기술적 에러를 이해하기 쉬운 메시지로 변환
- **접근성**: 스크린 리더 지원 및 키보드 네비게이션