import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import com.vowser.client.AppViewModel
import com.vowser.client.data.RealNaverDataGenerator
import com.vowser.client.data.VoiceTestScenario
import com.vowser.client.navigation.NavigationProcessor
import com.vowser.client.ui.graph.ModernNetworkGraph
import com.vowser.client.ui.graph.GraphInteractionType
import com.vowser.client.ui.contribution.ContributionModeOverlay
import com.vowser.client.ui.contribution.ContributionSuccessDialog
import com.vowser.client.ui.error.*
import com.vowser.client.visualization.ComposeVisualizationEngine

@Composable
fun App() {
    // 앱 상태 관리
    var currentScreen by remember { mutableStateOf(AppScreen.GRAPH) }
    var isContributionMode by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    
    val coroutineScope = rememberCoroutineScope()
    val viewModel = remember { AppViewModel(coroutineScope) }
    
    // 네비게이션 프로세서 초기화 - 실제 네이버 데이터 사용
    val expandedGraph = remember { RealNaverDataGenerator.createExpandedNaverData() }
    val navigationProcessor = remember { NavigationProcessor(expandedGraph, ComposeVisualizationEngine()) }
    
    // 음성 테스트 시나리오들
    val voiceScenarios = remember { RealNaverDataGenerator.getVoiceTestScenarios() }
    
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val receivedMessage by viewModel.receivedMessage.collectAsState()
    
    // ConnectionStatus를 String으로 변환
    val connectionStatusString = connectionStatus.toString()

    LaunchedEffect(Unit) {
        // 뷰 모델이 커넥션을 알아서 핸들링하므로 connect() 호출 필요 X
    }

    MaterialTheme(
        colors = if (isContributionMode) {
            darkColors(
                primary = Color(0xFF00D4AA),
                secondary = Color(0xFF00D4AA),
                background = Color(0xFF0D1117)
            )
        } else {
            darkColors(
                primary = Color(0xFF0969DA),
                secondary = Color(0xFF238636),
                background = Color(0xFF0D1117)
            )
        }
    ) {
        when (currentScreen) {
            AppScreen.GRAPH -> {
                ModernGraphScreen(
                    navigationProcessor = navigationProcessor,
                    isContributionMode = isContributionMode,
                    isLoading = isLoading,
                    connectionStatus = connectionStatusString,
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

@Composable
fun ModernGraphScreen(
    navigationProcessor: NavigationProcessor,
    isContributionMode: Boolean,
    isLoading: Boolean,
    connectionStatus: String,
    receivedMessage: String,
    onModeToggle: () -> Unit,
    onLoadingStateChange: (Boolean) -> Unit,
    onScreenChange: (AppScreen) -> Unit,
    onReconnect: () -> Unit,
    onSendToolCall: (String, Map<String, String>) -> Unit
) {
    var selectedPath by remember { mutableStateOf<List<String>>(emptyList()) }
    var activeNodeId by remember { mutableStateOf<String?>(null) }
    var showStats by remember { mutableStateOf(false) }
    
    // 기여 모드 상태
    var isRecording by remember { mutableStateOf(false) }
    var currentStep by remember { mutableStateOf(0) }
    var lastClickedElement by remember { mutableStateOf<String?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    
    // 에러 처리 상태
    var loadingState by remember { mutableStateOf<LoadingState>(LoadingState.Idle) }
    var errorState by remember { mutableStateOf<ErrorState>(ErrorState.None) }
    var toastMessage by remember { mutableStateOf<String?>(null) }
    var toastType by remember { mutableStateOf(ToastType.INFO) }
    
    // 음성 테스트 상태
    var currentVoiceTest by remember { mutableStateOf<VoiceTestScenario?>(null) }
    var voiceTestIndex by remember { mutableStateOf(0) }
    
    // 음성 테스트 시나리오들
    val voiceScenarios = remember { RealNaverDataGenerator.getVoiceTestScenarios() }
    
    // 샘플 경로 데이터 - 현재 음성 테스트나 선택된 경로 표시
    val highlightedPath = currentVoiceTest?.expectedPath?.takeIf { it.isNotEmpty() } 
        ?: selectedPath.takeIf { it.isNotEmpty() } 
        ?: listOf("voice_start", "naver_main")
    
    val graphData = navigationProcessor.getCurrentVisualizationData()
    
    // 로딩 상태 자동 해제
    LaunchedEffect(loadingState) {
        if (loadingState is LoadingState.Loading) {
            delay(2000)
            loadingState = LoadingState.Success("경로 탐색 완료")
            delay(1000)
            loadingState = LoadingState.Idle
        }
    }
    
    ErrorBoundary(
        errorState = errorState,
        onRetry = { 
            errorState = ErrorState.None
            loadingState = LoadingState.Loading
        },
        onReportError = { error ->
            toastMessage = "오류가 신고되었습니다."
            toastType = ToastType.SUCCESS
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 메인 그래프 화면
            ModernNetworkGraph(
                nodes = graphData.nodes,
                edges = graphData.edges,
                highlightedPath = if (selectedPath.isNotEmpty()) selectedPath else highlightedPath,
                activeNodeId = activeNodeId,
                isContributionMode = isContributionMode,
                isLoading = isLoading,
                onNodeClick = { node ->
                    activeNodeId = node.id
                    selectedPath = listOf("root", node.id)
                    
                    // 기여 모드일 때 클릭 기록
                    if (isContributionMode && isRecording) {
                        currentStep += 1
                        lastClickedElement = node.label
                    }
                },
                onNodeLongClick = { node ->
                    toastMessage = "노드 정보: ${node.label}"
                    toastType = ToastType.INFO
                },
                onGraphInteraction = { interactionType ->
                    when (interactionType) {
                        GraphInteractionType.ToggleMode -> onModeToggle()
                        GraphInteractionType.CenterView -> {
                            selectedPath = emptyList()
                            activeNodeId = null
                        }
                        GraphInteractionType.Reset -> {
                            selectedPath = emptyList()
                            activeNodeId = null
                            if (isContributionMode) {
                                isRecording = false
                                currentStep = 0
                            }
                        }
                        else -> {}
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            
            // 기여 모드 오버레이
            if (isContributionMode) {
                ContributionModeOverlay(
                    isRecording = isRecording,
                    currentStep = currentStep,
                    totalSteps = 5,
                    lastClickedElement = lastClickedElement,
                    onStartRecording = { 
                        isRecording = true
                        currentStep = 0
                        toastMessage = "경로 기록을 시작합니다"
                        toastType = ToastType.INFO
                    },
                    onStopRecording = { 
                        isRecording = false
                        showSuccessDialog = true
                    },
                    onPauseRecording = { 
                        isRecording = false
                        toastMessage = "기록이 일시정지되었습니다"
                        toastType = ToastType.WARNING
                    },
                    onDiscardRecording = { 
                        isRecording = false
                        currentStep = 0
                        lastClickedElement = null
                        toastMessage = "기록이 취소되었습니다"
                        toastType = ToastType.ERROR
                    }
                )
            }
        
        // 상단 앱바
        ModernAppBar(
            connectionStatus = connectionStatus,
            isContributionMode = isContributionMode,
            onSettingsClick = { onScreenChange(AppScreen.SETTINGS) },
            onStatsToggle = { showStats = !showStats },
            modifier = Modifier.align(Alignment.TopCenter)
        )
        
        // 통계 패널 (선택적 표시)
        if (showStats) {
            StatisticsPanel(
                navigationProcessor = navigationProcessor,
                onClose = { showStats = false },
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
        
            // 네트워크 연결 인디케이터
            NetworkConnectionIndicator(
                connectionStatus = connectionStatus,
                onReconnect = onReconnect,
                modifier = Modifier.align(Alignment.TopEnd)
            )
            
            // 스마트 로딩 인디케이터
            SmartLoadingIndicator(
                loadingState = loadingState,
                loadingMessage = "그래프를 업데이트하는 중...",
                onRetry = { 
                    loadingState = LoadingState.Loading
                },
                onDismiss = {
                    loadingState = LoadingState.Idle
                },
                modifier = Modifier.align(Alignment.Center)
            )
            
            // 하단 상태 바
            StatusBar(
                receivedMessage = currentVoiceTest?.voiceCommand ?: receivedMessage,
                currentVoiceTest = currentVoiceTest,
                onReconnect = onReconnect,
                onTestCommand = {
                    // 음성 테스트 시나리오 순환
                    if (voiceScenarios.isNotEmpty()) {
                        val nextScenario = voiceScenarios[voiceTestIndex % voiceScenarios.size]
                        currentVoiceTest = nextScenario
                        voiceTestIndex += 1
                        
                        // 경로 하이라이트
                        selectedPath = nextScenario.expectedPath
                        activeNodeId = nextScenario.targetNodeId
                        
                        // 토스트 메시지로 음성 명령 표시
                        toastMessage = "🎤 \"${nextScenario.voiceCommand}\""
                        toastType = ToastType.INFO
                        
                        loadingState = LoadingState.Loading
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
            
            // 토스트 메시지
            toastMessage?.let { message ->
                Box(modifier = Modifier.align(Alignment.TopCenter)) {
                    ToastMessage(
                        message = message,
                        type = toastType,
                        isVisible = true,
                        onDismiss = { toastMessage = null }
                    )
                }
            }
            
            // 기여 성공 다이얼로그
            ContributionSuccessDialog(
                isVisible = showSuccessDialog,
                pathName = "새 경로 ${currentStep}단계",
                stepCount = currentStep,
                estimatedTime = currentStep * 2,
                onSave = {
                    showSuccessDialog = false
                    toastMessage = "경로가 저장되었습니다!"
                    toastType = ToastType.SUCCESS
                },
                onEdit = {
                    showSuccessDialog = false
                    toastMessage = "편집 모드로 전환합니다"
                    toastType = ToastType.INFO
                },
                onDiscard = {
                    showSuccessDialog = false
                    currentStep = 0
                    lastClickedElement = null
                    toastMessage = "경로가 삭제되었습니다"
                    toastType = ToastType.WARNING
                },
                onDismiss = {
                    showSuccessDialog = false
                }
            )
        }
    }
}

@Composable
fun ModernAppBar(
    connectionStatus: String,
    isContributionMode: Boolean,
    onSettingsClick: () -> Unit,
    onStatsToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(16.dp),
        backgroundColor = Color.Black.copy(alpha = 0.7f),
        elevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (connectionStatus.contains("Connected")) Icons.Default.CheckCircle else Icons.Default.Clear,
                contentDescription = "Connection Status",
                tint = if (connectionStatus.contains("Connected")) Color(0xFF00D4AA) else Color(0xFFFF4444)
            )
            
            Text(
                text = "가던길 - Vowser",
                color = Color.White,
                style = MaterialTheme.typography.h6
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            IconButton(onClick = onStatsToggle) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = "Statistics",
                    tint = Color.White
                )
            }
            
            IconButton(onClick = onSettingsClick) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun StatisticsPanel(
    navigationProcessor: NavigationProcessor,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stats = navigationProcessor.getGraphStatistics()
    
    Card(
        modifier = modifier
            .width(300.dp)
            .padding(16.dp),
        backgroundColor = Color.Black.copy(alpha = 0.9f),
        elevation = 12.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "그래프 통계",
                    color = Color.White,
                    style = MaterialTheme.typography.h6
                )
                IconButton(onClick = onClose) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }
            
            Divider(color = Color.White.copy(alpha = 0.3f))
            
            StatItem("총 노드", "${stats.totalNodes}개")
            StatItem("총 관계", "${stats.totalRelationships}개")
            StatItem("평균 클릭수", "${stats.averageClicks}")
            StatItem("평균 시간", "${stats.averageTime.toInt()}초")
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.7f),
            style = MaterialTheme.typography.body2
        )
        Text(
            text = value,
            color = Color.White,
            style = MaterialTheme.typography.body2
        )
    }
}

@Composable
fun StatusBar(
    receivedMessage: String,
    currentVoiceTest: VoiceTestScenario? = null,
    onReconnect: () -> Unit,
    onTestCommand: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        backgroundColor = Color.Black.copy(alpha = 0.8f),
        elevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (currentVoiceTest != null) "🎤 음성 테스트" else "최근: $receivedMessage",
                    color = Color.White,
                    style = MaterialTheme.typography.caption
                )
                currentVoiceTest?.let { test ->
                    Text(
                        text = "\"${test.voiceCommand}\" → ${test.description}",
                        color = Color(0xFF00D4AA),
                        style = MaterialTheme.typography.caption,
                        maxLines = 1
                    )
                }
            }
            
            Button(
                onClick = onTestCommand,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = if (currentVoiceTest != null) Color(0xFF00D4AA) else Color(0xFF238636)
                )
            ) {
                Text(
                    text = if (currentVoiceTest != null) "다음 테스트" else "음성 테스트",
                    color = Color.White
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Button(
                onClick = onReconnect,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFF8250DF)
                )
            ) {
                Text("재연결", color = Color.White)
            }
        }
    }
}

@Composable
fun SettingsScreen(
    onBackPress: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = onBackPress) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Text(
                text = "설정",
                color = Color.White,
                style = MaterialTheme.typography.h4
            )
        }
        
        Card(
            backgroundColor = Color.Black.copy(alpha = 0.7f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "접근성 설정",
                    color = Color.White,
                    style = MaterialTheme.typography.h6
                )
                
                // 설정 항목들
                SettingItem("음성 속도", "1.0x")
                SettingItem("자동 하이라이트", "켜짐")
                SettingItem("애니메이션", "켜짐")
                SettingItem("키보드 단축키", "활성화")
            }
        }
    }
}

@Composable
fun SettingItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color.White,
            style = MaterialTheme.typography.body1
        )
        Text(
            text = value,
            color = Color(0xFF00D4AA),
            style = MaterialTheme.typography.body2
        )
    }
}

enum class AppScreen {
    GRAPH,
    SETTINGS
}

expect fun getPlatformName(): String