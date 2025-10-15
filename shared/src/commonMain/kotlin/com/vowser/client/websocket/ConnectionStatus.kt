package com.vowser.client.websocket

import androidx.compose.ui.graphics.Color
import com.vowser.client.ui.theme.AppTheme

enum class ConnectionStatus(
    val displayText: String,
    val displayColor: Color,
) {
    Disconnected("연결 끊김", AppTheme.Colors.Error),
    Connecting("연결 중", AppTheme.Colors.Loading),
    Connected("연결됨", AppTheme.Colors.Green),
    Error("연결 끊김", AppTheme.Colors.Error)
}