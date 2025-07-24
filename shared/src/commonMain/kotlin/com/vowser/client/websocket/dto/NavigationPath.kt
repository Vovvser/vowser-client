package com.vowser.client.websocket.dto

import kotlinx.serialization.Serializable

@Serializable
data class NavigationPath(
    val pathId: String,
    val description: String,
    val steps: List<NavigationStep>
)

@Serializable
data class NavigationStep(
    val url: String,
    val title: String,
    val action: String, // e.g., "navigate", "click", "submit"
    val selector: String? = null, // CSS selector or XPath
    val elementType: String? = null,
    val htmlAttributes: Map<String, String>? = null,
    val waitCondition: String? = null // e.g., "networkidle", "domcontentloaded"
)
