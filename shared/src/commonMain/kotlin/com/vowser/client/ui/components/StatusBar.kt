package com.vowser.client.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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
            .padding(14.dp),
        elevation = 3.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 메시지 영역
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "최근: $receivedMessage",
                    style = MaterialTheme.typography.caption
                )
            }
            
            // 모의 테스트 버튼
            if (isDeveloperMode) {
                Button(
                    onClick = onTestCommand,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (currentVoiceTest != null) 
                            AppTheme.Colors.Contribution else AppTheme.Colors.Success,
                        contentColor = Color.White
                    )
                ) {
                    Text("모의 테스트")
                }
                
                Spacer(modifier = Modifier.width(7.dp))
            }
            
        }
    }
}