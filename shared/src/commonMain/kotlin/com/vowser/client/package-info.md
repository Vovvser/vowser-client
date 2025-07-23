# Package: com.vowser.client

앱의 메인 진입점과 전체 상태 관리를 담당하는 루트 패키지

## 주요 파일
- `App.kt` - Compose UI 메인 진입점, 화면 네비게이션
- `AppViewModel.kt` - 앱 전체 상태 관리, WebSocket 연결 관리

## 하위 패키지
- `data/` - 데이터 모델 및 생성기
- `navigation/` - 네비게이션 로직
- `ui/` - UI 컴포넌트 (그래프, 기여모드, 에러처리)
- `visualization/` - 그래프 시각화 엔진
- `voice/` - 음성 명령 시스템
- `websocket/` - 실시간 WebSocket 통신