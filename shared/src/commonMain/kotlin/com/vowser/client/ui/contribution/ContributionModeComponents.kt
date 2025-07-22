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
            modifier = Modifier.align(Alignment.TopCenter)
        )
        
        // 실시간 클릭 피드백
        if (isRecording && lastClickedElement != null) {
            ClickFeedbackAnimation(
                elementName = lastClickedElement,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        
        // 기여 가이드 패널
        ContributionGuidePanel(
            isRecording = isRecording,
            currentStep = currentStep,
            modifier = Modifier.align(Alignment.CenterStart)
        )
        
        // 기록 컨트롤 패널
        RecordingControlPanel(
            isRecording = isRecording,
            onStopRecording = onStopRecording,
            onPauseRecording = onPauseRecording,
            onDiscardRecording = onDiscardRecording,
            modifier = Modifier.align(Alignment.BottomCenter)
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
        shape = RoundedCornerShape(16.dp),
        elevation = 12.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 기록 상태 인디케이터
            RecordingStatusIndicator(isRecording = isRecording)
            
            Column {
                Text(
                    text = if (isRecording) "경로 기록 중..." else "기여 모드 대기",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                
                if (isRecording) {
                    Text(
                        text = "단계: $currentStep / $totalSteps",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // 컨트롤 버튼들
            if (!isRecording) {
                Button(
                    onClick = onStartRecording,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFF00D4AA)
                    )
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("시작", color = Color.White)
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = onPauseRecording,
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                            .size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.PlayArrow, 
                            contentDescription = "Pause",
                            tint = Color.White
                        )
                    }
                    
                    IconButton(
                        onClick = onStopRecording,
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                            .size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.Check, 
                            contentDescription = "Stop",
                            tint = Color.White
                        )
                    }
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
    
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isRecording) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    
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
                // 기록 중 - 펄스링 효과
                drawCircle(
                    color = Color.White.copy(alpha = 0.3f),
                    radius = radius * 1.5f,
                    center = center
                )
                drawCircle(
                    color = Color.White,
                    radius = radius,
                    center = center
                )
            } else {
                // 대기 중 - 간단한 원
                drawCircle(
                    color = Color(0xFF00D4AA),
                    radius = radius,
                    center = center
                )
            }
        }
        
        Icon(
            imageVector = if (isRecording) Icons.Default.CheckCircle else Icons.Default.LocationOn,
            contentDescription = if (isRecording) "Recording" else "Ready",
            tint = if (isRecording) Color(0xFFFF4444) else Color.White,
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
        delay(2000)
        showAnimation = false
    }
    
    AnimatedVisibility(
        visible = showAnimation,
        enter = fadeIn() + slideInVertically { -it / 2 },
        exit = fadeOut() + slideOutVertically { -it / 2 },
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .background(
                    color = Color(0xFF00D4AA),
                    shape = RoundedCornerShape(12.dp)
                ),
            elevation = 8.dp,
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                
                Text(
                    text = "클릭됨: $elementName",
                    color = Color.White,
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
            backgroundColor = Color.Black.copy(alpha = 0.9f),
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
                        tint = Color(0xFF00D4AA),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "기여 가이드",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Divider(color = Color.White.copy(alpha = 0.3f))
                
                val guideSteps = listOf(
                    "1. 목표 페이지로 이동하세요",
                    "2. 필요한 요소들을 클릭하세요",
                    "3. 경로가 자동으로 기록됩니다",
                    "4. 완료되면 저장 버튼을 누르세요"
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
                                        Color(0xFF00D4AA) 
                                    else 
                                        Color.White.copy(alpha = 0.3f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (index < currentStep) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            } else {
                                Text(
                                    text = "${index + 1}",
                                    color = Color.White,
                                    fontSize = 10.sp
                                )
                            }
                        }
                        
                        Text(
                            text = step,
                            color = if (index < currentStep) 
                                Color.White 
                            else 
                                Color.White.copy(alpha = 0.6f),
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
            backgroundColor = Color.Black.copy(alpha = 0.9f),
            elevation = 12.dp,
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 일시정지 버튼
                Button(
                    onClick = onPauseRecording,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFFFFA500)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Pause",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("일시정지", color = Color.White, fontSize = 14.sp)
                }
                
                // 완료 버튼
                Button(
                    onClick = onStopRecording,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFF00D4AA)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Save",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("완료", color = Color.White, fontSize = 14.sp)
                }
                
                // 취소 버튼
                Button(
                    onClick = onDiscardRecording,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFFFF4444)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Discard",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("취소", color = Color.White, fontSize = 14.sp)
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
                .background(Color.Black.copy(alpha = 0.7f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .width(400.dp)
                    .clickable(enabled = false) { },
                backgroundColor = Color.White,
                elevation = 16.dp,
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 성공 아이콘
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(Color(0xFF00D4AA).copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = Color(0xFF00D4AA),
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    
                    Text(
                        text = "경로 기록 완료!",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    
                    Text(
                        text = "새로운 웹 탐색 경로가 성공적으로 기록되었습니다.",
                        fontSize = 14.sp,
                        color = Color.Black.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                    
                    // 경로 정보 카드
                    Card(
                        backgroundColor = Color(0xFFF8F9FA),
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
                    
                    // 액션 버튼들
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = onDiscard,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFFF4444)
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
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = Color(0xFF00D4AA)
                            )
                        ) {
                            Text("저장", color = Color.White)
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
            color = Color.Black.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Black
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