package com.vowserclient.shared.browserautomation

expect object BrowserAutomationBridge {
    suspend fun goBackInBrowser()
    suspend fun goForwardInBrowser()
    suspend fun navigateInBrowser(url: String)
}