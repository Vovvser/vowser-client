package com.vowser.client.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.vowser.client.AppViewModel
import com.vowser.client.StatusLogEntry
import com.vowser.client.StatusLogType
import com.vowser.client.exception.DialogState
import com.vowser.client.ui.components.GenericAppBar
import com.vowser.client.ui.components.StatisticsPanel
import com.vowser.client.ui.components.StandardDialogs
import com.vowser.client.ui.error.ErrorBoundary
import com.vowser.client.ui.error.ErrorState
import com.vowser.client.ui.error.LoadingState
import com.vowser.client.ui.error.SmartLoadingIndicator
import com.vowser.client.ui.graph.ModernNetworkGraph
import com.vowser.client.ui.theme.AppTheme
import com.vowser.client.websocket.ConnectionStatus
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

/**
 * 그래프 메인 화면 컴포넌트
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphScreen(
    appViewModel: AppViewModel,
    isDeveloperMode: Boolean,
    onReconnect: () -> Unit,
    onClearStatusHistory: () -> Unit,
    onConfirmUserWait: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    DisposableEffect(Unit) {
        onDispose {
            appViewModel.cancelActiveAutomation()
        }
    }

    val dialogState by appViewModel.dialogState.collectAsState()
    val connectionStatus by appViewModel.connectionStatus.collectAsState()
    val receivedMessage by appViewModel.receivedMessage.collectAsState()
    val currentGraphDataState by appViewModel.currentGraphData.collectAsState()
    val statusHistory by appViewModel.statusHistory.collectAsState()
    val isWaitingForUser by appViewModel.isWaitingForUser.collectAsState()
    val waitMessage by appViewModel.waitMessage.collectAsState()
    val graphLoading by appViewModel.graphLoading.collectAsState()

    // 그래프 상태
    var selectedPath by remember { mutableStateOf<List<String>>(emptyList()) }
    var activeNodeId by remember { mutableStateOf<String?>(null) }
    var showStats by remember { mutableStateOf(false) }

    // 뷰 모드 상태
    var showGraphView by remember { mutableStateOf(false) }

    // 에러 처리 상태
    var loadingState by remember { mutableStateOf<LoadingState>(LoadingState.Idle) }
    var errorState by remember { mutableStateOf<ErrorState>(ErrorState.None) }

    val pendingCommand by appViewModel.pendingCommand.collectAsState()
    LaunchedEffect(pendingCommand) {
        pendingCommand?.let { command ->
            val trimmed = command.trim()
            if (trimmed.isNotEmpty()) {
                appViewModel.executeQuery(trimmed)
                appViewModel.clearPendingCommand()
            }
        }
    }

    // 현재 그래프 데이터에서 하이라이트된 경로 추출 (실시간 데이터 우선)
    val graphData = currentGraphDataState
    val highlightedPath = graphData?.highlightedPath?.takeIf { it.isNotEmpty() }
        ?: selectedPath.takeIf { it.isNotEmpty() }
        ?: listOf("voice_start", "naver_main")

    // 실시간 데이터에서 활성 노드 가져오기
    val realTimeActiveNodeId = graphData?.activeNodeId

    // 그래프 로딩 상태 반영
    LaunchedEffect(graphLoading) {
        if (graphLoading) {
            loadingState = LoadingState.Loading
        }
    }

    // 로딩 상태 자동 해제
    LaunchedEffect(loadingState, graphLoading) {
        if (loadingState is LoadingState.Loading && !graphLoading) {
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
        Scaffold(
            topBar = {
                GenericAppBar(
                    title = "경로 실행 그래프",
                    onBackPress = {
                        appViewModel.cancelActiveAutomation()
                        onNavigateBack()
                    }
                )
            }
        ) { paddingValues ->
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                val shouldShowGraph = isDeveloperMode && showGraphView && graphData != null

                if (shouldShowGraph && graphData != null) {
                    ModernNetworkGraph(
                        nodes = graphData.nodes,
                        edges = graphData.edges,
                        highlightedPath = highlightedPath,
                        activeNodeId = realTimeActiveNodeId ?: activeNodeId,
                        isContributionMode = false,
                        searchInfo = graphData.searchInfo,
                        allMatchedPaths = graphData.allMatchedPaths,
                        modifier = Modifier
                            .size(width = 1920.dp, height = 1080.dp)
                            .align(Alignment.Center)
                        ,
                        contentScale = 1.0f
                    )
                } else {
                    EmptyStateUI(
                        connectionStatus = connectionStatus,
                        statusHistory = statusHistory,
                        isDeveloperMode = isDeveloperMode,
                        receivedMessage = receivedMessage,
                        onReconnect = onReconnect,
                        onClearStatusHistory = onClearStatusHistory,
                        onShowGraph = { if (graphData != null) showGraphView = true },
                        modifier = Modifier.fillMaxSize(),
                        routeNodes = graphData?.nodes,
                        activeNodeId = realTimeActiveNodeId,
                    )
                }

                if (isDeveloperMode && showStats) {
                    StatisticsPanel(
                        onClose = { showStats = false },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )
                }

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

                if (isWaitingForUser) {
                    UserWaitDialog(
                        waitMessage = waitMessage,
                        onConfirm = onConfirmUserWait
                    )
                }

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
}


/**
 * 통합 상태 UI
 */
@OptIn(ExperimentalResourceApi::class)
@Composable
private fun EmptyStateUI(
    connectionStatus: ConnectionStatus,
    statusHistory: List<StatusLogEntry>,
    isDeveloperMode: Boolean,
    receivedMessage: String,
    onReconnect: () -> Unit,
    onClearStatusHistory: () -> Unit,
    onShowGraph: () -> Unit,
    modifier: Modifier = Modifier,
    routeNodes: List<com.vowser.client.ui.graph.GraphNode>? = null,
    activeNodeId: String? = null,
) {
    val listState = rememberLazyListState()

    // 새 로그가 추가될 때 자동 스크롤
    LaunchedEffect(statusHistory.size) {
        if (statusHistory.isNotEmpty()) {
            listState.animateScrollToItem(statusHistory.size - 1)
        }
    }

    Column(
        modifier = modifier
            .padding(AppTheme.Dimensions.paddingMedium)
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.spacedBy(AppTheme.Dimensions.paddingSmall)
    ) {
        // 상단 버튼 영역
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            // 재연결 버튼
            OutlinedButton(
                onClick = onReconnect,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.onBackground
                ),
                contentPadding = PaddingValues(AppTheme.Dimensions.paddingMedium, 0.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(AppTheme.Dimensions.borderRadiusSmall)
            ) {
                Icon(
                    painter = painterResource("drawable/refresh.png"),
                    contentDescription = "Reconnect",
                    modifier = Modifier.size(AppTheme.Dimensions.iconSizeSmall)
                )
                Spacer(modifier = Modifier.width(AppTheme.Dimensions.paddingSmall))
                Text("재연결")
            }

            Spacer(Modifier.width(AppTheme.Dimensions.paddingSmall))

            // 개발자 모드 전용 그래프 보기 버튼
            if (isDeveloperMode && receivedMessage != "No message" || statusHistory.any { it.type == StatusLogType.SUCCESS }) {
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

            Spacer(modifier = Modifier.weight(1f))

            // 클리어 버튼
            OutlinedButton(
                onClick = onClearStatusHistory,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.onBackground
                ),
                contentPadding = PaddingValues(AppTheme.Dimensions.paddingMedium, 0.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(AppTheme.Dimensions.borderRadiusSmall)
            ) {
                Icon(
                    painter = painterResource("drawable/bin.png"),
                    contentDescription = "Clear logs",
                    modifier = Modifier.size(AppTheme.Dimensions.iconSizeSmall)
                )
                Spacer(modifier = Modifier.width(AppTheme.Dimensions.paddingSmall))
                Text("Clear")
            }
        }

        // 상태 로그 영역
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(AppTheme.Dimensions.paddingSmall)
                .background(MaterialTheme.colorScheme.background),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shape = RoundedCornerShape(AppTheme.Dimensions.borderRadiusXLarge),
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                if (!routeNodes.isNullOrEmpty()) {
                    val steps = routeNodes
                    val activeIdx = steps.indexOfFirst { it.id == activeNodeId }.let { if (it >= 0) it else null }
                    Box(
                        modifier = Modifier
                            .width(200.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(AppTheme.Dimensions.paddingMedium)
                    ) {
                        com.vowser.client.ui.graph.LineDiagram(
                            steps = steps,
                            activeIndex = activeIdx,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    // 연결 상태 표시
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(top = AppTheme.Dimensions.paddingMedium, end = AppTheme.Dimensions.paddingMedium),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Canvas(
                            modifier = Modifier
                                .padding(end = AppTheme.Dimensions.paddingSmall)
                                .size(12.dp)
                        ) {
                            drawCircle(color = connectionStatus.displayColor)
                        }
                        Text(
                            text = connectionStatus.displayText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                        contentPadding = PaddingValues(AppTheme.Dimensions.paddingSmall),
                        verticalArrangement = Arrangement.spacedBy(AppTheme.Dimensions.paddingXSmall)
                    ) {
                        items(statusHistory) { logEntry ->
                            StatusLogItem(logEntry)
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
            StatusLogType.SUCCESS -> AppTheme.Colors.Success to "✓"
            StatusLogType.ERROR -> AppTheme.Colors.Error to "✗"
            StatusLogType.WARNING -> AppTheme.Colors.Warning to "⚠"
            StatusLogType.INFO -> MaterialTheme.colorScheme.primary to "ℹ"
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
 * 사용자 작업 완료 대기 다이얼로그
 */
@Composable
private fun UserWaitDialog(
    waitMessage: String,
    onConfirm: () -> Unit
) {
    AlertDialog(
        containerColor = MaterialTheme.colorScheme.background,
        onDismissRequest = { /* 사용자가 임의로 닫을 수 없음 */ },
        title = {
            Text(
                text = "사용자 입력",
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(AppTheme.Dimensions.paddingSmall)
            ) {
                Text(
                    text = waitMessage.ifBlank { "입력 값이 필요합니다." },
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "작업을 완료하신 후 확인을 눌러주세요.",
                    color = MaterialTheme.colorScheme.background,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black,
                    contentColor = Color.White
                )
            ) {
                Text("확인")
            }
        },
    )
}
