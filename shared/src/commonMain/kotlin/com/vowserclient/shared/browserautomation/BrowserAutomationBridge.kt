package com.vowserclient.shared.browserautomation

import com.vowser.client.websocket.dto.NavigationPath

expect object BrowserAutomationBridge {
    suspend fun goBackInBrowser()
    suspend fun goForwardInBrowser()
    suspend fun navigateInBrowser(url: String)
    suspend fun executeNavigationPath(path: NavigationPath)
}