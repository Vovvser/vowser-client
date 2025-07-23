# Package: com.vowser.client.ui.screens

애플리케이션의 전체 화면을 담당하는 패키지

## 주요 파일
- `GraphScreen.kt` - 메인 그래프 화면 (240라인)
  - 네트워크 그래프 시각화
  - 기여 모드 오버레이
  - 에러 처리 및 로딩 상태
  - 음성 테스트 시나리오 관리
  
- `SettingsScreen.kt` - 설정 화면 (80라인)
  - 접근성 설정 (음성 속도, 하이라이트, 애니메이션)
  - 키보드 단축키 설정

- `AppScreen.kt` - 화면 열거형
  - GRAPH, SETTINGS 화면 정의

## 화면 간 네비게이션
```kotlin
// App.kt에서 화면 전환 관리
when (currentScreen) {
    AppScreen.GRAPH -> GraphScreen(...)
    AppScreen.SETTINGS -> SettingsScreen(...)
}
```

## 리팩토링 효과
- 기존 App.kt 595라인 → App.kt 79라인 + GraphScreen.kt 240라인
- 화면별 책임 명확히 분리
- 독립적인 테스트 및 유지보수 가능