import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import com.vowser.client.AppViewModel
import com.vowser.client.data.RealNaverDataGenerator
import com.vowser.client.navigation.NavigationProcessor
import com.vowser.client.ui.screens.AppScreen
import com.vowser.client.ui.screens.GraphScreen
import com.vowser.client.ui.screens.SettingsScreen
import com.vowser.client.ui.theme.AppTheme

@Composable
fun App() {
    // 앱 전역 상태 관리
    var currentScreen by remember { mutableStateOf(AppScreen.GRAPH) }
    var isContributionMode by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var isDarkTheme by remember { mutableStateOf(false) }
    var isDeveloperMode by remember { mutableStateOf(false) }
    
    // 의존성 초기화
    val coroutineScope = rememberCoroutineScope()
    val viewModel = remember { AppViewModel(coroutineScope) }
    
    // 네비게이션 프로세서 초기화 - 실제 네이버 데이터 사용
    val expandedGraph = remember { RealNaverDataGenerator.createExpandedNaverData() }
    val navigationProcessor = remember { 
        NavigationProcessor(expandedGraph) 
    }
    
    // WebSocket 상태 구독
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val receivedMessage by viewModel.receivedMessage.collectAsState()
    
    // 음성 녹음 상태 구독
    val isRecording by viewModel.isRecording.collectAsState()
    val recordingStatus by viewModel.recordingStatus.collectAsState()
    
    // 그래프 데이터 구독
    val currentGraphData by viewModel.currentGraphData.collectAsState()
    val graphLoading by viewModel.graphLoading.collectAsState()
    
    // 상태 히스토리(로그) 구독
    val statusHistory by viewModel.statusHistory.collectAsState()
    
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
                    navigationProcessor = navigationProcessor,
                    isContributionMode = isContributionMode,
                    isLoading = isLoading || graphLoading,
                    connectionStatus = connectionStatus.toString(),
                    receivedMessage = receivedMessage,
                    isRecording = isRecording,
                    recordingStatus = recordingStatus,
                    currentGraphData = currentGraphData,
                    isDeveloperMode = isDeveloperMode,
                    statusHistory = statusHistory,
                    onModeToggle = { isContributionMode = !isContributionMode },
                    onScreenChange = { currentScreen = it },
                    onReconnect = { viewModel.reconnect() },
                    onSendToolCall = { toolName, args -> 
                        viewModel.sendToolCall(toolName, args)
                    },
                    onToggleRecording = { viewModel.toggleRecording() },
                    onRefreshGraph = { viewModel.refreshGraph() },
                    onNavigateToNode = { nodeId -> viewModel.navigateToNode(nodeId) },
                    onClearStatusHistory = { viewModel.clearStatusHistory() }
                )
            }
            AppScreen.SETTINGS -> {
                SettingsScreen(
                    isDarkTheme = isDarkTheme,
                    isDeveloperMode = isDeveloperMode,
                    onThemeToggle = { isDarkTheme = it },
                    onDeveloperModeToggle = { isDeveloperMode = it },
                    onBackPress = { currentScreen = AppScreen.GRAPH }
                )
            }
        }
    }
}

expect fun getPlatformName(): String