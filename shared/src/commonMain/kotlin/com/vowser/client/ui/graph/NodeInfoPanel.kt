package com.vowser.client.ui.graph

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vowser.client.ui.theme.AppTheme

/**
 * 노드 정보 패널 컴포넌트
 */
@Composable
fun NodeInfoPanel(
    node: GraphNode,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(AppTheme.Dimensions.paddingMedium),
        shape = RoundedCornerShape(AppTheme.Dimensions.borderRadiusXLarge),
        elevation = CardDefaults.cardElevation(defaultElevation = AppTheme.Dimensions.cardElevationXHigh),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
    ) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .padding(AppTheme.Dimensions.paddingMedium)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "노드 정보",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = AppTheme.Typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 노드 타입 배지
                Box(
                    modifier = Modifier
                        .background(
                            color = node.type.color.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(AppTheme.Dimensions.borderRadius)
                        )
                        .border(
                            width = 1.dp,
                            color = node.type.color,
                            shape = RoundedCornerShape(AppTheme.Dimensions.borderRadius)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = when (node.type) {
                            NodeType.NAVIGATE -> "이동"
                            NodeType.CLICK -> "클릭"
                            NodeType.INPUT -> "입력"
                            NodeType.WAIT -> "대기"
                            NodeType.START -> "시작점"
                            NodeType.WEBSITE -> "웹사이트"
                            NodeType.ACTION -> "액션"
                        },
                        color = node.type.color,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = node.label,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                
                Text(
                    text = "ID: ${node.id}",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 추가 정보
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    InfoChip("위치", "(${node.x.toInt()}, ${node.y.toInt()})")
                    InfoChip("크기", "${node.type.size.toInt()}px")
                }
            }
        }
    }
}

/**
 * 정보 칩 컴포넌트
 */
@Composable
fun InfoChip(label: String, value: String) {
    Column {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            fontSize = 10.sp
        )
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}