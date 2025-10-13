package com.vowser.client.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
            GenericAppBar(title = "Setting", onBackPress = onBackPress)
        }
    ) { paddingValues ->
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val maxWidth = this.maxWidth
            val maxHeight = this.maxHeight
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(maxWidth * 0.04f, maxHeight * 0.02f),
                verticalArrangement = Arrangement.spacedBy(AppTheme.Dimensions.paddingSmall)
            ) {
                // 테마 설정 카드
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(maxWidth * 0.03f, maxHeight * 0.02f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.background
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(
                            horizontal = maxWidth * 0.03f,
                            vertical = maxHeight * 0.03f
                        ),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "디스플레이",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        SwitchRow(
                            title = "다크 모드",
                            subtitle = "화면을 어두운 테마로 표시합니다",
                            checked = isDarkTheme,
                            onCheckedChange = onThemeToggle
                        )

                        SwitchRow(
                            title = "개발자 모드",
                            subtitle = "개발자를 위한 고급 기능을 활성화합니다",
                            checked = isDeveloperMode,
                            onCheckedChange = onDeveloperModeToggle
                        )
                    }
                }

                // 접근성 설정 카드
                Card(
                    modifier = Modifier.fillMaxWidth()
                        .padding(maxWidth * 0.03f,
                            maxHeight * 0.03f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
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
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

/**
 * 토글 설정 항목 컴포넌트
 */
@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground)

            Text(subtitle, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.surface,
                uncheckedTrackColor = MaterialTheme.colorScheme.outline
            )
        )
    }
}


