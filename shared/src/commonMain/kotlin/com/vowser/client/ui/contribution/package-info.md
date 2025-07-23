# Package: com.vowser.client.ui.contribution

사용자가 새로운 웹 탐색 경로를 기록하고 기여할 수 있는 기능을 제공하는 패키지

## 주요 파일
- `ContributionModeComponents.kt` - 기여 모드 전체 UI 컴포넌트
- `ContributionModeOverlay` - 기여 모드 오버레이
- `ContributionSuccessDialog` - 기여 완료 다이얼로그
- `ContributionRecordingPanel` - 기록 진행 상황 패널

## 주요 기능
- **경로 기록**: 사용자 클릭 순서를 단계적으로 기록
- **실시간 피드백**: 현재 단계와 진행률 표시
- **접근성 지원**: 음성 피드백 및 진동 피드백 (구현 준비)
- **기록 관리**: 저장/편집/삭제/일시정지 기능

## 사용 시나리오
1. 기여 모드 활성화
2. 웹 탐색 경로 기록 시작
3. 단계별 클릭 추적
4. 완료 후 경로 저장/편집