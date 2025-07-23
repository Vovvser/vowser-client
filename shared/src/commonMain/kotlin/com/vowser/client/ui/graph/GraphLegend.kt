package com.vowser.client.ui.graph

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 모던 그래프 범례 컴포넌트
 */
@Composable
fun ModernLegend(
    isContributionMode: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .background(
                color = Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "범례",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            
            NodeType.values().forEach { nodeType ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                            NodeType.PAGE -> "페이지"
                            NodeType.ACTION -> "액션"
                            NodeType.DEFAULT -> "기본"
                        },
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 11.sp
                    )
                }
            }
            
            if (isContributionMode) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "🔴 기여 모드 활성",
                    color = Color(0xFFFF4444),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}