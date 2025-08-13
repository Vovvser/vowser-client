package com.vowser.client.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.vowser.client.data.RealNaverDataGenerator
import com.vowser.client.data.VoiceTestScenario
import com.vowser.client.navigation.NavigationProcessor
import com.vowser.client.ui.graph.ModernNetworkGraph
import com.vowser.client.ui.graph.GraphInteractionType
import com.vowser.client.ui.contribution.ContributionModeOverlay
import com.vowser.client.ui.contribution.ContributionSuccessDialog
import com.vowser.client.ui.error.*
import com.vowser.client.ui.components.ModernAppBar
import com.vowser.client.ui.components.StatisticsPanel
import com.vowser.client.ui.components.StatusBar
import com.vowser.client.visualization.GraphVisualizationData

/**
 * 그래프 메인 화면 컴포넌트
 */
@Composable
fun GraphScreen(
    navigationProcessor: NavigationProcessor,
    isContributionMode: Boolean,
    isLoading: Boolean,
    connectionStatus: String,
    receivedMessage: String,
    isRecording: Boolean,
    recordingStatus: String,
    currentGraphData: GraphVisualizationData?,
    onModeToggle: () -> Unit,
    onLoadingStateChange: (Boolean) -> Unit,
    onScreenChange: (AppScreen) -> Unit,
    onReconnect: () -> Unit,
    onSendToolCall: (String, Map<String, String>) -> Unit,
    onToggleRecording: () -> Unit,
    onRefreshGraph: () -> Unit,
    onNavigateToNode: (String) -> Unit
) {
    // 그래프 상태
    var selectedPath by remember { mutableStateOf<List<String>>(emptyList()) }
    var activeNodeId by remember { mutableStateOf<String?>(null) }
    var showStats by remember { mutableStateOf(false) }
    
    // 기여 모드 상태
    var isRecordingContribution by remember { mutableStateOf(false) }
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

    val graphData = currentGraphData

    // 현재 그래프 데이터에서 하이라이트된 경로 추출 (실시간 데이터 우선)
    val highlightedPath = currentGraphData?.highlightedPath?.takeIf { it.isNotEmpty() }
        ?: currentVoiceTest?.expectedPath?.takeIf { it.isNotEmpty() } 
        ?: selectedPath.takeIf { it.isNotEmpty() } 
        ?: listOf("voice_start", "naver_main")
    
    // 실시간 데이터에서 활성 노드 가져오기 
    val realTimeActiveNodeId = currentGraphData?.activeNodeId
    
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
            if (graphData != null) {
                // 메인 그래프 화면
                ModernNetworkGraph(
                    nodes = graphData.nodes,
                    edges = graphData.edges,
                    highlightedPath = highlightedPath,
                    activeNodeId = realTimeActiveNodeId ?: activeNodeId,
                    isContributionMode = isContributionMode,
                    isLoading = isLoading,
                onNodeClick = { node ->
                    // 실시간 데이터가 없을 때만 로컬 상태 업데이트
                    if (currentGraphData == null) {
                        activeNodeId = node.id
                        selectedPath = listOf("root", node.id)
                    } else {
                        // 실시간 데이터가 있을 때는 서버에 탐색 요청
                        onNavigateToNode(node.id)
                    }
                    
                    // 기여 모드일 때 클릭 기록
                    if (isContributionMode && isRecordingContribution) {
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
                                isRecordingContribution = false
                                currentStep = 0
                            }
                            // 그래프 새로고침 요청
                            onRefreshGraph()
                        }
                        else -> {}
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            } else {
                // 빈 상태 UI - 음성 명령 안내
                EmptyStateUI(
                    isRecording = isRecording,
                    recordingStatus = recordingStatus,
                    onToggleRecording = onToggleRecording,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            // 기여 모드 오버레이
            if (isContributionMode) {
                ContributionModeOverlay(
                    isRecording = isRecordingContribution,
                    currentStep = currentStep,
                    totalSteps = 5,
                    lastClickedElement = lastClickedElement,
                    onStartRecording = { 
                        isRecordingContribution = true
                        currentStep = 0
                        toastMessage = "경로 기록을 시작합니다"
                        toastType = ToastType.INFO
                    },
                    onStopRecording = { 
                        isRecordingContribution = false
                        showSuccessDialog = true
                    },
                    onPauseRecording = { 
                        isRecordingContribution = false
                        toastMessage = "기록이 일시정지되었습니다"
                        toastType = ToastType.WARNING
                    },
                    onDiscardRecording = { 
                        isRecordingContribution = false
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
                isRecording = isRecording,
                recordingStatus = recordingStatus,
                onSettingsClick = { onScreenChange(AppScreen.SETTINGS) },
                onStatsToggle = { showStats = !showStats },
                onModeToggle = onModeToggle,
                onToggleRecording = onToggleRecording,
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .align(Alignment.TopCenter)
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

/**
 * 빈 상태 UI - 음성 명령 안내
 */
@Composable
private fun EmptyStateUI(
    isRecording: Boolean,
    recordingStatus: String,
    onToggleRecording: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            // 마이크 아이콘
            Icon(
                imageVector = if (isRecording) Icons.Default.Add else Icons.Default.PlayArrow,
                contentDescription = if (isRecording) "Recording" else "Not Recording",
                tint = if (isRecording) Color.Red else Color.Gray,
                modifier = Modifier.size(64.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 메인 메시지
            Text(
                text = if (isRecording) {
                    "음성 명령을 말해보세요"
                } else {
                    "음성으로 명령해보세요!"
                },
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = Color.Black
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 상태 메시지
            Text(
                text = recordingStatus,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                color = Color.Black.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // 녹음 버튼
            FloatingActionButton(
                onClick = onToggleRecording,
                backgroundColor = if (isRecording) Color.Red else MaterialTheme.colors.primary,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Default.Add else Icons.Default.PlayArrow,
                    contentDescription = if (isRecording) "Stop Recording" else "Start Recording",
                    tint = Color.White
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 설명 텍스트
            Text(
                text = "버튼을 눌러 음성 명령을 시작하세요\n예: \"웹툰 보고싶어\", \"서울 날씨 알려줘\"",
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                color = Color.Black.copy(alpha = 0.5f),
                lineHeight = 20.sp
            )
        }
    }
}