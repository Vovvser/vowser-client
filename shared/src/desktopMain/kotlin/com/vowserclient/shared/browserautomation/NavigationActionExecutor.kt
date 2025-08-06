package com.vowserclient.shared.browserautomation

import com.vowser.client.websocket.dto.NavigationStep
import io.github.aakira.napier.Napier

interface NavigationActionExecutor {
    suspend fun execute(browserActions: BrowserActions, step: NavigationStep)
}

class NavigateActionExecutor : NavigationActionExecutor {
    override suspend fun execute(browserActions: BrowserActions, step: NavigationStep) {
        browserActions.navigate(step.url)
    }
}

class ClickActionExecutor : NavigationActionExecutor {
    override suspend fun execute(browserActions: BrowserActions, step: NavigationStep) {
        step.selector?.let { selector ->
            browserActions.click(selector)
        } ?: Napier.e("Selector is null for click action.")
    }
}

class TypeActionExecutor : NavigationActionExecutor {
    override suspend fun execute(browserActions: BrowserActions, step: NavigationStep) {
        step.selector?.let { selector ->
            val textToType = step.htmlAttributes?.get("value") ?: step.htmlAttributes?.get("text") ?: ""
            browserActions.type(selector, textToType)
        } ?: Napier.e("Selector is null for type action.")
    }
}

class SubmitActionExecutor : NavigationActionExecutor {
    override suspend fun execute(browserActions: BrowserActions, step: NavigationStep) {
        step.selector?.let { selector ->
            ClickActionExecutor().execute(browserActions, step)
        } ?: Napier.e("Selector is null for submit action.")
    }
}
