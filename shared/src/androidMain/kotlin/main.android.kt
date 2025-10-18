import androidx.compose.runtime.Composable
import com.vowser.client.AppViewModel

actual fun getPlatformName(): String = "Android"

@Composable fun MainView() = App(AppViewModel())
