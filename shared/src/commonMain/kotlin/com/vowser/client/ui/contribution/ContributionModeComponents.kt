package com.vowser.client.ui.contribution

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vowser.client.ui.theme.AppTheme
import kotlinx.coroutines.delay

/**
 * 기여 모드 전용 UI 컴포넌트들
 * - 경로 기록 상태 표시
 * - 실시간 클릭 감지 시각화
 * - 기여 진행 상황 표시
 * - 접근성 친화적 피드백
 */
@Composable
fun ContributionModeOverlay(
    isRecording: Boolean,
    currentStep: Int,
    totalSteps: Int,
    lastClickedElement: String?,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onPauseRecording: () -> Unit,
    onDiscardRecording: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        // 기록 상태 헤더
        ContributionHeader(
            isRecording = isRecording,
            currentStep = currentStep,
            totalSteps = totalSteps,
            onStartRecording = onStartRecording,
            onStopRecording = onStopRecording,
            onPauseRecording = onPauseRecording,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 80.dp)
        )

        // 실시간 클릭 피드백
        if (isRecording && lastClickedElement != null) {
            ClickFeedbackAnimation(
                elementName = lastClickedElement,
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = 100.dp)
            )
        }

        // 기여 가이드 패널
        ContributionGuidePanel(
            isRecording = isRecording,
            currentStep = currentStep,
            modifier = Modifier.align(Alignment.TopStart).padding(start = 16.dp, top = 172.dp)
        )

        // 기록 컨트롤 패널
        RecordingControlPanel(
            isRecording = isRecording,
            onStopRecording = onStopRecording,
            onPauseRecording = onPauseRecording,
            onDiscardRecording = onDiscardRecording,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 84.dp)
        )
    }
}

@Composable
private fun ContributionHeader(
    isRecording: Boolean,
    currentStep: Int,
    totalSteps: Int,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onPauseRecording: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(16.dp),
        shape = RoundedCornerShape(AppTheme.Dimensions.borderRadiusXLarge),
        elevation = AppTheme.Dimensions.cardElevationXHigh
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 기록 상태 인디케이터
            RecordingStatusIndicator(isRecording = isRecording)

            Column {
                Text(
                    text = if (isRecording) "경로 기록 중..." else "기여 모드 대기",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                if (isRecording) {
                    Text(
                        text = "단계: $currentStep / $totalSteps",
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            if (!isRecording) {
                Button(onClick = onStartRecording, content = {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colors.onPrimary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("시작", color = MaterialTheme.colors.onPrimary)
                })
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = onPauseRecording,
                        modifier = Modifier
                            .background(MaterialTheme.colors.onSurface.copy(alpha = 0.2f), CircleShape)
                            .size(40.dp),
                        content = {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "Pause",
                                tint = MaterialTheme.colors.onSurface
                            )
                        }
                    )

                    IconButton(
                        onClick = onStopRecording,
                        modifier = Modifier
                            .background(MaterialTheme.colors.onSurface.copy(alpha = 0.2f), CircleShape)
                            .size(40.dp),
                        content = {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Stop",
                                tint = MaterialTheme.colors.onSurface
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun RecordingStatusIndicator(
    isRecording: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording) 1.3f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        )
    )

    val primaryColor = MaterialTheme.colors.primary
    val secondaryColor = MaterialTheme.colors.secondary

    Box(
        modifier = modifier.size(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .scale(pulseScale)
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 4f

            if (isRecording) {
                drawCircle(
                    color = primaryColor.copy(alpha = 0.3f),
                    radius = radius * 1.5f,
                    center = center
                )
                drawCircle(
                    color = primaryColor,
                    radius = radius,
                    center = center
                )
            } else {
                drawCircle(
                    color = secondaryColor,
                    radius = radius,
                    center = center
                )
            }
        }

        Icon(
            imageVector = if (isRecording) Icons.Default.CheckCircle else Icons.Default.LocationOn,
            contentDescription = if (isRecording) "Recording" else "Ready",
            tint = if (isRecording) primaryColor else secondaryColor,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun ClickFeedbackAnimation(
    elementName: String,
    modifier: Modifier = Modifier
) {
    var showAnimation by remember { mutableStateOf(true) }

    LaunchedEffect(elementName) {
        showAnimation = true
        delay(3000)
        showAnimation = false
    }

    AnimatedVisibility(
        visible = showAnimation,
        enter = fadeIn() + slideInVertically { -it / 2 },
        exit = fadeOut() + slideOutVertically { -it / 2 },
        modifier = modifier
    ) {
        Card(
            elevation = 8.dp,
            shape = RoundedCornerShape(12.dp),
            backgroundColor = MaterialTheme.colors.primary
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colors.onPrimary,
                    modifier = Modifier.size(24.dp)
                )

                Text(
                    text = "클릭됨: $elementName",
                    color = MaterialTheme.colors.onPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun ContributionGuidePanel(
    isRecording: Boolean,
    currentStep: Int,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isRecording,
        enter = slideInHorizontally { -it } + fadeIn(),
        exit = slideOutHorizontally { -it } + fadeOut(),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .width(280.dp)
                .padding(16.dp),
            elevation = 8.dp,
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "기여 가이드",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.3f))

                val guideSteps = listOf(
                    "1. 목표 페이지로 이동하세요",
                    "2. 필요한 요소들을 클릭하세요",
                    "3. 경로가 자동으로 기록됩니다",
                    "4. 완료되면 저장 버튼을 누르세요."
                )

                guideSteps.forEachIndexed { index, step ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(
                                    color = if (index < currentStep)
                                        MaterialTheme.colors.primary
                                    else
                                        MaterialTheme.colors.onSurface.copy(alpha = 0.3f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (index < currentStep) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colors.onPrimary,
                                    modifier = Modifier.size(12.dp)
                                )
                            } else {
                                Text(
                                    text = "${index + 1}",
                                    color = MaterialTheme.colors.onSurface,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Text(
                            text = step,
                            color = if (index < currentStep)
                                MaterialTheme.colors.onSurface
                            else
                                MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordingControlPanel(
    isRecording: Boolean,
    onStopRecording: () -> Unit,
    onPauseRecording: () -> Unit,
    onDiscardRecording: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isRecording,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier.padding(16.dp),
            elevation = AppTheme.Dimensions.cardElevationXHigh,
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onPauseRecording,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = AppTheme.Colors.Warning,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Pause",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("일시정지", fontSize = 14.sp)
                }

                Button(
                    onClick = onStopRecording,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = AppTheme.Colors.Success,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Save",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("완료", fontSize = 14.sp)
                }

                Button(
                    onClick = onDiscardRecording,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = AppTheme.Colors.Error,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Discard",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("취소", fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun ContributionSuccessDialog(
    isVisible: Boolean,
    pathName: String,
    stepCount: Int,
    estimatedTime: Int,
    onSave: () -> Unit,
    onEdit: () -> Unit,
    onDiscard: () -> Unit,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.onSurface.copy(alpha = 0.7f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .width(400.dp)
                    .clickable(enabled = false) { },
                elevation = 16.dp,
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(MaterialTheme.colors.primary.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = MaterialTheme.colors.primary,
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    Text(
                        text = "경로 기록 완료!",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "새로운 웹 탐색 경로가 성공적으로 기록되었습니다.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )

                    Card(
                        backgroundColor = MaterialTheme.colors.background,
                        elevation = 0.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            InfoRow("경로 이름", pathName)
                            InfoRow("단계 수", "${stepCount}단계")
                            InfoRow("예상 시간", "${estimatedTime}초")
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = onDiscard,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colors.error
                            )
                        ) {
                            Text("삭제")
                        }

                        OutlinedButton(
                            onClick = onEdit,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("수정")
                        }

                        Button(
                            onClick = onSave,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("저장")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// 접근성 피드백 (음성, 진동 등)
@Composable
fun AccessibilityFeedback(
    isEnabled: Boolean,
    feedbackType: FeedbackType,
    message: String
) {
    // 음성 피드백, 진동 피드백 등 구현
    LaunchedEffect(feedbackType, message) {
        if (isEnabled) {
            when (feedbackType) {
                FeedbackType.VOICE -> {
                    // TTS 구현
                    println("Voice feedback: $message")
                }
                FeedbackType.VIBRATION -> {
                    // 진동 피드백 구현
                    println("Vibration feedback")
                }
                FeedbackType.SOUND -> {
                    // 사운드 피드백 구현
                    println("Sound feedback")
                }
            }
        }
    }
}

enum class FeedbackType {
    VOICE,
    VIBRATION,
    SOUND
}