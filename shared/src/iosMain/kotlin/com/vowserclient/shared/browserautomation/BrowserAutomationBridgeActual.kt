package com.vowserclient.shared.browserautomation

import io.github.aakira.napier.Napier

actual object BrowserAutomationBridge {
    actual suspend fun goBackInBrowser() {
        Napier.i { "iOS: BrowserAutomationBridge.goBackInBrowser() - Not implemented yet" }
        // TODO: iOS 브라우저 자동화 구현
    }

    actual suspend fun goForwardInBrowser() {
        Napier.i { "iOS: BrowserAutomationBridge.goForwardInBrowser() - Not implemented yet" }
        // TODO: iOS 브라우저 자동화 구현
    }

    actual suspend fun navigateInBrowser(url: String) {
        Napier.i { "iOS: BrowserAutomationBridge.navigateInBrowser($url) - Not implemented yet" }
        // TODO: iOS 브라우저 자동화 구현
    }
}