package com.vowserclient.shared.browserautomation

import com.vowser.client.websocket.dto.NavigationStep
import io.github.aakira.napier.Napier

interface NavigationActionExecutor {
    suspend fun execute(step: NavigationStep)
}

class NavigateActionExecutor : NavigationActionExecutor {
    override suspend fun execute(step: NavigationStep) {
        BrowserAutomationService.navigate(step.url)
    }
}

class ClickActionExecutor : NavigationActionExecutor {
    override suspend fun execute(step: NavigationStep) {
        step.selector?.let { selector ->
            BrowserAutomationService.clickElement(selector)
        } ?: Napier.e("Selector is null for click action.")
    }
}

class TypeActionExecutor : NavigationActionExecutor {
    override suspend fun execute(step: NavigationStep) {
        step.selector?.let { selector ->
            val textToType = step.htmlAttributes?.get("value") ?: step.htmlAttributes?.get("text") ?: ""
            BrowserAutomationService.typeText(selector, textToType)
        } ?: Napier.e("Selector is null for type action.")
    }
}

class SubmitActionExecutor : NavigationActionExecutor {
    override suspend fun execute(step: NavigationStep) {
        step.selector?.let { selector ->
            BrowserAutomationService.clickElement(selector)
        } ?: Napier.e("Selector is null for submit action.")
    }
}
