import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.vowser.client.rememberAppViewModel
import com.vowser.client.browserautomation.BrowserAutomationService
import io.github.aakira.napier.Napier
import com.vowser.client.logging.Tags
import kotlinx.coroutines.runBlocking

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        val coroutineScope = rememberCoroutineScope()
        val viewModel = rememberAppViewModel(coroutineScope)
        viewModel.startAuthCallbackServer()

        App(viewModel)
    }

    // 애플리케이션 종료 시 리소스 정리
    Runtime.getRuntime().addShutdownHook(Thread {
        runBlocking {
            try {
                BrowserAutomationService.cleanup()
                Napier.i("BrowserAutomationService 정리 완료.", tag = Tags.SYSTEM_SHUTDOWN)
            } catch (e: Exception) {
                Napier.e("BrowserAutomationService 정리 중 오류 발생: ${e.message}", e, tag = Tags.SYSTEM_SHUTDOWN)
            }
        }
    })
}