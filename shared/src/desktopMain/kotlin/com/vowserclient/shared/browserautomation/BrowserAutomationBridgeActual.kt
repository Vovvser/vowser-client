package com.vowserclient.shared.browserautomation

import io.github.aakira.napier.Napier

actual object BrowserAutomationBridge {
    actual suspend fun goBackInBrowser() {
        Napier.i { "Desktop: BrowserAutomationBridge.goBackInBrowser() 호출됨" }
        BrowserAutomationService.goBack()
    }

    actual suspend fun goForwardInBrowser() {
        Napier.i { "Desktop: BrowserAutomationBridge.goForwardInBrowser() 호출됨" }
        BrowserAutomationService.goForward()
    }

    actual suspend fun navigateInBrowser(url: String) {
        Napier.i { "Desktop: BrowserAutomationBridge.navigateInBrowser($url) 호출됨" }
        BrowserAutomationService.navigate(url)
    }
}