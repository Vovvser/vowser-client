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
    onReconnect: () -> Unit,
    onTestCommand: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        backgroundColor = Color.Black.copy(alpha = 0.8f),
        elevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 메시지 영역
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (currentVoiceTest != null) "🎤 음성 테스트" else "최근: $receivedMessage",
                    color = Color.White,
                    style = MaterialTheme.typography.caption
                )
                currentVoiceTest?.let { test ->
                    Text(
                        text = "\"${test.voiceCommand}\" → ${test.description}",
                        color = AppTheme.Colors.Contribution,
                        style = MaterialTheme.typography.caption,
                        maxLines = 1
                    )
                }
            }
            
            // 음성 테스트 버튼
            Button(
                onClick = onTestCommand,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = if (currentVoiceTest != null) 
                        AppTheme.Colors.Contribution else AppTheme.Colors.Success
                )
            ) {
                Text(
                    text = if (currentVoiceTest != null) "다음 테스트" else "음성 테스트",
                    color = Color.White
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // 재연결 버튼
            Button(
                onClick = onReconnect,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFF8250DF)
                )
            ) {
                Text("재연결", color = Color.White)
            }
        }
    }
}