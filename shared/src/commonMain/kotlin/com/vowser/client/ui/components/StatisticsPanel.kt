package com.vowser.client.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vowser.client.ui.theme.AppTheme
import com.vowser.client.navigation.NavigationProcessor

/**
 * 그래프 통계 패널 컴포넌트
 */
@Composable
fun StatisticsPanel(
    navigationProcessor: NavigationProcessor,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stats by navigationProcessor.graphStatistics.collectAsState()

    Card(
        modifier = modifier
            .width(300.dp)
            .padding(AppTheme.Dimensions.paddingMedium),
        elevation = CardDefaults.cardElevation(defaultElevation = AppTheme.Dimensions.cardElevationXHigh)
    ) {
        Column(
            modifier = Modifier.padding(AppTheme.Dimensions.paddingMedium),
            verticalArrangement = Arrangement.spacedBy(AppTheme.Dimensions.paddingSmall)
        ) {
            // 헤더
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "그래프 통계",
                    style = MaterialTheme.typography.headlineSmall
                )
                IconButton(onClick = onClose) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close"
                    )
                }
            }

            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
            // 통계 항목들
            StatItem("총 노드", "${stats.totalNodes}개")
            StatItem("총 관계", "${stats.totalRelationships}개")
            StatItem("평균 클릭수", "${stats.averageClicks}")
            StatItem("평균 시간", "${stats.averageTime.toInt()}초")
        }
    }
}

/**
 * 통계 항목 컴포넌트
 */
@Composable
private fun StatItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}