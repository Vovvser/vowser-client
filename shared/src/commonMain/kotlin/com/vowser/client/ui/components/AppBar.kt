package com.vowser.client.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vowser.client.ui.theme.AppTheme

/**
 * 앱 상단바 컴포넌트
 */
@Composable
fun ModernAppBar(
    connectionStatus: String,
    isContributionMode: Boolean,
    isRecording: Boolean,
    recordingStatus: String,
    onSettingsClick: () -> Unit,
    onStatsToggle: () -> Unit,
    onModeToggle: () -> Unit,
    onToggleRecording: () -> Unit,
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
            // 연결 상태 아이콘
            Icon(
                imageVector = if (connectionStatus.contains("Connected")) 
                    Icons.Default.CheckCircle else Icons.Default.Clear,
                contentDescription = "Connection Status",
                tint = if (connectionStatus.contains("Connected")) 
                    AppTheme.Colors.Contribution else AppTheme.Colors.Error
            )
            
            // 앱 제목
            Text(
                text = "Vowser",
                color = Color.White,
                style = MaterialTheme.typography.h6
            )
            
            Spacer(modifier = Modifier.weight(1f))

            // 기여 모드 토글
            Switch(
                checked = isContributionMode,
                onCheckedChange = { onModeToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = AppTheme.Colors.Contribution,
                    checkedTrackColor = AppTheme.Colors.Contribution.copy(alpha = 0.5f),
                    uncheckedThumbColor = Color.Gray,
                    uncheckedTrackColor = Color.DarkGray
                )
            )
            
            // 마이크 버튼
            AppBarMicrophoneButton(
                isRecording = isRecording,
                recordingStatus = recordingStatus,
                onToggleRecording = onToggleRecording
            )
            
            // 통계 버튼
            IconButton(onClick = onStatsToggle) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = "Statistics",
                    tint = Color.White
                )
            }
            
            // 설정 버튼
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

/**
 * 앱바용 마이크 버튼 컴포넌트
 */
@Composable
private fun AppBarMicrophoneButton(
    isRecording: Boolean,
    recordingStatus: String,
    onToggleRecording: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 녹음 상태 텍스트 (컴팩트하게)
        AnimatedVisibility(
            visible = recordingStatus != "Ready to record" && isRecording,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Text(
                text = "REC",
                fontSize = 8.sp,
                color = Color.Red,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }

        // 메인 마이크 버튼
        IconButton(
            onClick = onToggleRecording,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    if (isRecording) Color.Red.copy(alpha = 0.2f) else Color.Transparent
                )
        ) {
            AnimatedContent(
                targetState = isRecording,
                transitionSpec = {
                    scaleIn() + fadeIn() togetherWith scaleOut() + fadeOut()
                }
            ) { recording ->
                Text(
                    text = if (recording) "⏹" else "🎤",
                    fontSize = 16.sp,
                    color = if (recording) Color.Red else Color.White
                )
            }
        }

        // 녹음 중 펄스 인디케이터 (간소화)
        AnimatedVisibility(
            visible = isRecording,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.padding(top = 2.dp)
            ) {
                repeat(3) { index ->
                    var isVisible by remember { mutableStateOf(false) }
                    
                    LaunchedEffect(isRecording) {
                        while (isRecording) {
                            kotlinx.coroutines.delay(200L * (index + 1))
                            isVisible = !isVisible
                        }
                        isVisible = false
                    }
                    
                    Box(
                        modifier = Modifier
                            .size(3.dp)
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