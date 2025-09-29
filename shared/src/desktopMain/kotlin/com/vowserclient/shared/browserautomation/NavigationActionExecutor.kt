package com.vowserclient.shared.browserautomation

import com.vowser.client.websocket.dto.NavigationStep
import io.github.aakira.napier.Napier
import com.vowser.client.logging.Tags

interface NavigationActionExecutor {
    suspend fun execute(browserActions: BrowserActions, step: NavigationStep): Boolean
}

class NavigateActionExecutor : NavigationActionExecutor {
    override suspend fun execute(browserActions: BrowserActions, step: NavigationStep): Boolean {
        return try {
            browserActions.navigate(step.url)
            true
        } catch (e: Exception) {
            Napier.e("Navigate action failed: ${e.message}", e, tag = Tags.BROWSER_AUTOMATION)
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
                Napier.e("Click action failed: ${e.message}", e, tag = Tags.BROWSER_AUTOMATION)
                false
            }
        } ?: run {
            Napier.e("Selector is null for click action.", tag = Tags.BROWSER_AUTOMATION)
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
                Napier.e("Type action failed: ${e.message}", e, tag = Tags.BROWSER_AUTOMATION)
                false
            }
        } ?: run {
            Napier.e("Selector is null for type action.", tag = Tags.BROWSER_AUTOMATION)
            false
        }
    }
}

class SubmitActionExecutor : NavigationActionExecutor {
    override suspend fun execute(browserActions: BrowserActions, step: NavigationStep): Boolean {
        return step.selector?.let { selector ->
            ClickActionExecutor().execute(browserActions, step)
        } ?: run {
            Napier.e("Selector is null for submit action.", tag = Tags.BROWSER_AUTOMATION)
            false
        }
    }
}
