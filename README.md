# Vowser-Client

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.21-blue.svg?logo=kotlin)](https://kotlinlang.org)
[![Compose Multiplatform](https://img.shields.io/badge/Compose-Multiplatform-brightgreen.svg)](https://www.jetbrains.com/lp/compose-multiplatform/)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

The User-Facing Application for the Vowser Ecosystem.

This repository contains the source code for the Vowser client application. It's responsible for the UI, providing a voice-driven web browsing experience, and handling the logic to control a web browser.

## Architecture

Vowser operates on a distributed architecture. The `vowser-client` plays the role of the "Face and Hands".

`[vowser-client (This Repo)] <=> [vowser-backend (Kotlin)] <=> [vowser-mcp-server (Python)]`

-   **vowser-client (This Repository):** A Kotlin Compose Multiplatform desktop application that directly interacts with the user. It captures the user's voice commands, sends them to the backend, and executes control commands received from the backend via Playwright.
-   **vowser-backend:** The central server that orchestrates the flow.
-   **vowser-mcp-server:** The AI engine that performs heavy computational tasks.

## Code Structure

The `shared` module contains platform-independent core logic, while platform-specific modules (`desktopApp`, `androidApp`, etc.) contain entry points and specialized code for that platform.

```
.
├── shared                # 1. Shared Module: Code common to all platforms
│   └── src
│       └── commonMain
│           └── kotlin
│               └── com
│                   └── vowser
│                       └── client
│                           ├── core        # Core logic (Playwright, Ktor, etc.)
│                           ├── model       # Data models
│                           └── ui          # Common UI components (Compose)
└── desktopApp            # 2. Desktop App Module
    └── src
        └── main
            ├── kotlin      # Desktop App's Main function
            └── resources   # Config files, icons, etc.
```

## Features

-   **Voice Interface:** Captures user voice commands and sends them to the backend.
-   **Web Browser Control:** Precisely controls various browsers like Chromium, WebKit, and Firefox using Playwright.
-   **Real-time Communication:** Receives and executes control commands from the backend in real-time via WebSockets.
-   **Multiplatform Support:** Built with Kotlin Compose Multiplatform to support desktops (Windows, macOS, Linux) with easy extension to mobile platforms in the future.

---

## Getting Started

### Prerequisites

-   **JDK 17 or higher**
-   **Android Studio** (Required for building the Android app)
-   **(Optional) Xcode** (Required for building the iOS app)

### Configuration

Before running the application, you need to configure the address of the `vowser-backend` server.

1.  In the `desktopApp/src/main/resources/` directory, copy `config.properties.example` to a new file named `config.properties`.
2.  Open the new file and modify the `backend.url` to match your environment.

For Android development, you also need to set up the `local.properties` file at the project root. (See platform-specific instructions below).

### Running the Application

#### 🖥️ Desktop App

From the project root directory, run the following command:

```bash
./gradlew :desktopApp:run
```

#### 🤖 Android App

1.  Open the project folder in Android Studio.
2.  **(First time only)** Copy the `local.properties.example` file at the project root to `local.properties` and set your Android SDK path.
3.  Select `androidApp` from the run configurations and click the ▶️ (Run) button.

#### 📱 iOS App

**Note:** Building and running the iOS app requires a macOS environment and Xcode.

1.  **(First time only)** Run the following command to generate the Xcode project file:
    ```bash
    ./gradlew :shared:podinstall
    ```
2.  Open `iosApp/iosApp.xcworkspace` in Xcode.
3.  Select a simulator and click the ▶️ (Build and run) button.

## License

This project is licensed under the Apache 2.0 License.

---

# Vowser-Client (Korean)

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.21-blue.svg?logo=kotlin)](https://kotlinlang.org)
[![Compose Multiplatform](https://img.shields.io/badge/Compose-Multiplatform-brightgreen.svg)](https://www.jetbrains.com/lp/compose-multiplatform/)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

Vowser 생태계를 위한 사용자 대면 애플리케이션입니다.

이 레포지토리는 Vowser의 클라이언트 애플리케이션 소스 코드를 포함하며, 사용자에게 음성 기반 웹 브라우징 경험을 제공하는 UI와 실제 브라우저를 제어하는 로직을 담당합니다.

## 아키텍처

Vowser는 분산 아키텍처로 동작하며, `vowser-client`는 사용자의 '얼굴과 손발(Face/Hands)' 역할을 수행합니다.

`[vowser-client (현재 레포지토리)] <=> [vowser-backend (Kotlin)] <=> [vowser-mcp-server (Python)]`

-   **vowser-client (현재 레포지토리):** 사용자와 직접 상호작용하는 Kotlin Compose Multiplatform 기반 데스크톱 애플리케이션입니다. 사용자의 음성 입력을 받아 백엔드에 전달하고, 백엔드로부터 받은 제어 명령을 Playwright를 통해 실제 브라우저에 실행합니다.
-   **vowser-backend:** 전체 흐름을 조율하는 중앙 서버입니다.
-   **vowser-mcp-server:** 웹페이지 분석 등 AI 연산을 수행하는 Python 서버입니다.

## 코드 구조

`shared` 모듈은 플랫폼 독립적인 핵심 로직을, 각 플랫폼(`desktopApp`, `androidApp` 등) 모듈은 해당 플랫폼의 진입점과 특화된 코드를 담습니다.

```
.
├── shared                # 1. 공유 모듈: 모든 플랫폼이 공통으로 사용하는 코드
│   └── src
│       └── commonMain
│           └── kotlin
│               └── com
│                   └── vowser
│                       └── client
│                           ├── core        # Playwright, Ktor 등 핵심 로직
│                           ├── model       # 데이터 모델
│                           └── ui          # 공통 UI 컴포넌트 (Compose)
└── desktopApp            # 2. 데스크톱 앱 모듈
    └── src
        └── main
            ├── kotlin      # 데스크톱 앱 Main 함수
            └── resources   # 설정 파일, 아이콘 등
```

## 주요 기능

-   **음성 인터페이스:** 사용자의 음성 명령을 인식하여 백엔드로 전송합니다.
-   **웹 브라우저 제어:** Playwright를 통해 Chromium, WebKit, Firefox 등 다양한 브라우저를 정밀하게 제어합니다.
-   **실시간 통신:** WebSocket을 통해 백엔드로부터 제어 명령을 실시간으로 수신하고 실행합니다.
-   **멀티플랫폼 지원:** Kotlin Compose Multiplatform을 사용하여 데스크톱(Windows, macOS, Linux)을 지원하며, 향후 모바일로 확장이 용이합니다.

---

## 시작하기

### 사전 요구사항

-   **JDK 17 이상**
-   **Android Studio** (Android 앱 빌드 시 필요)
-   **(선택) Xcode** (iOS 앱 빌드 시 필요)

### 설정

애플리케이션을 실행하기 전에, `vowser-backend` 서버의 주소를 설정해야 합니다.

1.  `desktopApp/src/main/resources/` 디렉토리의 `config.properties.example` 파일을 복사하여 `config.properties` 파일을 생성하세요.
2.  파일을 열어 백엔드 서버 주소를 실제 환경에 맞게 수정합니다.

또한, 안드로이드 빌드를 위해 프로젝트 루트에서 `local.properties` 파일을 설정해야 합니다. (자세한 내용은 아래 플랫폼별 실행 방법 참고)

### 플랫폼별 실행 방법

#### 🖥️ Desktop App

프로젝트 루트 디렉토리에서 다음 명령어를 실행하세요.

```bash
./gradlew :desktopApp:run
```

#### 🤖 Android App

1.  Android Studio에서 이 프로젝트 폴더를 엽니다.
2.  **(최초 1회)** 프로젝트 루트의 `local.properties.example` 파일을 `local.properties`로 복사하고, 본인의 Android SDK 경로를 설정합니다.
3.  상단 실행 설정에서 `androidApp`을 선택하고, 에뮬레이터나 기기에서 ▶️ (Run) 버튼을 클릭합니다.

#### 📱 iOS App

**참고:** iOS 앱을 빌드하고 실행하려면 macOS 환경과 Xcode가 반드시 필요합니다.

1.  **최초 1회:** 다음 명령어로 Xcode 프로젝트 파일을 생성합니다.
    ```bash
    ./gradlew :shared:podinstall
    ```
2.  Xcode에서 `iosApp/iosApp.xcworkspace` 파일을 엽니다.
3.  시뮬레이터를 선택하고 ▶️ (Build and run) 버튼을 클릭합니다.

## 라이선스

이 프로젝트는 Apache 2.0 라이선스를 따릅니다.