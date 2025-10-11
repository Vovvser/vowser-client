package com.vowser.client.ui.graph

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vowser.client.ui.theme.AppTheme

/**
 * 글래스모피즘 효과가 적용된 그래프 헤더 컴포넌트
 */
@Composable
fun GlassmorphismHeader(
    title: String,
    nodeCount: Int,
    isContributionMode: Boolean,
    onModeToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(AppTheme.Dimensions.paddingMedium),
        shape = RoundedCornerShape(AppTheme.Dimensions.borderRadiusXLarge),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.2f),
                            Color.White.copy(alpha = 0.1f)
                        )
                    )
                )
                .border(
                    width = 1.dp,  // 보더 너비
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.3f),
                            Color.White.copy(alpha = 0.1f)
                        )
                    ),
                    shape = RoundedCornerShape(AppTheme.Dimensions.borderRadiusXLarge)
                )
                .padding(AppTheme.Dimensions.paddingMedium)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = AppTheme.Typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$nodeCount 개 노드",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = AppTheme.Typography.bodyMedium
                    )
                }
                
                Row {
                    // 모드 토글 스위치
                    Switch(
                        checked = isContributionMode,
                        onCheckedChange = { onModeToggle() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF00D4AA),
                            checkedTrackColor = Color(0xFF00D4AA).copy(alpha = 0.5f),
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color.White.copy(alpha = 0.3f)
                        )
                    )
                    Spacer(modifier = Modifier.width(AppTheme.Dimensions.paddingSmall))
                    Text(
                        text = if (isContributionMode) "기여" else "탐색",
                        color = Color.White,
                        fontSize = AppTheme.Typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}