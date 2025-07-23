import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import com.vowser.client.AppViewModel
import com.vowser.client.data.RealNaverDataGenerator
import com.vowser.client.navigation.NavigationProcessor
import com.vowser.client.ui.screens.AppScreen
import com.vowser.client.ui.screens.GraphScreen
import com.vowser.client.ui.screens.SettingsScreen
import com.vowser.client.ui.theme.AppTheme
import com.vowser.client.visualization.ComposeVisualizationEngine

/**
 * 메인 앱 컴포넌트 - 리팩토링된 버전
 * 
 * 주요 개선사항:
 * - 화면별 로직을 별도 파일로 분리
 * - 테마 관리를 별도 모듈로 분리  
 * - UI 컴포넌트를 재사용 가능한 형태로 모듈화
 */
@Composable
fun App() {
    // 앱 전역 상태 관리
    var currentScreen by remember { mutableStateOf(AppScreen.GRAPH) }
    var isContributionMode by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    
    // 의존성 초기화
    val coroutineScope = rememberCoroutineScope()
    val viewModel = remember { AppViewModel(coroutineScope) }
    
    // 네비게이션 프로세서 초기화 - 실제 네이버 데이터 사용
    val expandedGraph = remember { RealNaverDataGenerator.createExpandedNaverData() }
    val navigationProcessor = remember { 
        NavigationProcessor(expandedGraph, ComposeVisualizationEngine()) 
    }
    
    // WebSocket 상태 구독
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val receivedMessage by viewModel.receivedMessage.collectAsState()
    
    // 테마 적용
    MaterialTheme(
        colors = if (isContributionMode) {
            AppTheme.ContributionTheme
        } else {
            AppTheme.NormalTheme
        }
    ) {
        // 화면 라우팅
        when (currentScreen) {
            AppScreen.GRAPH -> {
                GraphScreen(
                    navigationProcessor = navigationProcessor,
                    isContributionMode = isContributionMode,
                    isLoading = isLoading,
                    connectionStatus = connectionStatus.toString(),
                    receivedMessage = receivedMessage,
                    onModeToggle = { isContributionMode = !isContributionMode },
                    onLoadingStateChange = { isLoading = it },
                    onScreenChange = { currentScreen = it },
                    onReconnect = { viewModel.reconnect() },
                    onSendToolCall = { toolName, args -> 
                        viewModel.sendToolCall(toolName, args)
                    }
                )
            }
            AppScreen.SETTINGS -> {
                SettingsScreen(
                    onBackPress = { currentScreen = AppScreen.GRAPH }
                )
            }
        }
    }
}

/**
 * 플랫폼별 구현을 위한 expect 함수  
 */
expect fun getPlatformName(): String