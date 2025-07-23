package com.vowser.client.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.vowser.client.ui.theme.AppTheme

/**
 * 설정 화면 컴포넌트
 */
@Composable
fun SettingsScreen(
    onBackPress: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 헤더
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = onBackPress) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Text(
                text = "설정",
                color = Color.White,
                style = MaterialTheme.typography.h4
            )
        }
        
        // 접근성 설정 카드
        Card(
            backgroundColor = Color.Black.copy(alpha = 0.7f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "접근성 설정",
                    color = Color.White,
                    style = MaterialTheme.typography.h6
                )
                
                // 설정 항목들
                SettingItem("음성 속도", "1.0x")
                SettingItem("자동 하이라이트", "켜짐")
                SettingItem("애니메이션", "켜짐")
                SettingItem("키보드 단축키", "활성화")
            }
        }
    }
}

/**
 * 개별 설정 항목 컴포넌트
 */
@Composable
private fun SettingItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color.White,
            style = MaterialTheme.typography.body1
        )
        Text(
            text = value,
            color = AppTheme.Colors.Contribution,
            style = MaterialTheme.typography.body2
        )
    }
}