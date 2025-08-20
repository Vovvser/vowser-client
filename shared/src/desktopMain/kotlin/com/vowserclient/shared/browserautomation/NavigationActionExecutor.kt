package com.vowserclient.shared.browserautomation

import com.vowser.client.websocket.dto.NavigationStep
import io.github.aakira.napier.Napier

interface NavigationActionExecutor {
    suspend fun execute(browserActions: BrowserActions, step: NavigationStep): Boolean
}

class NavigateActionExecutor : NavigationActionExecutor {
    override suspend fun execute(browserActions: BrowserActions, step: NavigationStep): Boolean {
        return try {
            browserActions.navigate(step.url)
            true
        } catch (e: Exception) {
            Napier.e("Navigate action failed: ${e.message}")
            false
        }
    }
}

class ClickActionExecutor : NavigationActionExecutor {
    override suspend fun execute(browserActions: BrowserActions, step: NavigationStep): Boolean {
        return step.selector?.let { selector ->
            try {
                browserActions.click(selector)
            } catch (e: Exception) {
                Napier.e("Click action failed: ${e.message}")
                false
            }
        } ?: run {
            Napier.e("Selector is null for click action.")
            false
        }
    }
}

class TypeActionExecutor : NavigationActionExecutor {
    override suspend fun execute(browserActions: BrowserActions, step: NavigationStep): Boolean {
        return step.selector?.let { selector ->
            try {
                val textToType = step.htmlAttributes?.get("value") ?: step.htmlAttributes?.get("text") ?: ""
                browserActions.type(selector, textToType)
                true
            } catch (e: Exception) {
                Napier.e("Type action failed: ${e.message}")
                false
            }
        } ?: run {
            Napier.e("Selector is null for type action.")
            false
        }
    }
}

class SubmitActionExecutor : NavigationActionExecutor {
    override suspend fun execute(browserActions: BrowserActions, step: NavigationStep): Boolean {
        return step.selector?.let { selector ->
            ClickActionExecutor().execute(browserActions, step)
        } ?: run {
            Napier.e("Selector is null for submit action.")
            false
        }
    }
}
