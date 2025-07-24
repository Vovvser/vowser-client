package com.vowser.client.websocket.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface WebSocketMessage {
    @Serializable
    @SerialName("browser_command")
    data class BrowserCommandWrapper(val data: BrowserCommand) : WebSocketMessage

    @Serializable
    @SerialName("navigation_path")
    data class NavigationPathWrapper(val data: NavigationPath) : WebSocketMessage
}
