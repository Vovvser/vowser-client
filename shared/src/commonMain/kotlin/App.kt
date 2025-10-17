import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vowser.client.AppViewModel
import com.vowser.client.ui.navigation.LocalScreenNavigator
import com.vowser.client.ui.navigation.StackScreenNavigator
import com.vowser.client.ui.screens.AppScreen
import com.vowser.client.ui.screens.ContributionScreen
import com.vowser.client.ui.screens.GraphScreen
import com.vowser.client.ui.screens.HomeScreen
import com.vowser.client.ui.screens.SettingsScreen
import com.vowser.client.ui.screens.UserScreen
import com.vowser.client.ui.theme.AppTheme

@Composable
fun App(viewModel: AppViewModel) {
    val screenStack = remember { mutableStateListOf(AppScreen.HOME) }
    val navigator = remember { StackScreenNavigator(screenStack) }
    val currentScreen = navigator.current()

    var isDarkTheme by remember { mutableStateOf(false) }
    var isDeveloperMode by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.checkAuthStatus()
    }

    // 사용자 대기 상태 구독
    val isWaitingForUser by viewModel.isWaitingForUser.collectAsState()
    val waitMessage by viewModel.waitMessage.collectAsState()

    // 사용자 입력 대기 상태 구독
    val isWaitingForUserInput by viewModel.isWaitingForUserInput.collectAsState()
    val inputRequest by viewModel.inputRequest.collectAsState()

    // 테마 적용
    val colors = if (isDarkTheme) {
        AppTheme.DarkTheme
    } else {
        AppTheme.LightTheme
    }

    MaterialTheme(
        colorScheme = colors
    ) {
        CompositionLocalProvider(LocalScreenNavigator provides navigator) {
            when (currentScreen) {
                AppScreen.HOME -> {
                    HomeScreen(viewModel = viewModel)
                }

                AppScreen.GRAPH -> {
                    GraphScreen(
                        appViewModel = viewModel,
                        isDeveloperMode = isDeveloperMode,
                        onReconnect = { viewModel.reconnect() },
                        onSendToolCall = { toolName, args ->
                            viewModel.sendToolCall(toolName, args)
                        },
                        onToggleRecording = { viewModel.toggleRecording() },
                        onClearStatusHistory = { viewModel.clearStatusHistory() },
                        onToggleSttMode = { modeId -> viewModel.toggleSttMode(modeId) },
                        onConfirmUserWait = { viewModel.confirmUserWait() },
                        onNavigateBack = { navigator.pop() },
                    )
                }

                AppScreen.CONTRIBUTION -> ContributionScreen(appViewModel = viewModel)
                AppScreen.SETTINGS -> {
                    SettingsScreen(
                        isDarkTheme = isDarkTheme,
                        isDeveloperMode = isDeveloperMode,
                        onThemeToggle = { isDarkTheme = it },
                        onDeveloperModeToggle = { isDeveloperMode = it },
                    )
                }

                AppScreen.USER -> {
                    UserScreen(
                        viewModel = viewModel
                    )
                }
            }
        }

        if (isWaitingForUserInput) {
            var inputText by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { viewModel.cancelUserInput() },
                title = { Text("사용자 입력 필요") },
                text = {
                    Column {
                        Text(inputRequest?.description ?: "입력 값이 필요합니다.")
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            label = { Text(inputRequest?.textLabels?.joinToString(", ") ?: "입력") }
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = { viewModel.submitUserInput(inputText) }) {
                        Text("확인")
                    }
                },
                dismissButton = {
                    Button(onClick = { viewModel.cancelUserInput() }) {
                        Text("취소")
                    }
                }
            )
        }
    }
}

expect fun getPlatformName(): String