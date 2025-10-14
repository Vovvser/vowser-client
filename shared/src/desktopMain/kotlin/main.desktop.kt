import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.vowser.client.rememberAppViewModel

actual fun getPlatformName(): String = "Desktop"

@Composable fun MainView() = App(rememberAppViewModel(rememberCoroutineScope()))

@Preview
@Composable
fun AppPreview() {
    App(rememberAppViewModel(rememberCoroutineScope()))
}