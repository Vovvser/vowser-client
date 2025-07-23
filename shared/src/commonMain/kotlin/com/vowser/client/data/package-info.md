# Package: com.vowser.client.data

웹 네비게이션 데이터 모델과 테스트 데이터를 생성하는 패키지

## 주요 파일
- `WebNavigationData.kt` - 웹 네비게이션 기본 데이터 모델 (노드, 엣지, 관계)
- `ExpandedNaverData.kt` - 네이버 서비스 확장 데이터 모델
- `RealNaverDataGenerator.kt` - 실제 네이버 서비스 기반 테스트 데이터 생성기
  - 10개의 VoiceTestScenario 제공
  - 네이버 메인, 쇼핑, 뉴스, 지도, 웹툰 등 실제 서비스 구조 반영

## 데이터 구조
```kotlin
WebNavigationNode -> 웹페이지/액션을 나타내는 노드
WebRelationship -> 노드 간의 연결 관계
VoiceTestScenario -> 음성 명령과 예상 경로 매핑
```