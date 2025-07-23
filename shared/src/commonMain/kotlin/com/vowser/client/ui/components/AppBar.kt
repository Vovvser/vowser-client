package com.vowser.client.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.vowser.client.ui.theme.AppTheme

/**
 * 앱 상단바 컴포넌트
 */
@Composable
fun ModernAppBar(
    connectionStatus: String,
    isContributionMode: Boolean,
    onSettingsClick: () -> Unit,
    onStatsToggle: () -> Unit,
    onModeToggle: () -> Unit,
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