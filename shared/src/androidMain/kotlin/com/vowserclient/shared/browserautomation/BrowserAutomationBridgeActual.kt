package com.vowserclient.shared.browserautomation

import com.vowser.client.websocket.dto.NavigationPath
import com.vowser.client.logging.VowserLogger
import com.vowser.client.logging.Tags

actual object BrowserAutomationBridge {
    actual suspend fun goBackInBrowser() {
        VowserLogger.info("Android: BrowserAutomationBridge.goBackInBrowser() - Not implemented yet", Tags.BROWSER_AUTOMATION)
        // TODO: Android 브라우저 자동화 구현
    }

    actual suspend fun goForwardInBrowser() {
        VowserLogger.info("Android: BrowserAutomationBridge.goForwardInBrowser() - Not implemented yet", Tags.BROWSER_AUTOMATION)
        // TODO: Android 브라우저 자동화 구현
    }

    actual suspend fun navigateInBrowser(url: String) {
        VowserLogger.info("Android: BrowserAutomationBridge.navigateInBrowser($url) - Not implemented yet", Tags.BROWSER_AUTOMATION)
        // TODO: Android 브라우저 자동화 구현
    }

    actual suspend fun executeNavigationPath(path: NavigationPath) {
        VowserLogger.info("Android: BrowserAutomationBridge.executeNavigationPath(${path.pathId}) - Not implemented yet", Tags.BROWSER_AUTOMATION)
        // TODO: Android 브라우저 자동화 구현
    }
}