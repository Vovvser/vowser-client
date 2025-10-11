package com.vowser.client.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.vowser.client.data.VoiceTestScenario
import com.vowser.client.ui.theme.AppTheme

/**
 * 하단 상태바 컴포넌트
 */
@Composable
fun StatusBar(
    receivedMessage: String,
    currentVoiceTest: VoiceTestScenario? = null,
    isDeveloperMode: Boolean = false,
    onTestCommand: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(AppTheme.Dimensions.paddingLarge),
        elevation = CardDefaults.cardElevation(defaultElevation = AppTheme.Dimensions.cardElevation)
    ) {
        Row(
            modifier = Modifier.padding(AppTheme.Dimensions.paddingLarge),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 메시지 영역
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "최근: $receivedMessage",
                    fontSize = AppTheme.Typography.bodySmall
                )
            }
            
            // 모의 테스트 버튼
            if (isDeveloperMode) {
                Button(
                    onClick = onTestCommand,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (currentVoiceTest != null)
                            AppTheme.Colors.Contribution else AppTheme.Colors.Success,
                        contentColor = Color.White
                    )
                ) {
                    Text("모의 테스트")
                }
                
                Spacer(modifier = Modifier.width(AppTheme.Dimensions.paddingSmall))
            }
            
        }
    }
}