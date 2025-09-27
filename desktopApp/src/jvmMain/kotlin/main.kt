import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.vowserclient.shared.browserautomation.BrowserAutomationService
import com.vowser.client.logging.VowserLogger
import com.vowser.client.logging.Tags
import kotlinx.coroutines.runBlocking

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        App()
    }

    // 애플리케이션 종료 시 리소스 정리
    Runtime.getRuntime().addShutdownHook(Thread {
        runBlocking { // shutdown hook 스레드 내에서 suspend 함수를 호출하기 위해 runBlocking 사용
            try {
                BrowserAutomationService.cleanup()
                VowserLogger.info("BrowserAutomationService 정리 완료.", Tags.SYSTEM_SHUTDOWN)
            } catch (e: Exception) {
                VowserLogger.error("BrowserAutomationService 정리 중 오류 발생: ${e.message}", Tags.SYSTEM_SHUTDOWN)
            }
        }
    })
}