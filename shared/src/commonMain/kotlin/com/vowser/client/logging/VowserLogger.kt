package com.vowser.client.logging

/**
 * Vowser logging utilities
 */
object LogUtils {
    private val sensitiveDataRegex = Regex(
        "(sessionId|apiKey|token|key)\\s*[=:]\\s*[^&\\s,}]+|\"(sessionId|apiKey|token|key)\"\\s*:\\s*\"[^\"]+\"",
        RegexOption.IGNORE_CASE
    )

    /**
     * Filter sensitive information for production logging
     */
    fun filterSensitive(data: String): String =
        sensitiveDataRegex.replace(data) { "${it.groupValues[1]}=***" }
}

/**
 * Hierarchical logging tags
 */
object Tags {
    const val VOWSER = "Vowser"

    // Network related
    const val NETWORK = "Vowser.Network"
    const val NETWORK_WEBSOCKET = "Vowser.Network.WebSocket"
    const val API = "Vowser.Network.API"

    // Browser related
    const val BROWSER = "Vowser.Browser"
    const val BROWSER_AUTOMATION = "Vowser.Browser.Automation"
    const val BROWSER_PLAYWRIGHT = "Vowser.Browser.Playwright"
    const val BROWSER_ACTIONS = "Vowser.Browser.Actions"
    const val BROWSER_NAVIGATION = "Vowser.Browser.Navigation"

    // UI related
    const val UI_GRAPH = "Vowser.UI.Graph"

    // Exception related
    const val EXCEPTION_HANDLER = "Vowser.Exception.Handler"
    const val EXCEPTION_RECOVERY = "Vowser.Exception.Recovery"

    // Media related
    const val MEDIA_RECORDING = "Vowser.Media.Recording"
    const val MEDIA_SPEECH = "Vowser.Media.Speech"

    // Contribution related
    const val CONTRIBUTION = "Vowser.Contribution"
    const val CONTRIBUTION_MODE = "Vowser.Contribution.Mode"

    // System related
    const val SYSTEM_SHUTDOWN = "Vowser.System.Shutdown"

    // App related
    const val APP_VIEWMODEL = "Vowser.App.ViewModel"
}