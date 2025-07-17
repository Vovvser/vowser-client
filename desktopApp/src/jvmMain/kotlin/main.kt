import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.vowserclient.shared.browserautomation.BrowserAutomationService
import com.vowser.client.websocket.BrowserControlWebSocketClient
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun main() = application {
    Napier.base(DebugAntilog())

    // BrowserAutomationService 초기화
    // application 스코프 내에서 suspend 함수를 호출하기 위해 launch 블록 사용
    CoroutineScope(Dispatchers.IO).launch { // Dispatchers.IO는 Playwright 초기화에 적합
        try {
            BrowserAutomationService.initialize()
            Napier.i("BrowserAutomationService 초기화 완료.", tag = "AppInit")
        } catch (e: Exception) {
            Napier.e("BrowserAutomationService 초기화 실패: ${e.message}", e, tag = "AppInit")
            // 초기화 실패 시 애플리케이션 종료 또는 다른 오류 처리 로직 추가
            exitApplication() // Compose 애플리케이션 종료
        }
    }

    // WebSocket 클라이언트 인스턴스 생성
    val webSocketClient = BrowserControlWebSocketClient()

    // WebSocket 비동기적 연결 시도
    CoroutineScope(Dispatchers.IO).launch {
        try {
            webSocketClient.connect()
            Napier.i("WebSocket 클라이언트 연결 완료.", tag = "WebSocket")
        } catch (e: Exception) {
            Napier.e("Failed to connect to WebSocket: ${e.message}", e, tag = "WebSocket")
            webSocketClient.reconnect()
        }
    }

    Window(onCloseRequest = ::exitApplication) {
        App()
    }

    // 애플리케이션 종료 시 리소스 정리
    Runtime.getRuntime().addShutdownHook(Thread {
        runBlocking { // shutdown hook 스레드 내에서 suspend 함수를 호출하기 위해 runBlocking 사용
            try {
                BrowserAutomationService.cleanup()
                Napier.i("BrowserAutomationService 정리 완료.", tag = "AppShutdown")
            } catch (e: Exception) {
                Napier.e("BrowserAutomationService 정리 중 오류 발생: ${e.message}", e, tag = "AppShutdown")
            }
            try {
                webSocketClient.close()
                Napier.i("WebSocket 클라이언트 종료 완료.", tag = "AppShutdown")
            } catch (e: Exception) {
                Napier.e("WebSocket 클라이언트 종료 중 오류 발생: ${e.message}", e, tag = "AppShutdown")
            }
        }
    })
}