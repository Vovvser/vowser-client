import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import com.vowser.client.AppViewModel
import com.vowser.client.ui.screens.AppScreen
import com.vowser.client.ui.screens.GraphScreen
import com.vowser.client.ui.screens.SettingsScreen
import com.vowser.client.ui.theme.AppTheme
import com.vowser.client.contribution.ContributionConstants

@Composable
fun App(
    onOAuthCallbackReceived: ((AppViewModel) -> Unit)? = null
) {
    // 앱 전역 상태 관리
    var currentScreen by remember { mutableStateOf(AppScreen.GRAPH) }
    var isContributionMode by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var isDarkTheme by remember { mutableStateOf(false) }
    var isDeveloperMode by remember { mutableStateOf(false) }

    // 의존성 초기화
    val coroutineScope = rememberCoroutineScope()
    val viewModel = remember { AppViewModel(coroutineScope) }

    // OAuth 콜백 설정
    LaunchedEffect(Unit) {
        onOAuthCallbackReceived?.invoke(viewModel)
    }
    
    // WebSocket 상태 구독
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val receivedMessage by viewModel.receivedMessage.collectAsState()
    
    // 음성 녹음 상태 구독
    val isRecording by viewModel.isRecording.collectAsState()
    val recordingStatus by viewModel.recordingStatus.collectAsState()

    // STT 모드 상태 구독
    val selectedSttModes by viewModel.selectedSttModes.collectAsState()

    // 로그인 상태 구독
    val authState by viewModel.authState.collectAsState()
    
    // 그래프 데이터 구독
    val currentGraphData by viewModel.currentGraphData.collectAsState()
    val graphLoading by viewModel.graphLoading.collectAsState()
    
    // 상태 히스토리(로그) 구독
    val statusHistory by viewModel.statusHistory.collectAsState()
    
    // 기여 모드 상태 구독
    val contributionStatus by viewModel.contributionStatus.collectAsState()
    val contributionStepCount by viewModel.contributionStepCount.collectAsState()
    val contributionTask by viewModel.contributionTask.collectAsState()
    
    // 테마 적용
    val colors = if (isDarkTheme) {
        AppTheme.DarkTheme
    } else {
        AppTheme.LightTheme
    }
    
    MaterialTheme(
        colors = colors
    ) {
        // 화면 라우팅
        when (currentScreen) {
            AppScreen.GRAPH -> {
                GraphScreen(
                    appViewModel = viewModel,
                    isContributionMode = isContributionMode,
                    isLoading = isLoading || graphLoading,
                    connectionStatus = connectionStatus.toString(),
                    receivedMessage = receivedMessage,
                    isRecording = isRecording,
                    currentGraphData = currentGraphData,
                    isDeveloperMode = isDeveloperMode,
                    statusHistory = statusHistory,
                    contributionStatus = contributionStatus,
                    contributionStepCount = contributionStepCount,
                    contributionTask = contributionTask,
                    selectedSttModes = selectedSttModes,
                    onModeToggle = {
                        isContributionMode = !isContributionMode
                        if (isContributionMode) {
                            viewModel.startContribution(ContributionConstants.DEFAULT_TASK_NAME)
                            viewModel.toggleRecording()
                        } else {
                            viewModel.stopContribution()
                        }
                    },
                    onScreenChange = { currentScreen = it },
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
                    authState = authState,
                    isDarkTheme = isDarkTheme,
                    isDeveloperMode = isDeveloperMode,
                    onLogin = { viewModel.login() },
                    onLogout = { viewModel.logout() },
                    onThemeToggle = { isDarkTheme = it },
                    onDeveloperModeToggle = { isDeveloperMode = it },
                    onBackPress = { currentScreen = AppScreen.GRAPH }
                )
            }
        }
    }
}

expect fun getPlatformName(): String