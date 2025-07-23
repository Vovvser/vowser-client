# Package: com.vowser.client.navigation

웹 네비게이션 로직과 통계 처리를 담당하는 패키지

## 주요 파일
- `NavigationProcessor.kt` - 핵심 네비게이션 처리 클래스
  - 웹 네비게이션 그래프 관리
  - 음성 명령 처리 및 경로 탐색
  - 그래프 통계 생성
- `NavigationStatistics.kt` - 네비게이션 통계 데이터 모델

## 주요 기능
- **경로 탐색**: 음성 명령을 받아 최적 웹 탐색 경로 계산
- **그래프 관리**: WebNavigationGraph 상태 관리
- **통계 생성**: 총 노드 수, 관계 수, 평균 클릭수, 평균 시간 등
- **시각화 데이터 변환**: 내부 데이터를 UI 표시용으로 변환

## 사용 예시
```kotlin
val processor = NavigationProcessor(graph, visualizationEngine)
val result = processor.processVoiceCommand("오늘 날씨는 어때?")
val stats = processor.getGraphStatistics()
```