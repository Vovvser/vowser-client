package com.vowser.client.logging

import io.github.aakira.napier.Napier

/**
 * Vowser logging utilities
 */
object VowserLogger {

    private val isDevelopment by lazy { detectDevelopmentEnvironment() }
    private val sensitiveDataRegex by lazy {
        Regex("(sessionId|apiKey|token|key)\\s*[=:]\\s*[^&\\s,}]+|\"(sessionId|apiKey|token|key)\"\\s*:\\s*\"[^\"]+\"", RegexOption.IGNORE_CASE)
    }

    private fun detectDevelopmentEnvironment(): Boolean = true

    private inline fun logIfEnabled(level: () -> Unit) {
        if (isDevelopment) level()
    }

    fun debug(message: String, tag: String = Tags.VOWSER, throwable: Throwable? = null) {
        logIfEnabled { Napier.d(message, throwable, tag = tag) }
    }

    fun info(message: String, tag: String = Tags.VOWSER, throwable: Throwable? = null) {
        logIfEnabled { Napier.i(message, throwable, tag = tag) }
    }

    fun warn(message: String, tag: String = Tags.VOWSER, throwable: Throwable? = null) {
        logIfEnabled { Napier.w(message, throwable, tag = tag) }
    }

    fun error(message: String, tag: String = Tags.VOWSER, throwable: Throwable? = null) {
        Napier.e(message, throwable, tag = tag)
    }

    fun filterSensitiveData(data: String): String =
        if (isDevelopment) data else sensitiveDataRegex.replace(data) { "${it.groupValues[1]}=***" }

    fun logUrl(url: String): String = url
    fun logUserInput(input: String): String = input
}

/**
 * Hierarchical logging tags
 */
object Tags {
    const val VOWSER = "Vowser"

    // Network related
    const val NETWORK = "Vowser.Network"
    const val NETWORK_WEBSOCKET = "Vowser.Network.WebSocket"

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