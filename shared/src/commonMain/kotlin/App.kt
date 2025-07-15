import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import com.vowser.client.AppViewModel

@OptIn(ExperimentalResourceApi::class)
@Composable
fun App() {
    MaterialTheme {
        var greetingText by remember { mutableStateOf("If you click this button, we will send&receive message!") }
        var showImage by remember { mutableStateOf(false) }

        val coroutineScope = rememberCoroutineScope()
        val viewModel = remember { AppViewModel(coroutineScope) }

        val connectionStatus by viewModel.connectionStatus.collectAsState()
        val receivedMessage by viewModel.receivedMessage.collectAsState()

        LaunchedEffect(Unit) {
            // 뷰 모델이 커넥션을 알아서 핸들링하므로 connect() 호출 필요 X
        }

        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Connection Status: $connectionStatus")
            Button(onClick = {
                greetingText = "Hello, ${getPlatformName()}!"
                showImage = !showImage
                // 예시 : Element 클릭 시 clickElement 실행
                viewModel.sendToolCall("clickElement", mapOf("elementId" to "someButton"))
            }) {
                Text(greetingText)
            }
            AnimatedVisibility(showImage) {
                Image(
                    painterResource("compose-multiplatform.xml"),
                    contentDescription = "Compose Multiplatform icon"
                )
            }
            Text("Received: $receivedMessage")

            // Session 재연결이 필요한 경우 버튼 클릭
            Button(onClick = { viewModel.reconnect() }) {
                Text("Reconnect")
            }
        }
    }
}

expect fun getPlatformName(): String