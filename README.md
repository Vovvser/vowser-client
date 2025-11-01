# Vowser-Client

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.21-blue.svg?logo=kotlin)](https://kotlinlang.org)
[![Compose Multiplatform](https://img.shields.io/badge/Compose-Multiplatform-brightgreen.svg)](https://www.jetbrains.com/lp/compose-multiplatform/)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

The User-Facing Application for the Vowser Ecosystem.

This repository contains the source code for the Vowser client application. It's responsible for the UI, providing a
voice-driven web browsing experience, and handling the logic to control a web browser.

## Architecture

Vowser operates on a distributed architecture. The `vowser-client` plays the role of the "Face and Hands".

`[vowser-client (This Repo)] <=> [vowser-backend (Kotlin)] <=> [vowser-agent-server (Python)]`

- **vowser-client (This Repository):** A Kotlin Compose Multiplatform desktop application that captures voice commands and executes browser automation via Playwright.
- **vowser-backend:** The central server that orchestrates the flow.
- **vowser-agent-server:** The AI engine that performs heavy computational tasks.

At the moment only the desktop client is production-ready; Android/iOS targets remain on the public roadmap.

## Code Structure

The `shared` module contains platform-independent core logic, while platform-specific modules (`desktopApp`,
`androidApp`, etc.) contain entry points and specialized code for that platform.

```
.
├── shared
│   └── src
│       ├── commonMain
│       │   ├── resources
│       │   │   ├── drawable/                 # Shared icons and vector assets
│       │   │   └── mock/paths/               # Sample browser automation payloads
│       │   └── kotlin/com
│       │       ├── vowser/client
│       │       │   ├── api/                  # Backend HTTP client and DTOs
│       │       │   ├── auth/                 # Authentication state and token handling
│       │       │   ├── browserautomation/    # Browser automation domain logic
│       │       │   ├── contribution/         # Contribution session state management
│       │       │   ├── data/                 # Local persistence and settings
│       │       │   ├── exception/            # Domain-specific exceptions
│       │       │   ├── logging/              # Napier-based logging utilities
│       │       │   ├── model/                # UI and domain models
│       │       │   ├── ui/                   # Compose UI (components, screens, theme, etc.)
│       │       │   ├── visualization/        # Timeline and status graph rendering
│       │       │   └── websocket/            # WebSocket client and message DTOs
│       │       └── vowserclient/shared/browserautomation/
│       │                                   # Multiplatform Playwright bridge
│       ├── desktopMain/kotlin/com/vowser/client/
│       │   ├── auth/                        # Desktop-specific actual implementations
│       │   ├── config/                      # Desktop environment configuration loading
│       │   ├── desktop/                     # Window management and desktop utilities
│       │   └── media/                       # Microphone and audio handling
│       └── desktopTest/kotlin/com/vowserclient/shared/browserautomation/
│                                           # Playwright-based desktop automation tests
└── desktopApp
    └── src
        ├── desktopMain/kotlin/main.kt       # Compose Desktop entry point
        ├── jvmMain/kotlin/main.kt           # JVM launcher
        └── main/resources/                  # config.properties(.example), icons, etc.
```

## Features

- **Voice Interface:** Captures user voice commands and sends them to the backend.
- **Web Browser Control:** Precisely controls various browsers like Chromium, WebKit, and Firefox using Playwright.
- **Real-time Communication:** Receives and executes control commands from the backend in real-time via WebSockets.
- **Composable Architecture:** Built with Kotlin Compose Multiplatform to keep the codebase ready for future Android/iOS targets while delivering the desktop experience today.

---

## Getting Started

### Prerequisites

- **JDK 17 or higher**
- **Gradle Wrapper** (bundled with this repository)

### Environment Setup

Before running the application, make sure the companion services are up and reachable:

- [vowser-agent-server](https://github.com/Vovvser/vowser-agent-server)
- [vowser-backend](https://github.com/Vovvser/vowser-backend)

### Run the Desktop Application

From the project root directory, run:

```bash
./gradlew :desktopApp:run
```

If the app hangs or Playwright caches become inconsistent, reset the automation environment with:

```bash
./gradlew :vowser-client:cleanup
```

### Future Platform Support

Android and iOS modules exist as scaffolding and will ship with runnable launchers in upcoming releases.

## Roadmap

- Deliver the Android launcher UI and authentication flow.
- Improve accessibility based on user feedback (dark theme, keyboard shortcuts, etc.).

For contribution and community guidelines, please see [`CONTRIBUTING.md`](./CONTRIBUTING.md) and [`CODE_OF_CONDUCT.md`](./CODE_OF_CONDUCT.md).

## License

This project is licensed under the Apache 2.0 License.

---

## Testing & Quality Checks

- **Shared module tests:** `./gradlew :vowser-client:shared:test`
- **Desktop smoke test:** `./gradlew :vowser-client:desktopApp:run`
- **Playwright cleanup:** `./gradlew :vowser-client:cleanup`

## Support & Contact

- **Security disclosures and inquiries:** vowser_security@gmail.com

## Releases

- 0.0.1 — Initial public preview
  - Provides the desktop UI skeleton and Playwright browser control pipeline

# Vowser-Client (Korean)

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.21-blue.svg?logo=kotlin)](https://kotlinlang.org)
[![Compose Multiplatform](https://img.shields.io/badge/Compose-Multiplatform-brightgreen.svg)](https://www.jetbrains.com/lp/compose-multiplatform/)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

Vowser는 디지털 취약계층을 위한 음성 인식 웹 자동화 애플리케이션입니다.

이 레포지토리는 Vowser의 클라이언트 애플리케이션 소스 코드를 포함하며, 사용자에게 음성 기반 웹 브라우징 경험을 제공하는 UI와 실제 브라우저를 제어하는 로직을 담당합니다.

## 아키텍처

Vowser는 분산 아키텍처로 동작합니다.

`[vowser-client (현재 레포지토리)] <=> [vowser-backend (Java)] <=> [vowser-agent-server (Python)]`

- **vowser-client (현재 레포지토리):** Kotlin Compose Multiplatform으로 작성된 데스크톱 애플리케이션입니다. 사용자의 음성 입력을 받아 백엔드에 전달하고 Playwright를 통해 브라우저 제어 명령을 실행합니다.
- **vowser-backend:** 전체 흐름을 조율하는 중앙 서버입니다.
- **vowser-agent-server:** 음성 이해와 고도화된 AI 처리를 담당합니다.

현재는 데스크톱 클라이언트만 제공되며, 모바일 타깃(Android/iOS)은 로드맵 단계에 있습니다.

## 코드 구조

`shared` 모듈은 플랫폼 독립적인 핵심 로직을, 각 플랫폼(`desktopApp`, `androidApp` 등) 모듈은 해당 플랫폼의 진입점과 특화된 코드를 담습니다.

```
.
├── shared
│   ├── build.gradle.kts
│   └── src
│       ├── commonMain
│       │   ├── resources
│       │   │   ├── drawable/                 # 공통 아이콘·이미지 리소스
│       │   │   └── mock/paths/               # 브라우저 자동화용 샘플 경로 데이터
│       │   └── kotlin/com
│       │       ├── vowser/client
│       │       │   ├── api/                  # 백엔드 HTTP 클라이언트와 DTO
│       │       │   ├── auth/                 # 인증 상태 및 토큰 관리
│       │       │   ├── browserautomation/    # 브라우저 제어 도메인 로직
│       │       │   ├── contribution/         # 기여 세션 상태 흐름
│       │       │   ├── data/                 # 로컬 저장/설정 관리
│       │       │   ├── exception/            # 도메인 예외 정의
│       │       │   ├── logging/              # Napier 기반 로깅 유틸
│       │       │   ├── model/                # UI·도메인 모델 클래스
│       │       │   ├── ui/                   # Compose UI (components, screens, theme 등)
│       │       │   ├── visualization/        # 타임라인 및 상태 그래프 렌더링
│       │       │   └── websocket/            # WebSocket 클라이언트 및 메시지 DTO
│       │       └── vowserclient/shared/browserautomation/
│       │                                   # 멀티플랫폼 Playwright 브리지
│       ├── desktopMain/kotlin/com/vowser/client/
│       │   ├── auth/                        # 데스크톱 actual 구현
│       │   ├── config/                      # 데스크톱 환경 설정 로딩
│       │   ├── desktop/                     # 창 관리 및 데스크톱 유틸
│       │   └── media/                       # 마이크·오디오 처리
│       └── desktopTest/kotlin/com/vowserclient/shared/browserautomation/
│                                           # Playwright 기반 데스크톱 자동화 테스트
└── desktopApp
    ├── build.gradle.kts
    └── src
        ├── desktopMain/kotlin/main.kt       # Compose Desktop 엔트리포인트
        ├── jvmMain/kotlin/main.kt           # JVM 런처
        └── main/resources/                  # config.properties(.example) 등
```

## 주요 기능

- **음성 인터페이스:** 사용자의 음성 명령을 인식하여 백엔드로 전송합니다.
- **웹 브라우저 제어:** Playwright를 통해 Chromium 브라우저를 정밀하게 제어합니다.
- **실시간 통신:** WebSocket을 통해 백엔드로부터 제어 명령을 실시간으로 수신하고 실행합니다.
- **Compose 기반 구조:** Kotlin Compose Multiplatform으로 작성되어 향후 Android/iOS 등 다른 플랫폼으로 확장할 수 있는 구조를 유지합니다. (현재는 데스크톱 클라이언트만 제공)

## 로드맵

- Android 런처 UI 및 인증 흐름 구현
- 사용자 피드백 기반 접근성 개선(다크 테마 등)

---

## 시작하기

### 사전 요구사항

- **JDK 17 이상**
- **Gradle Wrapper**

### 환경 설정

애플리케이션을 실행하기 전에, `vowser-agent-server`, `vowser-backend` 서버를 실행해야 합니다.

- [vowser-agent-server](https://github.com/Vovvser/vowser-agent-server)
- [vowser-backend](https://github.com/Vovvser/vowser-backend)


### 데스크톱 애플리케이션 실행

프로젝트 루트에서 다음 명령으로 Compose Desktop 앱을 실행합니다.

```
./gradlew :desktopApp:run
```

실행이 멈추거나 Playwright 드라이버가 꼬였을 경우 `./gradlew :vowser-client:cleanup`으로 초기화할 수 있습니다.

### 추후 플랫폼 지원

Android/iOS은 향후 릴리스에서 런처 및 플랫폼별 actual 구현을 추가할 예정입니다.

기여 및 커뮤니티 행동 가이드는 [`CONTRIBUTING.md`](./CONTRIBUTING.md), [`CODE_OF_CONDUCT.md`](./CODE_OF_CONDUCT.md)를 참고해 주세요.

## 테스트 및 품질 확인

- **공유 모듈 유닛 테스트:** `./gradlew :vowser-client:shared:test`
- **데스크톱 앱 실행 검증:** `./gradlew :vowser-client:desktopApp:run`
- **Playwright 캐시/세션 초기화:** `./gradlew :vowser-client:cleanup`

## 지원 및 문의

- **보안 제보 및 문의:** vowser_security@gmail.com

## 라이선스

이 프로젝트는 Apache 2.0 라이선스를 따릅니다.

## 릴리스

- 0.0.1 — 초기 공개 버전