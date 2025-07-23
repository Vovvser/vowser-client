# Package: com.vowser.client.voice

음성 명령어 처리 및 장애인 친화적 접근성 기능을 담당하는 패키지

## 주요 파일
- `VoiceCommands.kt` - 기본 음성 명령어 시스템
  - `VoiceCommandType` - 명령어 타입 분류 (REQUEST, SEARCH, NAVIGATION 등)
  - `VoiceCommand` - 음성 명령어 데이터 모델
  - `AccessibilityVoiceCommands` - 장애인 친화적 명령어 세트

- `ExpandedVoiceCommands.kt` - 확장된 음성 명령어
  - 더 다양한 표현과 동의어 지원
  - 자연어 처리 개선

## 주요 기능
- **다양한 명령어 타입**: 요청, 검색, 네비게이션, 제어
- **접근성 최적화**: 시각장애인, 거동불편자를 위한 명령어
- **자연어 지원**: "오늘 날씨는 어때?", "만화 추천해줘" 등
- **다국어 준비**: 한국어 기본, 영어 확장 가능

## 명령어 예시
```kotlin
"오늘 날씨는 어때?" -> 날씨 페이지 이동
"만화 추천해줘" -> 웹툰 추천 페이지
"강남역 근처 맛집 찾아줘" -> 지도 맛집 검색
```