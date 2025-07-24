package com.vowserclient.shared.browserautomation

import com.vowser.client.websocket.dto.NavigationPath
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

    actual suspend fun executeNavigationPath(path: NavigationPath) {
        Napier.i { "Desktop: BrowserAutomationBridge.executeNavigationPath(${path.pathId}) 호출됨" }
        for (step in path.steps) {
            Napier.i { "Executing step: ${step.action} on ${step.url} with selector ${step.selector}" }
            when (step.action) {
                "navigate" -> {
                    BrowserAutomationService.navigate(step.url)
                }
                "click" -> {
                    step.selector?.let { selector ->
                        BrowserAutomationService.clickElement(selector)
                    } ?: Napier.e("Selector is null for click action.")
                }
                "type" -> {
                    step.selector?.let { selector ->
                        val textToType = step.htmlAttributes?.get("value") ?: step.htmlAttributes?.get("text") ?: ""
                        BrowserAutomationService.typeText(selector, textToType)
                    } ?: Napier.e("Selector is null for type action.")
                }
                "submit" -> {
                    step.selector?.let { selector ->
                        BrowserAutomationService.clickElement(selector) // For now, treat submit as click
                    } ?: Napier.e("Selector is null for submit action.")
                }
                else -> {
                    Napier.w { "Unknown navigation action: ${step.action}" }
                }
            }
            // TODO: wait conditions (step.waitCondition) 추가
        }
        Napier.i { "Navigation path ${path.pathId} execution completed." }
    }
}