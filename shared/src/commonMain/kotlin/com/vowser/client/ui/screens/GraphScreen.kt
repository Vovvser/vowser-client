package com.vowser.client.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.vowser.client.AppViewModel
import com.vowser.client.StatusLogEntry
import com.vowser.client.StatusLogType
import com.vowser.client.exception.DialogState
import com.vowser.client.ui.components.GenericAppBar
import com.vowser.client.ui.components.StatisticsPanel
import com.vowser.client.ui.components.SttModeSelector
import com.vowser.client.ui.components.StandardDialogs
import com.vowser.client.ui.error.ErrorBoundary
import com.vowser.client.ui.error.ErrorState
import com.vowser.client.ui.error.LoadingState
import com.vowser.client.ui.error.SmartLoadingIndicator
import com.vowser.client.ui.graph.ModernNetworkGraph
import com.vowser.client.ui.theme.AppTheme
import com.vowser.client.websocket.ConnectionStatus

/**
 * 그래프 메인 화면 컴포넌트
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphScreen(
    appViewModel: AppViewModel,
    isDeveloperMode: Boolean,
    onReconnect: () -> Unit,
    onSendToolCall: (String, Map<String, String>) -> Unit,
    onToggleRecording: () -> Unit,
    onClearStatusHistory: () -> Unit,
    onToggleSttMode: (String) -> Unit,
    onConfirmUserWait: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    val dialogState by appViewModel.dialogState.collectAsState()
    val connectionStatus by appViewModel.connectionStatus.collectAsState()
    val receivedMessage by appViewModel.receivedMessage.collectAsState()
    val isRecording by appViewModel.isRecording.collectAsState()
    val currentGraphDataState by appViewModel.currentGraphData.collectAsState()
    val statusHistory by appViewModel.statusHistory.collectAsState()
    val selectedSttModes by appViewModel.selectedSttModes.collectAsState()
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
                    onBackPress = onNavigateBack
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
                        allMatchedPaths = graphData.allMatchedPaths
                    )
                } else {
                    EmptyStateUI(
                        isRecording = isRecording,
                        connectionStatus = connectionStatus,
                        statusHistory = statusHistory,
                        isDeveloperMode = isDeveloperMode,
                        receivedMessage = receivedMessage,
                        selectedSttModes = selectedSttModes,
                        onToggleRecording = onToggleRecording,
                        onReconnect = onReconnect,
                        onClearStatusHistory = onClearStatusHistory,
                        onToggleSttMode = onToggleSttMode,
                        onShowGraph = { if (graphData != null) showGraphView = true },
                        modifier = Modifier.fillMaxSize(), // 수동 padding 제거
                        maxWidth = maxWidth,
                        maxHeight = maxHeight
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
@Composable
private fun EmptyStateUI(
    isRecording: Boolean,
    connectionStatus: ConnectionStatus,
    statusHistory: List<StatusLogEntry>,
    isDeveloperMode: Boolean,
    receivedMessage: String,
    selectedSttModes: Set<String>,
    onToggleRecording: () -> Unit,
    onReconnect: () -> Unit,
    onClearStatusHistory: () -> Unit,
    onShowGraph: () -> Unit,
    onToggleSttMode: (String) -> Unit,
    modifier: Modifier = Modifier,
    maxWidth: Dp,
    maxHeight: Dp
) {
    val listState = rememberLazyListState()

    // 새 로그가 추가될 때 자동 스크롤
    LaunchedEffect(statusHistory.size) {
        if (statusHistory.isNotEmpty()) {
            listState.animateScrollToItem(statusHistory.size - 1)
        }
    }

    Column(
        modifier = modifier.padding(horizontal = maxWidth * 0.05f),
        verticalArrangement = Arrangement.spacedBy(AppTheme.Dimensions.paddingMedium)
    ) {
        // 상단 버튼 영역
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.Dimensions.paddingSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
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

        // 연결 상태 표시
        Row (
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppTheme.Dimensions.paddingXSmall)
        )
        {
            Canvas(modifier = Modifier.size(12.dp)) {
                drawCircle(color = connectionStatus.displayColor)
            }
            Text(
                text = connectionStatus.displayText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        // STT 모드 선택기
        SttModeSelector(
            selectedModes = selectedSttModes,
            onModeToggle = onToggleSttMode,
            isVisible = !isRecording,
            modifier = Modifier.fillMaxWidth()
        )

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
 * 사용자 작업 완료 대기 다이얼로그
 */
@Composable
private fun UserWaitDialog(
    waitMessage: String,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* 사용자가 임의로 닫을 수 없음 */ },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppTheme.Dimensions.paddingSmall)
            ) {
                Text(
                    text = "⏸️",
                    fontSize = 24.sp
                )
                Text(
                    text = "사용자 작업 대기 중",
                    style = MaterialTheme.typography.titleLarge
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(AppTheme.Dimensions.paddingMedium)
            ) {
                Text(
                    text = waitMessage,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "작업을 완료하신 후 아래 버튼을 눌러주세요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppTheme.Colors.Success,
                    contentColor = Color.White
                )
            ) {
                Text("완료")
            }
        },
    )
}
