import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() {
    // macOS 접근성 API 충돌 방지
    System.setProperty("java.awt.headless", "false")
    System.setProperty("apple.awt.enableTemplateImages", "false")
    System.setProperty("apple.laf.useScreenMenuBar", "false")

    application {
        Window(onCloseRequest = ::exitApplication) {
            MainView()
        }
    }
}