package com.vowserclient.shared.browserautomation

import com.vowser.client.websocket.dto.NavigationPath
import io.github.aakira.napier.Napier

actual object BrowserAutomationBridge {

    private val actionExecutors = mapOf(
        "navigate" to NavigateActionExecutor(),
        "click" to ClickActionExecutor(),
        "type" to TypeActionExecutor(),
        "submit" to SubmitActionExecutor()
    )

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
            actionExecutors[step.action]?.execute(step)
                ?: Napier.w { "Unknown navigation action or no executor found: ${step.action}" }
            // TODO: step.waitCondition 기반 wait conditions 추가
        }
        Napier.i { "Navigation path ${path.pathId} execution completed." }
    }
}