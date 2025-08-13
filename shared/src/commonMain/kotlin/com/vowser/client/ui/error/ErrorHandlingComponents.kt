package com.vowser.client.ui.error

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * 예외처리 및 로딩 상태를 위한 컴포넌트
 * - 우아한 에러 표시
 * - 스마트 로딩 인디케이터
 * - 네트워크 연결 상태 표시
 * - 사용자 친화적 오류 복구 옵션
 */

// 에러 상태 열거형
sealed class ErrorState {
    object None : ErrorState()
    data class NetworkError(val message: String, val isRetryable: Boolean = true) : ErrorState()
    data class ValidationError(val message: String, val field: String? = null) : ErrorState()
    data class ServerError(val message: String, val code: Int? = null) : ErrorState()
    data class UnknownError(val message: String, val exception: Throwable? = null) : ErrorState()
}

// 로딩 상태 열거형
sealed class LoadingState {
    object Idle : LoadingState()
    object Loading : LoadingState()
    data class Success(val message: String? = null) : LoadingState()
    data class Error(val errorState: ErrorState) : LoadingState()
}

@Composable
fun SmartLoadingIndicator(
    loadingState: LoadingState,
    modifier: Modifier = Modifier,
    showPercentage: Boolean = false,
    currentProgress: Float = 0f,
    loadingMessage: String = "로드 중...",
    onRetry: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null
) {
    AnimatedVisibility(
        visible = loadingState is LoadingState.Loading || loadingState is LoadingState.Error,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier.padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = 16.dp
        ) {
            Box(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.9f))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                when (loadingState) {
                    is LoadingState.Loading -> {
                        LoadingContent(
                            message = loadingMessage,
                            showPercentage = showPercentage,
                            progress = currentProgress
                        )
                    }
                    is LoadingState.Error -> {
                        ErrorContent(
                            errorState = loadingState.errorState,
                            onRetry = onRetry,
                            onDismiss = onDismiss
                        )
                    }
                    else -> {}
                }
            }
        }
    }
}

@Composable
private fun LoadingContent(
    message: String,
    showPercentage: Boolean,
    progress: Float
) {
    val infiniteTransition = rememberInfiniteTransition()
    
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier.size(80.dp),
            contentAlignment = Alignment.Center
        ) {
            if (showPercentage && progress > 0f) {
                CircularProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0969DA),
                    strokeWidth = 6.dp
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(pulseScale)
                        .rotate(rotationAngle)
                ) {
                    val strokeWidth = 6.dp.toPx()
                    drawArc(
                        color = Color(0xFF0969DA),
                        startAngle = 0f,
                        sweepAngle = 270f,
                        useCenter = false,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = strokeWidth,
                            cap = StrokeCap.Round
                        ),
                        size = androidx.compose.ui.geometry.Size(size.width, size.height)
                    )
                }
            }
        }
        
        Text(
            text = message,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
        
        if (!showPercentage) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(3) { index ->
                    val animationDelay = index * 200
                    val dotScale by infiniteTransition.animateFloat(
                        initialValue = 0.5f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600, delayMillis = animationDelay),
                            repeatMode = RepeatMode.Reverse
                        )
                    )
                    
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .scale(dotScale)
                            .background(Color.White.copy(alpha = 0.7f), CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorContent(
    errorState: ErrorState,
    onRetry: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.width(300.dp)
    ) {
        // 에러 아이콘
        val (icon, iconColor) = when (errorState) {
            is ErrorState.NetworkError -> Icons.Default.Warning to Color(0xFFFF9800)
            is ErrorState.ValidationError -> Icons.Default.Warning to Color(0xFFFFC107)
            is ErrorState.ServerError -> Icons.Default.Warning to Color(0xFFFF4444)
            is ErrorState.UnknownError -> Icons.Default.Warning to Color(0xFF9C27B0)
            else -> Icons.Default.Warning to Color(0xFFFF4444)
        }
        
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(iconColor.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "Error",
                tint = iconColor,
                modifier = Modifier.size(40.dp)
            )
        }
        
        // 에러 타이틀
        Text(
            text = when (errorState) {
                is ErrorState.NetworkError -> "연결 오류"
                is ErrorState.ValidationError -> "입력 오류"
                is ErrorState.ServerError -> "서버 오류"
                is ErrorState.UnknownError -> "알 수 없는 오류"
                else -> "오류 발생"
            },
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        // 에러 메시지
        Text(
            text = when (errorState) {
                is ErrorState.NetworkError -> errorState.message
                is ErrorState.ValidationError -> errorState.message
                is ErrorState.ServerError -> errorState.message
                is ErrorState.UnknownError -> errorState.message
                else -> "문제가 발생했습니다."
            },
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        
        // 액션 버튼들
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (onDismiss != null) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    )
                ) {
                    Text("닫기")
                }
            }
            
            if (onRetry != null && isRetryable(errorState)) {
                Button(
                    onClick = onRetry,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = iconColor
                    )
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Retry",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("다시 시도", color = Color.White)
                }
            }
        }
    }
}

private fun isRetryable(errorState: ErrorState): Boolean {
    return when (errorState) {
        is ErrorState.NetworkError -> errorState.isRetryable
        is ErrorState.ServerError -> true
        is ErrorState.UnknownError -> true
        is ErrorState.ValidationError -> false
        else -> false
    }
}

@Composable
fun NetworkConnectionIndicator(
    connectionStatus: String,
    modifier: Modifier = Modifier,
    onReconnect: (() -> Unit)? = null
) {
    val isConnected = connectionStatus.lowercase().contains("connected")
    val isConnecting = connectionStatus.lowercase().contains("connecting")
    
    AnimatedVisibility(
        visible = !isConnected,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .padding(8.dp)
                .background(
                    color = if (isConnecting) Color(0xFFFFA500) else Color(0xFFFF4444),
                    shape = RoundedCornerShape(8.dp)
                ),
            shape = RoundedCornerShape(8.dp),
            elevation = 8.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isConnecting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = "Disconnected",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
                
                Text(
                    text = if (isConnecting) "연결 중..." else "연결 끊김",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                
                if (!isConnecting && onReconnect != null) {
                    TextButton(
                        onClick = onReconnect,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color.White
                        ),
                        modifier = Modifier.padding(0.dp)
                    ) {
                        Text(
                            text = "재연결",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ErrorBoundary(
    errorState: ErrorState,
    onRetry: (() -> Unit)? = null,
    onReportError: ((ErrorState) -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        when (errorState) {
            is ErrorState.None -> {
                content()
            }
            else -> {
                ErrorFallbackUI(
                    errorState = errorState,
                    onRetry = onRetry,
                    onReportError = onReportError,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun ErrorFallbackUI(
    errorState: ErrorState,
    onRetry: (() -> Unit)? = null,
    onReportError: ((ErrorState) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF1A1A1A),
                    Color(0xFF2D2D2D)
                )
            )
        ),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(32.dp)
                .background(
                    color = Color.Black.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(16.dp)
                ),
            shape = RoundedCornerShape(16.dp),
            elevation = 16.dp
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 큰 에러 아이콘
                val (icon, iconColor) = when (errorState) {
                    is ErrorState.NetworkError -> Icons.Default.Warning to Color(0xFFFF9800)
                    is ErrorState.ValidationError -> Icons.Default.Warning to Color(0xFFFFC107)
                    is ErrorState.ServerError -> Icons.Default.Warning to Color(0xFFFF4444)
                    is ErrorState.UnknownError -> Icons.Default.Warning to Color(0xFF9C27B0)
                    else -> Icons.Default.Warning to Color(0xFFFF4444)
                }
                
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .background(iconColor.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = "Error",
                        tint = iconColor,
                        modifier = Modifier.size(60.dp)
                    )
                }
                
                Text(
                    text = "문제가 발생했습니다",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = when (errorState) {
                        is ErrorState.NetworkError -> "네트워크 연결을 확인해주세요.\n${errorState.message}"
                        is ErrorState.ValidationError -> "입력 정보를 확인해주세요.\n${errorState.message}"
                        is ErrorState.ServerError -> "서버에 문제가 발생했습니다.\n잠시 후 다시 시도해주세요."
                        is ErrorState.UnknownError -> "예상치 못한 문제가 발생했습니다.\n${errorState.message}"
                        else -> "알 수 없는 오류가 발생했습니다."
                    },
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )
                
                // 액션 버튼들
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (onRetry != null && isRetryable(errorState)) {
                        Button(
                            onClick = onRetry,
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = iconColor
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Retry",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("다시 시도", color = Color.White)
                        }
                    }
                    
                    if (onReportError != null) {
                        OutlinedButton(
                            onClick = { onReportError(errorState) },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = "Report",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("문제 신고")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ToastMessage(
    message: String,
    type: ToastType = ToastType.INFO,
    isVisible: Boolean,
    duration: Long = 3000L,
    onDismiss: () -> Unit
) {
    LaunchedEffect(isVisible) {
        if (isVisible) {
            delay(duration)
            onDismiss()
        }
    }
    
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut()
    ) {
        Card(
            modifier = Modifier
                .padding(16.dp)
                .background(
                    color = when (type) {
                        ToastType.SUCCESS -> Color(0xFF4CAF50)
                        ToastType.ERROR -> Color(0xFFFF4444)
                        ToastType.WARNING -> Color(0xFFFF9800)
                        ToastType.INFO -> Color(0xFF2196F3)
                    },
                    shape = RoundedCornerShape(8.dp)
                ),
            shape = RoundedCornerShape(8.dp),
            elevation = 8.dp
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = when (type) {
                        ToastType.SUCCESS -> Icons.Default.CheckCircle
                        ToastType.ERROR -> Icons.Default.Warning
                        ToastType.WARNING -> Icons.Default.Warning
                        ToastType.INFO -> Icons.Default.Info
                    },
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                
                Text(
                    text = message,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

enum class ToastType {
    SUCCESS,
    ERROR,
    WARNING,
    INFO
}