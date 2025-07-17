package com.vowser.client.websocket.dto

import kotlinx.serialization.Serializable

@Serializable
sealed class BrowserCommand {
    @Serializable data object GoBack : BrowserCommand()
    @Serializable data object GoForward : BrowserCommand()
    @Serializable data class Navigate(val url: String) : BrowserCommand()
}