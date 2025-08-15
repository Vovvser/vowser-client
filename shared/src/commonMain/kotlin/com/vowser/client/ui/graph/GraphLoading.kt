package com.vowser.client.ui.graph

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 로딩 그래프 애니메이션 컴포넌트
 */
@Composable
fun LoadingGraphAnimation(
    modifier: Modifier = Modifier,
    isContributionMode: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition()
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 로딩 스피너
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .scale(scale)
                    .graphicsLayer { rotationZ = rotation }
            ) {
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val strokeWidth = 6.dp.toPx()
                    drawArc(
                        color = if (isContributionMode) Color(0xFF00D4AA) else Color(0xFF0969DA),
                        startAngle = 0f,
                        sweepAngle = 270f,
                        useCenter = false,
                        style = Stroke(strokeWidth, cap = StrokeCap.Round),
                        size = androidx.compose.ui.geometry.Size(size.width, size.height)
                    )
                }
            }
            
            Text(
                text = if (isContributionMode) "경로를 기록하고 있습니다..." else "그래프를 로드하고 있습니다...",
                color = MaterialTheme.colors.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = "잠시만 기다려 주세요",
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
        }
    }
}