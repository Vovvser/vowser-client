package com.vowserclient.shared.browserautomation

import io.github.aakira.napier.Napier

actual object BrowserAutomationBridge {
    actual suspend fun goBackInBrowser() {
        Napier.i { "Android: BrowserAutomationBridge.goBackInBrowser() - Not implemented yet" }
        // TODO: Android 브라우저 자동화 구현
    }

    actual suspend fun goForwardInBrowser() {
        Napier.i { "Android: BrowserAutomationBridge.goForwardInBrowser() - Not implemented yet" }
        // TODO: Android 브라우저 자동화 구현
    }

    actual suspend fun navigateInBrowser(url: String) {
        Napier.i { "Android: BrowserAutomationBridge.navigateInBrowser($url) - Not implemented yet" }
        // TODO: Android 브라우저 자동화 구현
    }
}