package com.vowser.client.browserautomation

import com.vowser.client.websocket.dto.NavigationPath
import io.github.aakira.napier.Napier
import com.vowser.client.logging.Tags

actual object BrowserAutomationBridge {
    actual suspend fun goBackInBrowser() {
        Napier.i("Android: BrowserAutomationBridge.goBackInBrowser() - Not implemented yet", tag = Tags.BROWSER_AUTOMATION)
        // TODO: Android 브라우저 자동화 구현
    }

    actual suspend fun goForwardInBrowser() {
        Napier.i("Android: BrowserAutomationBridge.goForwardInBrowser() - Not implemented yet", tag = Tags.BROWSER_AUTOMATION)
        // TODO: Android 브라우저 자동화 구현
    }

    actual suspend fun navigateInBrowser(url: String) {
        Napier.i("Android: BrowserAutomationBridge.navigateInBrowser($url) - Not implemented yet", tag = Tags.BROWSER_AUTOMATION)
        // TODO: Android 브라우저 자동화 구현
    }

    actual suspend fun executeNavigationPath(path: NavigationPath) {
        Napier.i("Android: BrowserAutomationBridge.executeNavigationPath(${path.pathId}) - Not implemented yet", tag = Tags.BROWSER_AUTOMATION)
        // TODO: Android 브라우저 자동화 구현
    }
}