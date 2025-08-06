package com.vowser.client.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun VoiceRecordingButton(
    isRecording: Boolean,
    recordingStatus: String,
    onToggleRecording: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        // 상태 텍스트
        AnimatedVisibility(
            visible = recordingStatus != "Ready to record",
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            Card(
                elevation = 4.dp,
                backgroundColor = MaterialTheme.colors.surface,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text(
                    text = recordingStatus,
                    fontSize = 12.sp,
                    color = if (isRecording) Color.Red else MaterialTheme.colors.onSurface,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        // 메인 녹음 버튼
        FloatingActionButton(
            onClick = onToggleRecording,
            backgroundColor = if (isRecording) Color.Red else MaterialTheme.colors.primary,
            contentColor = Color.White,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
        ) {
            AnimatedContent(
                targetState = isRecording,
                transitionSpec = {
                    scaleIn() + fadeIn() togetherWith scaleOut() + fadeOut()
                }
            ) { recording ->
                Text(
                    text = if (recording) "⏹" else "🎤",
                    fontSize = 24.sp,
                    color = Color.White
                )
            }
        }

        // 녹음 중 애니메이션 인디케이터
        AnimatedVisibility(
            visible = isRecording,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                repeat(3) { index ->
                    var isVisible by remember { mutableStateOf(false) }
                    
                    LaunchedEffect(isRecording) {
                        while (isRecording) {
                            kotlinx.coroutines.delay(300L * (index + 1))
                            isVisible = !isVisible
                        }
                        isVisible = false
                    }
                    
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(
                                if (isVisible) Color.Red else Color.Gray.copy(alpha = 0.3f)
                            )
                    )
                }
            }
        }
    }
}