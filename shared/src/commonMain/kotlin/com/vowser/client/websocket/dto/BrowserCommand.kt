package com.vowser.client.websocket.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName // <--- 이 import 문을 추가하세요.

@Serializable
sealed class BrowserCommand {
    @Serializable
    @SerialName("go_back")
    data object GoBack : BrowserCommand()

    @Serializable
    @SerialName("go_forward")
    data object GoForward : BrowserCommand()

    @Serializable
    @SerialName("navigate")
    data class Navigate(val url: String) : BrowserCommand()
}