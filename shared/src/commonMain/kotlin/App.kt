import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
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
    }
}

expect fun getPlatformName(): String