package com.vowser.client.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 설정 화면 컴포넌트
 */
@Composable
fun SettingsScreen(
    isDarkTheme: Boolean,
    isDeveloperMode: Boolean,
    onThemeToggle: (Boolean) -> Unit,
    onDeveloperModeToggle: (Boolean) -> Unit,
    onBackPress: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("설정") },
                navigationIcon = {
                    IconButton(onClick = onBackPress) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                backgroundColor = MaterialTheme.colors.surface,
                elevation = 4.dp
            )
        },
        backgroundColor = MaterialTheme.colors.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 테마 설정 카드
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "디스플레이",
                        style = MaterialTheme.typography.h6
                    )
                    SettingItemSwitch(
                        label = "다크 모드",
                        isChecked = isDarkTheme,
                        onCheckedChange = onThemeToggle
                    )
                    SettingItemSwitch(
                        label = "개발자 모드",
                        isChecked = isDeveloperMode,
                        onCheckedChange = onDeveloperModeToggle
                    )
                }
            }

            // 접근성 설정 카드
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "접근성 설정",
                        style = MaterialTheme.typography.h6
                    )
                    SettingItem("음성 속도", "1.0x")
                    SettingItem("자동 하이라이트", "켜짐")
                    SettingItem("애니메이션", "켜짐")
                    SettingItem("키보드 단축키", "활성화")
                }
            }
        }
    }
}

/**
 * 개별 설정 항목 컴포넌트 (토글 스위치)
 */
@Composable
private fun SettingItemSwitch(
    label: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.body1
        )
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colors.primary,
                uncheckedThumbColor = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.disabled)
            )
        )
    }
}

/**
 * 개별 설정 항목 컴포넌트 (정보 표시)
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
            style = MaterialTheme.typography.body1
        )
        Text(
            text = value,
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium)
        )
    }
}
