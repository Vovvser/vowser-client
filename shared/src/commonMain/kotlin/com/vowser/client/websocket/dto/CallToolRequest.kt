package com.vowser.client.websocket.dto

import kotlinx.serialization.Serializable

@Serializable
data class CallToolRequest(
    val toolName: String,
    val args: Map<String, String>
)