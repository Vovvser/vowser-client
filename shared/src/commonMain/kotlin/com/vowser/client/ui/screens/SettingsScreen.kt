package com.vowser.client.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.vowser.client.ui.components.GenericAppBar
import com.vowser.client.ui.theme.AppTheme

/**
 * 설정 화면 컴포넌트
 */
@OptIn(ExperimentalMaterial3Api::class)
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
            GenericAppBar(title = "Settings", onBackPress = onBackPress)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(AppTheme.Dimensions.paddingMedium),
            verticalArrangement = Arrangement.spacedBy(AppTheme.Dimensions.paddingMedium)
        ) {
            // 테마 설정 카드
            Card(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(AppTheme.Dimensions.paddingMedium),
                    verticalArrangement = Arrangement.spacedBy(AppTheme.Dimensions.spacingMedium)
                ) {
                    Text(
                        text = "디스플레이",
                        style = MaterialTheme.typography.headlineSmall
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
            ) {
                Column(
                    modifier = Modifier.padding(AppTheme.Dimensions.paddingMedium),
                    verticalArrangement = Arrangement.spacedBy(AppTheme.Dimensions.spacingMedium)
                ) {
                    Text(
                        text = "접근성 설정",
                        style = MaterialTheme.typography.headlineSmall
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
            style = MaterialTheme.typography.bodyLarge
        )
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
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
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

