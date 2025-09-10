import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.vowserclient.shared.browserautomation.BrowserAutomationService
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import kotlinx.coroutines.runBlocking

fun main() = application {
    Napier.base(DebugAntilog())

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
        }
    })
}