package com.vowser.client.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.vowser.client.data.VoiceTestScenario
import com.vowser.client.navigation.NavigationProcessor
import com.vowser.client.ui.graph.ModernNetworkGraph
import com.vowser.client.ui.graph.GraphInteractionType
import com.vowser.client.ui.error.ErrorBoundary
import com.vowser.client.ui.error.LoadingState
import com.vowser.client.ui.error.ErrorState
import com.vowser.client.ui.error.ToastType
import com.vowser.client.ui.error.SmartLoadingIndicator
import com.vowser.client.ui.components.ModernAppBar
import com.vowser.client.ui.components.StatisticsPanel
import com.vowser.client.ui.theme.AppTheme
import com.vowser.client.visualization.GraphVisualizationData
import com.vowser.client.StatusLogEntry
import com.vowser.client.StatusLogType

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
    isDeveloperMode: Boolean,
    statusHistory: List<StatusLogEntry>,
    onModeToggle: () -> Unit,
    onScreenChange: (AppScreen) -> Unit,
    onReconnect: () -> Unit,
    onSendToolCall: (String, Map<String, String>) -> Unit,
    onToggleRecording: () -> Unit,
    onRefreshGraph: () -> Unit,
    onNavigateToNode: (String) -> Unit,
    onClearStatusHistory: () -> Unit,
) {
    // 그래프 상태
    var selectedPath by remember { mutableStateOf<List<String>>(emptyList()) }
    var activeNodeId by remember { mutableStateOf<String?>(null) }
    var showStats by remember { mutableStateOf(false) }
    
    // 뷰 모드 상태 (개발자 모드에서 그래프/홈 전환용)
    var showGraphView by remember { mutableStateOf(false) }
    
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
            loadingState = LoadingState.Loading
        }
    ) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colors.background)) {
            if (isDeveloperMode && showGraphView && currentGraphData != null) {
                //  그래프 화면
                ModernNetworkGraph(
                    nodes = currentGraphData.nodes,
                    edges = currentGraphData.edges,
                    highlightedPath = highlightedPath,
                    activeNodeId = realTimeActiveNodeId ?: activeNodeId,
                    isContributionMode = isContributionMode,
                    isLoading = isLoading,
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
                // 통합 상태 UI
                EmptyStateUI(
                    isRecording = isRecording,
                    recordingStatus = recordingStatus,
                    isContributionMode = isContributionMode,
                    statusHistory = statusHistory,
                    isDeveloperMode = isDeveloperMode,
                    receivedMessage = currentVoiceTest?.voiceCommand ?: receivedMessage,
                    onToggleRecording = onToggleRecording,
                    onModeToggle = onModeToggle,
                    onReconnect = onReconnect,
                    onClearStatusHistory = onClearStatusHistory,
                    onTestCommand = {
                        showGraphView = true  // 테스트 실행 시 그래프 뷰로 전환
                        // 새로운 날씨 검색 결과 모의 테스트 데이터
                        val mockData = """
                        {
                          "type": "all_navigation_paths",
                          "data": {
                            "query": "우리 지역 날씨 알고 싶어",
                            "paths": [
                              {
                                "pathId": "09e2a975413c0e18a7cd9d0f57b15dea",
                                "score": 0.489,
                                "total_weight": 73,
                                "last_used": null,
                                "estimated_time": null,
                                "steps": [
                                  {
                                    "url": "https://naver.com",
                                    "title": "naver.com 메인",
                                    "action": "navigate",
                                    "selector": "",
                                    "htmlAttributes": null
                                  },
                                  {
                                    "url": "https://www.naver.com",
                                    "title": "날씨",
                                    "action": "click",
                                    "selector": "a[href*='weather.naver.com']",
                                    "htmlAttributes": null
                                  },
                                  {
                                    "url": "https://weather.naver.com",
                                    "title": "지역선택",
                                    "action": "click",
                                    "selector": ".region_select .btn_region",
                                    "htmlAttributes": null
                                  },
                                  {
                                    "url": "https://weather.naver.com/region/list",
                                    "title": "부산",
                                    "action": "click",
                                    "selector": ".region_list .region_item[data-region='busan'] a",
                                    "htmlAttributes": null
                                  },
                                  {
                                    "url": "https://weather.naver.com/today/09440111",
                                    "title": "미세먼지",
                                    "action": "click",
                                    "selector": ".content_tabmenu .tab_item[data-tab='air'] a",
                                    "htmlAttributes": null
                                  },
                                  {
                                    "url": "https://weather.naver.com/air/09440111",
                                    "title": "주간",
                                    "action": "click",
                                    "selector": ".air_chart_area .btn_chart_period[data-period='week']",
                                    "htmlAttributes": null
                                  },
                                  {
                                    "url": "https://weather.naver.com/air/09440111?period=week",
                                    "title": "지역비교",
                                    "action": "click",
                                    "selector": ".compare_area .btn_compare",
                                    "htmlAttributes": null
                                  }
                                ]
                              }
                            ]
                          }
                        }
                        """.trimIndent()
                        
                        // 서버에 모의 데이터 전송
                        onSendToolCall("mock_navigation_data", mapOf("data" to mockData))
                        
                        // 그래프 표시 위해 로딩 상태로 전환
                        loadingState = LoadingState.Loading
                        
                        // 토스트 메시지 표시
                        toastMessage = "날씨 탐색 경로를 분석하는 중..."
                        toastType = ToastType.INFO
                    },
                    onShowGraph = { if (currentGraphData != null) showGraphView = true },
                    modifier = Modifier.fillMaxSize().padding(top = 60.dp)
                )
            }
            
        
            // 상단 앱바
            ModernAppBar(
                connectionStatus = connectionStatus,
                onSettingsClick = { onScreenChange(AppScreen.SETTINGS) },
                onStatsToggle = { showStats = !showStats },
                showHomeButton = isDeveloperMode && showGraphView && currentGraphData != null,
                onHomeClick = { showGraphView = false },
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            )
            
            // 통계 패널
            if (isDeveloperMode && showStats) {
                StatisticsPanel(
                    navigationProcessor = navigationProcessor,
                    onClose = { showStats = false },
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }
            
            
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
        }
    }
}

/**
 * 통합 상태 UI - 로그, 버튼, 기여모드 모두 포함
 */
@Composable
private fun EmptyStateUI(
    isRecording: Boolean,
    recordingStatus: String,
    isContributionMode: Boolean,
    statusHistory: List<StatusLogEntry>,
    isDeveloperMode: Boolean,
    receivedMessage: String,
    onToggleRecording: () -> Unit,
    onModeToggle: () -> Unit,
    onReconnect: () -> Unit,
    onClearStatusHistory: () -> Unit,
    onTestCommand: () -> Unit,
    onShowGraph: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    
    // 새 로그가 추가될 때 자동 스크롤
    LaunchedEffect(statusHistory.size) {
        if (statusHistory.isNotEmpty()) {
            listState.animateScrollToItem(statusHistory.size - 1)
        }
    }

    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 상단 버튼 영역
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 음성 녹음 버튼
            Button(
                onClick = onToggleRecording,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = if (isRecording) AppTheme.Colors.Error else MaterialTheme.colors.primary,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Default.Clear else Icons.Default.PlayArrow,
                    contentDescription = if (isRecording) "Stop Recording" else "Start Recording",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isRecording) "녹음 중지" else "음성 녹음")
            }

            // 기여 모드 토글
            Button(
                onClick = onModeToggle,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = if (isContributionMode) AppTheme.Colors.Contribution else MaterialTheme.colors.secondary,
                    contentColor = Color.White
                )
            ) {
                Text(if (isContributionMode) "기여모드 OFF" else "기여모드 ON")
            }

            // 재연결 버튼
            Button(
                onClick = onReconnect,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(133,118,162),
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reconnect",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("재연결")
            }

            // 개발자 모드 전용 버튼들
            if (isDeveloperMode) {
                Button(
                    onClick = onTestCommand,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = AppTheme.Colors.Success,
                        contentColor = Color.White
                    )
                ) {
                    Text("모의 테스트")
                }
                
                // 그래프 보기 버튼 (데이터가 있을 때만)
                if (receivedMessage != "No message" || statusHistory.any { it.type == StatusLogType.SUCCESS }) {
                    Button(
                        onClick = onShowGraph,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = AppTheme.Colors.Info,
                            contentColor = Color.White
                        )
                    ) {
                        Text("그래프 보기")
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 클리어 버튼
            OutlinedButton(
                onClick = onClearStatusHistory,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colors.onSurface
                )
            ) {
                Text("Clear")
            }
        }

        // 기여 모드 UI (통합)
        if (isContributionMode) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = AppTheme.Colors.Contribution.copy(alpha = 0.1f),
                elevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "🤝 기여 모드 활성화됨",
                        style = MaterialTheme.typography.h6,
                        color = AppTheme.Colors.Contribution
                    )
                    Text(
                        text = "웹 브라우징 패턴이 학습되고 있습니다",
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // 상태 로그 영역
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            elevation = 4.dp
        ) {
            Column {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (statusHistory.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "음성으로 명령해보세요!\n예: \"웹툰 보고싶어\", \"서울 날씨 알려줘\"",
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.body2,
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }
                    } else {
                        items(statusHistory) { logEntry ->
                            StatusLogItem(logEntry)
                        }
                    }
                    
                    // 개발자 모드에서만 receivedMessage 표시
                    if (isDeveloperMode && receivedMessage != "No message") {
                        item {
                            StatusLogItem(
                                StatusLogEntry(
                                    timestamp = "서버",
                                    message = receivedMessage,
                                    type = StatusLogType.INFO
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusLogItem(entry: StatusLogEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 상태 타입 인디케이터
        val (color, icon) = when (entry.type) {
            StatusLogType.SUCCESS -> AppTheme.Colors.Success to "✅"
            StatusLogType.ERROR -> AppTheme.Colors.Error to "❌"
            StatusLogType.WARNING -> AppTheme.Colors.Warning to "⚠️"
            StatusLogType.INFO -> MaterialTheme.colors.primary to "ℹ️"
        }
        
        Text(
            text = icon,
            fontSize = 12.sp,
            modifier = Modifier.padding(end = 8.dp)
        )
        
        // 타임스탬프
        Text(
            text = entry.timestamp,
            fontSize = 12.sp,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(end = 8.dp)
        )
        
        // 메시지
        Text(
            text = entry.message,
            fontSize = 13.sp,
            color = MaterialTheme.colors.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}