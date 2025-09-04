package com.vowser.client.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vowser.client.ui.theme.AppTheme

/**
 * 앱 상단바 컴포넌트
 */
@Composable
fun ModernAppBar(
    connectionStatus: String,
    onSettingsClick: () -> Unit,
    onStatsToggle: () -> Unit,
    showHomeButton: Boolean = false,
    onHomeClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 연결 상태 아이콘
        Icon(
            imageVector = if (connectionStatus.contains("Connected")) 
                Icons.Default.CheckCircle else Icons.Default.Clear,
            contentDescription = "Connection Status",
            tint = if (connectionStatus.contains("Connected")) 
                AppTheme.Colors.Success else AppTheme.Colors.Error
        )
        
        // 앱 제목
        Text(
            text = "Vowser",
            style = MaterialTheme.typography.h6,
            color = MaterialTheme.colors.onSurface
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        // 홈 버튼 (그래프에서 EmptyStateUI로 돌아가기)
        if (showHomeButton) {
            IconButton(onClick = onHomeClick) {
                Icon(
                    Icons.Default.Home,
                    contentDescription = "Home",
                    tint = MaterialTheme.colors.onSurface
                )
            }
        }
        
        // 통계 버튼
        IconButton(onClick = onStatsToggle) {
            Icon(
                Icons.Default.Info,
                contentDescription = "Statistics",
                tint = MaterialTheme.colors.onSurface
            )
        }
        
        // 설정 버튼
        IconButton(onClick = onSettingsClick) {
            Icon(
                Icons.Default.Settings,
                contentDescription = "Settings",
                tint = MaterialTheme.colors.onSurface
            )
        }
    }
}

