package com.vowser.client.ui.error

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.CircularProgressIndicator
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
import com.vowser.client.ui.theme.AppTheme

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
            modifier = Modifier.padding(AppTheme.Dimensions.paddingMedium),
            shape = RoundedCornerShape(AppTheme.Dimensions.borderRadiusXLarge),
            elevation = CardDefaults.cardElevation(defaultElevation = AppTheme.Dimensions.cardElevationMax)
        ) {
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                    .padding(AppTheme.Dimensions.paddingLarge),
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

    val primaryColor = MaterialTheme.colorScheme.primary

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppTheme.Dimensions.spacingLarge)
    ) {
        Box(
            modifier = Modifier.size(AppTheme.Dimensions.iconSizeXLarge),
            contentAlignment = Alignment.Center
        ) {
            if (showPercentage && progress > 0f) {
                CircularProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxSize(),
                    color = primaryColor,
                    strokeWidth = AppTheme.Dimensions.paddingSmall + 2.dp
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    color = MaterialTheme.colorScheme.onSurface,
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
                        color = primaryColor,
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
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = AppTheme.Typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
        
        if (!showPercentage) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(AppTheme.Dimensions.paddingXSmall)
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
                            .size(AppTheme.Dimensions.paddingSmall)
                            .scale(dotScale)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), CircleShape)
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
        verticalArrangement = Arrangement.spacedBy(AppTheme.Dimensions.spacingLarge),
        modifier = Modifier.width(300.dp)
    ) {
        // 에러 아이콘
        val (icon, iconColor) = when (errorState) {
            is ErrorState.NetworkError -> Icons.Default.Warning to AppTheme.Colors.Warning
            is ErrorState.ValidationError -> Icons.Default.Warning to AppTheme.Colors.Warning
            is ErrorState.ServerError -> Icons.Default.Warning to AppTheme.Colors.Error
            is ErrorState.UnknownError -> Icons.Default.Warning to AppTheme.Colors.Warning
            else -> Icons.Default.Warning to AppTheme.Colors.Error
        }
        
        Box(
            modifier = Modifier
                .size(AppTheme.Dimensions.iconSizeXLarge)
                .background(iconColor.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "Error",
                tint = iconColor,
                modifier = Modifier.size(AppTheme.Dimensions.iconSizeLarge)
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
            fontSize = AppTheme.Typography.titleMedium,
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
            fontSize = AppTheme.Typography.bodyMedium,
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        
        // 액션 버튼들
        Row(
            horizontalArrangement = Arrangement.spacedBy(AppTheme.Dimensions.spacingMedium),
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
                        containerColor = iconColor
                    )
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Retry",
                        modifier = Modifier.size(AppTheme.Dimensions.iconSizeSmall)
                    )
                    Spacer(modifier = Modifier.width(AppTheme.Dimensions.spacingXSmall))
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
                    AppTheme.Colors.StatusBackground,
                    Color(0xFF2D2D2D)
                )
            )
        ),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(AppTheme.Dimensions.paddingXLarge)
                .background(
                    color = Color.Black.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(AppTheme.Dimensions.borderRadiusXLarge)
                ),
            shape = RoundedCornerShape(AppTheme.Dimensions.borderRadiusXLarge),
            elevation = CardDefaults.cardElevation(defaultElevation = AppTheme.Dimensions.cardElevationMax)
        ) {
            Column(
                modifier = Modifier.padding(AppTheme.Dimensions.paddingXLarge),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AppTheme.Dimensions.spacingLarge)
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
                    fontSize = AppTheme.Typography.titleLarge,
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
                    fontSize = AppTheme.Typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    lineHeight = AppTheme.Typography.titleLarge
                )
                
                // 액션 버튼들
                Row(
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.Dimensions.spacingLarge)
                ) {
                    if (onRetry != null && isRetryable(errorState)) {
                        Button(
                            onClick = onRetry,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = iconColor
                            ),
                            shape = RoundedCornerShape(AppTheme.Dimensions.borderRadius)
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Retry",
                                modifier = Modifier.size(AppTheme.Dimensions.iconSizeSmall)
                            )
                            Spacer(modifier = Modifier.width(AppTheme.Dimensions.paddingSmall))
                            Text("다시 시도", color = Color.White)
                        }
                    }

                    if (onReportError != null) {
                        OutlinedButton(
                            onClick = { onReportError(errorState) },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(AppTheme.Dimensions.borderRadius)
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = "Report",
                                modifier = Modifier.size(AppTheme.Dimensions.iconSizeSmall)
                            )
                            Spacer(modifier = Modifier.width(AppTheme.Dimensions.paddingSmall))
                            Text("문제 신고")
                        }
                    }
                }
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
