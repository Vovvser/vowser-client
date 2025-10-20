package com.vowser.client.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.vowser.client.AppViewModel
import com.vowser.client.StatusLogEntry
import com.vowser.client.StatusLogType
import com.vowser.client.contribution.ContributionStatus
import com.vowser.client.ui.components.GenericAppBar
import com.vowser.client.ui.components.SttModeSelector
import com.vowser.client.ui.navigation.LocalScreenNavigator
import com.vowser.client.ui.theme.AppTheme
import com.vowser.client.websocket.ConnectionStatus
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

private enum class ContributionDialogOrigin {
    BackNavigation,
    CompletionButton
}

private data class ContributionDialogState(
    val origin: ContributionDialogOrigin
)

/**
 * 기여모드 전용 화면
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalResourceApi::class)
@Composable
fun ContributionScreen(
    appViewModel: AppViewModel
) {
    val navigator = LocalScreenNavigator.current
    val connectionStatus by appViewModel.connectionStatus.collectAsState()
    val contributionStatus by appViewModel.contributionStatus.collectAsState()
    val contributionStepCount by appViewModel.contributionStepCount.collectAsState()
    val contributionTask by appViewModel.contributionTask.collectAsState()
    val statusHistory by appViewModel.statusHistory.collectAsState()
    val isRecording by appViewModel.isRecording.collectAsState()
    val awaitingTask by appViewModel.awaitingContributionTask.collectAsState()
    val pendingContributionTask by appViewModel.pendingContributionTask.collectAsState()
    val isContributionInitializing by appViewModel.isContributionInitializing.collectAsState()
    val selectedSttModes by appViewModel.selectedSttModes.collectAsState()

    var dialogState by remember { mutableStateOf<ContributionDialogState?>(null) }
    var taskInput by remember { mutableStateOf("") }


    val beginContribution: (String) -> Unit = { rawTask ->
        if (isContributionInitializing) {
            appViewModel.notifyContributionInitializing()
        } else {
            val trimmed = rawTask.trim()
            if (trimmed.isNotEmpty() && awaitingTask) {
                taskInput = trimmed
                appViewModel.startContribution(trimmed)
                appViewModel.clearPendingContributionTask()
            }
        }
    }

    LaunchedEffect(contributionStatus) {
        if (contributionStatus == ContributionStatus.INACTIVE) {
            appViewModel.requestContributionTaskInput()
            taskInput = ""
        }
    }

    LaunchedEffect(awaitingTask) {
        if (awaitingTask) {
            taskInput = ""
        }
    }

    LaunchedEffect(pendingContributionTask) {
        val task = pendingContributionTask?.trim()
        if (awaitingTask && !task.isNullOrEmpty()) {
            beginContribution(task)
        }
    }

    val handleBackPress: () -> Unit = {
        when {
            isContributionInitializing -> {
                dialogState = null
                appViewModel.notifyContributionInitializing()
            }

            contributionStatus == ContributionStatus.INACTIVE -> navigator.pop()
            else -> dialogState = ContributionDialogState(ContributionDialogOrigin.BackNavigation)
        }
    }

    val handleStop: (ContributionDialogOrigin) -> Unit = { origin ->
        if (isContributionInitializing) {
            appViewModel.notifyContributionInitializing()
        } else {
            appViewModel.cancelContribution()
            if (origin == ContributionDialogOrigin.BackNavigation) {
                navigator.pop()
            }
        }
        dialogState = null
    }

    val handleComplete: (ContributionDialogOrigin) -> Unit = { origin ->
        if (isContributionInitializing) {
            appViewModel.notifyContributionInitializing()
        } else {
            appViewModel.stopContribution()
            if (origin == ContributionDialogOrigin.BackNavigation) {
                navigator.pop()
            }
        }
        dialogState = null
    }

    Scaffold(
        topBar = {
            GenericAppBar(
                title = "Contribution Mode",
                onBackPress = handleBackPress
            )
        }
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val maxWidth = this.maxWidth
            val maxHeight = this.maxHeight

            ContributionContent(
                connectionStatus = connectionStatus,
                contributionStatus = contributionStatus,
                contributionStepCount = contributionStepCount,
                contributionTask = contributionTask,
                statusHistory = statusHistory,
                isContributionInitializing = isContributionInitializing,
                onStopContribution = {
                    dialogState = ContributionDialogState(ContributionDialogOrigin.CompletionButton)
                },
                onReconnect = { appViewModel.reconnect() },
                onClearStatusHistory = { appViewModel.clearStatusHistory() },
                modifier = Modifier.fillMaxSize()
            )

            if (awaitingTask) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    event.changes.forEach { it.consume() }
                                }
                            }
                        }
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // STT 모드 선택
                if (awaitingTask) {
                    SttModeSelector(
                        selectedMode = selectedSttModes.firstOrNull(),
                        onModeSelect = { mode -> appViewModel.toggleSttMode(mode) },
                        isVisible = !isRecording,
                    )
                }

                ContributionTaskInputBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = maxWidth * 0.1f,
                            vertical = maxHeight * 0.05f
                        ),
                    value = taskInput,
                    isRecording = isRecording,
                    awaitingTask = awaitingTask,
                    onValueChange = { taskInput = it },
                    onSubmit = beginContribution,
                    onToggleRecording = { appViewModel.toggleRecording() }
                )
            }

            CompleteContributionDialog(
                state = dialogState,
                onDismiss = { dialogState = null },
                onStop = handleStop,
                onComplete = handleComplete
            )
        }
    }
}

/**
 * 기여모드 콘텐츠
 */
@OptIn(ExperimentalResourceApi::class)
@Composable
private fun ContributionContent(
    connectionStatus: ConnectionStatus,
    contributionStatus: ContributionStatus,
    contributionStepCount: Int,
    contributionTask: String,
    statusHistory: List<StatusLogEntry>,
    isContributionInitializing: Boolean,
    onStopContribution: () -> Unit,
    onReconnect: () -> Unit,
    onClearStatusHistory: () -> Unit,
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
        modifier = modifier
            .padding(
                AppTheme.Dimensions.paddingMedium,
                AppTheme.Dimensions.paddingMedium,
                AppTheme.Dimensions.paddingMedium,
                bottom = AppTheme.Dimensions.paddingXLarge + AppTheme.Dimensions.paddingXLarge + AppTheme.Dimensions.paddingMedium
            )
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.spacedBy(AppTheme.Dimensions.paddingSmall)
    ) {
        // 상단 버튼 영역
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
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

            Spacer(Modifier.width(AppTheme.Dimensions.paddingSmall))

            // 기여 완료 버튼
            if (contributionStatus != ContributionStatus.INACTIVE) {
                Button(
                    onClick = onStopContribution,
                    enabled = !isContributionInitializing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onBackground,
                        contentColor = MaterialTheme.colorScheme.background
                    ),
                    contentPadding = PaddingValues(AppTheme.Dimensions.paddingMedium, 0.dp),
                    shape = RoundedCornerShape(AppTheme.Dimensions.borderRadiusSmall)
                ) {
                    Icon(
                        painter = painterResource("drawable/accept.png"),
                        contentDescription = "Submit contribution logs",
                        modifier = Modifier.size(AppTheme.Dimensions.iconSizeSmall)
                    )
                    Spacer(modifier = Modifier.width(AppTheme.Dimensions.paddingSmall))
                    Text("기여 완료")
                }
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

            Column {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
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
                                    text = "브라우저에서 작업을 시작하세요!\n모든 클릭, 입력, 탐색이 자동으로 기록됩니다.",
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    } else {
                        items(statusHistory) { logEntry ->
                            StatusLogItem(logEntry)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
private fun ContributionTaskInputBar(
    modifier: Modifier,
    value: String,
    isRecording: Boolean,
    awaitingTask: Boolean,
    onValueChange: (String) -> Unit,
    onSubmit: (String) -> Unit,
    onToggleRecording: () -> Unit
) {
    Row(
        modifier = modifier
            .shadow(
                elevation = AppTheme.Dimensions.cardElevation,
                shape = RoundedCornerShape(AppTheme.Dimensions.borderRadiusXLarge),
                clip = true
            )
            .background(
                MaterialTheme.colorScheme.background,
                RoundedCornerShape(AppTheme.Dimensions.borderRadiusXLarge)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(AppTheme.Dimensions.borderRadiusXLarge)
            )
            .padding(8.dp, 0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .background(
                    MaterialTheme.colorScheme.surface,
                    RoundedCornerShape(AppTheme.Dimensions.borderRadiusXLarge)
                )
                .padding(0.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                enabled = awaitingTask,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = { onSubmit(value) }
                ),
                decorationBox = { inner ->
                    if (value.isEmpty()) {
                        Text(
                            text = "어떤 작업을 기록하시나요?",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    inner()
                }
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(onClick = onToggleRecording, enabled = awaitingTask) {
            if (isRecording) {
                Icon(
                    imageVector = Icons.Filled.Clear,
                    contentDescription = "Stop Recording",
                    tint = AppTheme.Colors.Error
                )
            } else {
                Icon(
                    painter = painterResource("drawable/microphone.png"),
                    contentDescription = "Start Recording",
                    modifier = Modifier.size(AppTheme.Dimensions.iconSizeMedium),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

/**
 * 상태 로그 아이템
 */
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
            text = "[" + entry.timestamp + "] ",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(end = AppTheme.Dimensions.paddingSmall)
        )

        Text(
            text = icon,
            fontSize = 12.sp,
            modifier = Modifier.padding(end = AppTheme.Dimensions.paddingXSmall)
        )

        Text(
            text = entry.message,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun CompleteContributionDialog(
    state: ContributionDialogState?,
    onDismiss: () -> Unit,
    onStop: (ContributionDialogOrigin) -> Unit,
    onComplete: (ContributionDialogOrigin) -> Unit
) {
    val dialogState = state ?: return

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(0.9f),
            shape = RoundedCornerShape(AppTheme.Dimensions.borderRadiusLarge)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(AppTheme.Dimensions.paddingLarge),
                verticalArrangement = Arrangement.spacedBy(AppTheme.Dimensions.paddingSmall)
            ) {
                Text(
                    "기여를 완료하시겠습니까?",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    "완료를 선택하면 수집된 단계가 서버로 전송됩니다. 중단을 선택하면 현재까지의 단계가 모두 폐기됩니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.height(AppTheme.Dimensions.paddingMedium))

                Row(
                    modifier = Modifier.align(Alignment.End),
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.Dimensions.paddingMedium)
                ) {
                    OutlinedButton(
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.background,
                            contentColor = MaterialTheme.colorScheme.onBackground,
                            disabledContainerColor = MaterialTheme.colorScheme.surface,
                            disabledContentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                        shape = RoundedCornerShape(AppTheme.Dimensions.borderRadius),
                        onClick = onDismiss
                    ) {
                        Text(
                            "취소",
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    OutlinedButton(
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.background,
                            contentColor = MaterialTheme.colorScheme.onBackground,
                            disabledContainerColor = MaterialTheme.colorScheme.surface,
                            disabledContentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                        shape = RoundedCornerShape(AppTheme.Dimensions.borderRadius),
                        onClick = { onStop(dialogState.origin) }) {
                        Text(
                            "중단",
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    OutlinedButton(
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.onBackground,
                            contentColor = MaterialTheme.colorScheme.background,
                            disabledContainerColor = MaterialTheme.colorScheme.onSurface,
                            disabledContentColor = MaterialTheme.colorScheme.surface,
                        ),
                        shape = RoundedCornerShape(AppTheme.Dimensions.borderRadius),
                        onClick = { onComplete(dialogState.origin) }) {
                        Text(
                            "완료",
                            color = MaterialTheme.colorScheme.background,
                        )
                    }
                }
            }
        }
    }
}
