import androidx.compose.ui.window.ComposeUIViewController
import com.vowser.client.AppViewModel

actual fun getPlatformName(): String = "iOS"

fun MainViewController() = ComposeUIViewController { App(AppViewModel()) }