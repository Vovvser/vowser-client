package com.vowser.client.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.vowser.client.AppViewModel
import com.vowser.client.StatusLogEntry
import com.vowser.client.StatusLogType
import com.vowser.client.contribution.ContributionStatus
import com.vowser.client.exception.DialogState
import com.vowser.client.model.AuthState
import com.vowser.client.ui.components.StatisticsPanel
import com.vowser.client.ui.components.SttModeSelector
import com.vowser.client.ui.components.StandardDialogs
import com.vowser.client.ui.error.ErrorBoundary
import com.vowser.client.ui.error.ErrorState
import com.vowser.client.ui.error.LoadingState
import com.vowser.client.ui.error.SmartLoadingIndicator
import com.vowser.client.ui.graph.GraphInteractionType
import com.vowser.client.ui.graph.ModernNetworkGraph
import com.vowser.client.ui.theme.AppTheme
import com.vowser.client.visualization.GraphVisualizationData
import kotlinx.coroutines.delay

/**
 * 그래프 메인 화면 컴포넌트
 */
@Composable
fun GraphScreen(
    appViewModel: AppViewModel,
    isContributionMode: Boolean,
    isLoading: Boolean,
    connectionStatus: String,
    receivedMessage: String,
    isRecording: Boolean,
    currentGraphData: GraphVisualizationData?,
    isDeveloperMode: Boolean,
    statusHistory: List<StatusLogEntry>,
    contributionStatus: ContributionStatus,
    contributionStepCount: Int,
    contributionTask: String,
    selectedSttModes: Set<String>,
    onModeToggle: () -> Unit,
    onScreenChange: (AppScreen) -> Unit,
    onReconnect: () -> Unit,
    onSendToolCall: (String, Map<String, String>) -> Unit,
    onToggleRecording: () -> Unit,
    onRefreshGraph: () -> Unit,
    onClearStatusHistory: () -> Unit,
    onToggleSttMode: (String) -> Unit,
) {
    val dialogState by appViewModel.dialogState.collectAsState()
    val authState by appViewModel.authState.collectAsState()

    // 그래프 상태
    var selectedPath by remember { mutableStateOf<List<String>>(emptyList()) }
    var activeNodeId by remember { mutableStateOf<String?>(null) }
    var showStats by remember { mutableStateOf(false) }

    // 뷰 모드 상태
    var showGraphView by remember { mutableStateOf(false) }

    // 에러 처리 상태
    var loadingState by remember { mutableStateOf<LoadingState>(LoadingState.Idle) }
    var errorState by remember { mutableStateOf<ErrorState>(ErrorState.None) }

    // 현재 그래프 데이터에서 하이라이트된 경로 추출 (실시간 데이터 우선)
    val highlightedPath = currentGraphData?.highlightedPath?.takeIf { it.isNotEmpty() }
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
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            // 상단 앱바
            GraphAppBar(
                onBackPress = { onScreenChange(AppScreen.HOME) },
                modifier = Modifier.align(Alignment.TopStart)
            )

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
                                // 그래프 새로고침 요청
                                onRefreshGraph()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // 통합 상태 UI
                EmptyStateUI(
                    isRecording = isRecording,
                    isContributionMode = isContributionMode,
                    contributionStatus = contributionStatus,
                    contributionStepCount = contributionStepCount,
                    contributionTask = contributionTask,
                    statusHistory = statusHistory,
                    isDeveloperMode = isDeveloperMode,
                    receivedMessage = receivedMessage,
                    selectedSttModes = selectedSttModes,
                    authState = authState,
                    onToggleRecording = onToggleRecording,
                    onModeToggle = onModeToggle,
                    onReconnect = onReconnect,
                    onClearStatusHistory = onClearStatusHistory,
                    onToggleSttMode = onToggleSttMode,
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
                    },
                    onShowGraph = { if (currentGraphData != null) showGraphView = true },
                    modifier = Modifier.fillMaxSize().padding(top = AppTheme.Dimensions.paddingXLarge + AppTheme.Dimensions.paddingSmall)
                )
            }

            // 통계 패널
            if (isDeveloperMode && showStats) {
                StatisticsPanel(
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

            // 에러 다이얼로그
            when (val currentDialogState = dialogState) {
                is DialogState.NetworkError -> {
                    StandardDialogs.NetworkError(
                        visible = true,
                        onRetryClick = currentDialogState.onRetry,
                        onDismiss = { appViewModel.exceptionHandler.hideDialog() }
                    )
                }
                is DialogState.BrowserError -> {
                    StandardDialogs.BrowserRetryDialog(
                        visible = true,
                        onRetryClick = currentDialogState.onRetry,
                        onAlternativeClick = currentDialogState.onAlternative,
                        onCancelClick = currentDialogState.onCancel
                    )
                }
                is DialogState.ContributionError -> {
                    StandardDialogs.ContributionFailureDialog(
                        visible = true,
                        onRetryClick = currentDialogState.onRetry,
                        onLaterClick = currentDialogState.onLater,
                        onGiveupClick = currentDialogState.onGiveUp
                    )
                }
                is DialogState.PlaywrightRestart -> {
                    StandardDialogs.PlaywrightRestartDialog(
                        visible = true,
                        onRestartClick = currentDialogState.onRestart,
                        onDismiss = { appViewModel.exceptionHandler.hideDialog() }
                    )
                }
                is DialogState.GenericError -> {
                    com.vowser.client.ui.components.ErrorDialog(
                        visible = true,
                        title = currentDialogState.title,
                        message = currentDialogState.message,
                        onPositiveClick = currentDialogState.onConfirm,
                        onDismiss = { appViewModel.exceptionHandler.hideDialog() }
                    )
                }
                is DialogState.Hidden -> {
                    // 다이얼로그 없음
                }
            }
        }
    }
}

/**
 * 통합 상태 UI
 */
@Composable
private fun EmptyStateUI(
    isRecording: Boolean,
    isContributionMode: Boolean,
    contributionStatus: ContributionStatus,
    contributionStepCount: Int,
    contributionTask: String,
    statusHistory: List<StatusLogEntry>,
    isDeveloperMode: Boolean,
    receivedMessage: String,
    selectedSttModes: Set<String>,
    onToggleRecording: () -> Unit,
    authState: AuthState,
    onModeToggle: () -> Unit,
    onReconnect: () -> Unit,
    onClearStatusHistory: () -> Unit,
    onTestCommand: () -> Unit,
    onShowGraph: () -> Unit,
    onToggleSttMode: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isLoggedIn = authState is AuthState.Authenticated
    val listState = rememberLazyListState()
    
    // 새 로그가 추가될 때 자동 스크롤
    LaunchedEffect(statusHistory.size) {
        if (statusHistory.isNotEmpty()) {
            listState.animateScrollToItem(statusHistory.size - 1)
        }
    }

    Column(
        modifier = modifier.padding(AppTheme.Dimensions.paddingMedium),
        verticalArrangement = Arrangement.spacedBy(AppTheme.Dimensions.paddingMedium)
    ) {
        // 상단 버튼 영역
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.Dimensions.paddingSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 기여 모드 토글 (로그인 시에만 표시)
            if (isLoggedIn) {
                Button(
                    onClick = onModeToggle,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isContributionMode) AppTheme.Colors.Contribution else MaterialTheme.colorScheme.secondary,
                        contentColor = Color.White
                    )
                ) {
                    Text(if (isContributionMode) "기여모드 완료" else "기여모드 시작")
                }
            }

            // 재연결 버튼
            Button(
                onClick = onReconnect,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppTheme.Colors.Info,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reconnect",
                    modifier = Modifier.size(AppTheme.Dimensions.iconSizeSmall)
                )
                Spacer(modifier = Modifier.width(AppTheme.Dimensions.paddingXSmall))
                Text("재연결")
            }

            // 개발자 모드 전용 버튼들
            if (isDeveloperMode) {
                Button(
                    onClick = onTestCommand,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppTheme.Colors.Success,
                        contentColor = Color.White
                    )
                ) {
                    Text("모의 테스트")
                }
                
                // 그래프 보기 버튼
                if (receivedMessage != "No message" || statusHistory.any { it.type == StatusLogType.SUCCESS }) {
                    Button(
                        onClick = onShowGraph,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppTheme.Colors.Info,
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
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text("Clear")
            }
        }

        // STT 모드 선택기
        SttModeSelector(
            selectedModes = selectedSttModes,
            onModeToggle = onToggleSttMode,
            isVisible = !isRecording,
            modifier = Modifier.fillMaxWidth()
        )

        // 기여 모드 UI
        if (isContributionMode) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AppTheme.Colors.Contribution.copy(alpha = 0.1f)),
            ) {
                Column(
                    modifier = Modifier.padding(AppTheme.Dimensions.paddingMedium)
                ) {
                    Text(
                        text = "🤝 기여 모드 활성화됨",
                        style = MaterialTheme.typography.headlineSmall,
                        color = AppTheme.Colors.Contribution
                    )
                    Text(
                        text = "$contributionTask 패턴을 기록하고 있는 중입니다.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // 상태 로그 영역
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            Column {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(AppTheme.Dimensions.paddingSmall),
                    verticalArrangement = Arrangement.spacedBy(AppTheme.Dimensions.paddingXSmall)
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
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
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
            .padding(
                horizontal = AppTheme.Dimensions.paddingSmall, 
                vertical = AppTheme.Dimensions.paddingXSmall
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val (color, icon) = when (entry.type) {
            StatusLogType.SUCCESS -> AppTheme.Colors.Success to "✅"
            StatusLogType.ERROR -> AppTheme.Colors.Error to "❌"
            StatusLogType.WARNING -> AppTheme.Colors.Warning to "⚠️"
            StatusLogType.INFO -> MaterialTheme.colorScheme.primary to "ℹ️"
        }
        
        Text(
            text = icon,
            fontSize = 12.sp,
            modifier = Modifier.padding(end = AppTheme.Dimensions.paddingSmall)
        )
        
        // 타임스탬프
        Text(
            text = entry.timestamp,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(end = AppTheme.Dimensions.paddingSmall)
        )
        
        // 메시지
        Text(
            text = entry.message,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * GraphScreen 상단 앱바
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GraphAppBar(
    onBackPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = { Text("Vowser", style = MaterialTheme.typography.titleLarge) },
        navigationIcon = {
            IconButton(onClick = onBackPress) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back to Home",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        ),
        modifier = modifier
    )
}