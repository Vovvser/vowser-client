import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import com.vowser.client.AppViewModel
import com.vowser.client.ui.screens.AppScreen
import com.vowser.client.ui.screens.GraphScreen
import com.vowser.client.ui.screens.HomeScreen
import com.vowser.client.ui.screens.SettingsScreen
import com.vowser.client.ui.screens.UserScreen
import com.vowser.client.ui.theme.AppTheme

@Composable
fun App(viewModel: AppViewModel) {
    val authState by viewModel.authState.collectAsState()
    val screenStack = remember { mutableStateListOf(AppScreen.HOME) }
    val currentScreen = screenStack.last()

    var isContributionMode by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var isDarkTheme by remember { mutableStateOf(false) }
    var isDeveloperMode by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.checkAuthStatus()
    }

    val colors = if (isDarkTheme) {
        AppTheme.DarkTheme
    } else {
        AppTheme.LightTheme
    }

    MaterialTheme(
        colorScheme = colors
    ) {
        when (currentScreen) {
            AppScreen.HOME -> {
                HomeScreen(
                    viewModel = viewModel,
                    onScreenChange = { screenStack.add(it) }
                )
            }
            AppScreen.GRAPH -> {
                GraphScreen(
                    appViewModel = viewModel,
                    isContributionMode = isContributionMode,
                    isLoading = isLoading || viewModel.graphLoading.value,
                    connectionStatus = viewModel.connectionStatus.value.toString(),
                    receivedMessage = viewModel.receivedMessage.value,
                    isRecording = viewModel.isRecording.value,
                    currentGraphData = viewModel.currentGraphData.value,
                    isDeveloperMode = isDeveloperMode,
                    statusHistory = viewModel.statusHistory.value,
                    contributionStatus = viewModel.contributionStatus.value,
                    contributionStepCount = viewModel.contributionStepCount.value,
                    contributionTask = viewModel.contributionTask.value,
                    selectedSttModes = viewModel.selectedSttModes.value,
                    onModeToggle = {
                        isContributionMode = !isContributionMode
                        if (isContributionMode) {
                            viewModel.startContribution(com.vowser.client.contribution.ContributionConstants.DEFAULT_TASK_NAME)
                            viewModel.toggleRecording()
                        } else {
                            viewModel.stopContribution()
                        }
                    },
                    onScreenChange = { screenStack.add(it) },
                    onReconnect = { viewModel.reconnect() },
                    onSendToolCall = { toolName, args ->
                        viewModel.sendToolCall(toolName, args)
                    },
                    onToggleRecording = { viewModel.toggleRecording() },
                    onRefreshGraph = { viewModel.refreshGraph() },
                    onClearStatusHistory = { viewModel.clearStatusHistory() },
                    onToggleSttMode = { modeId -> viewModel.toggleSttMode(modeId) }
                )
            }
            AppScreen.SETTINGS -> {
                SettingsScreen(
                    isDarkTheme = isDarkTheme,
                    isDeveloperMode = isDeveloperMode,
                    onThemeToggle = { isDarkTheme = it },
                    onDeveloperModeToggle = { isDeveloperMode = it },
                    onBackPress = { screenStack.removeLast() }
                )
            }
            AppScreen.USER -> {
                UserScreen(
                    viewModel = viewModel,
                    onBackPress = { screenStack.removeLast() },
                    onLogout = {
                        viewModel.logout()
                        screenStack.clear()
                        screenStack.add(AppScreen.HOME)
                    }
                )
            }
        }
    }
}

expect fun getPlatformName(): String