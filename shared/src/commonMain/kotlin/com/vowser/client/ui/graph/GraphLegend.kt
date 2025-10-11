package com.vowser.client.ui.graph

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
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
 * 그래프 범례 컴포넌트
 */
@Composable
fun ModernLegend(
    isContributionMode: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(AppTheme.Dimensions.borderRadiusLarge),
        elevation = CardDefaults.cardElevation(defaultElevation = AppTheme.Dimensions.cardElevation)
    ) {
        Column(
            modifier = Modifier.padding(AppTheme.Dimensions.spacingMedium),
            verticalArrangement = Arrangement.spacedBy(6.dp)  // 특수 간격
        ) {
            Text(
                text = "범례",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = AppTheme.Typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            
            NodeType.values().forEach { nodeType ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)  // 특수 간격
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(nodeType.color)
                    )
                    Text(
                        text = when (nodeType) {
                            NodeType.START -> "시작점"
                            NodeType.WEBSITE -> "웹사이트"
                            NodeType.ACTION -> "액션"
                        },
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        fontSize = 11.sp
                    )
                }
            }
            
            if (isContributionMode) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "🔴 기여 모드 활성",
                    color = Color(0xFF9F4147),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}