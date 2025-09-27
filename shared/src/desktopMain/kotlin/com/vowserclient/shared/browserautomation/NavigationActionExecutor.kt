package com.vowserclient.shared.browserautomation

import com.vowser.client.websocket.dto.NavigationStep
import com.vowser.client.logging.VowserLogger
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
            VowserLogger.error("Navigate action failed: ${e.message}", Tags.BROWSER_AUTOMATION)
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
                VowserLogger.error("Click action failed: ${e.message}", Tags.BROWSER_AUTOMATION)
                false
            }
        } ?: run {
            VowserLogger.error("Selector is null for click action.", Tags.BROWSER_AUTOMATION)
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
                VowserLogger.error("Type action failed: ${e.message}", Tags.BROWSER_AUTOMATION)
                false
            }
        } ?: run {
            VowserLogger.error("Selector is null for type action.", Tags.BROWSER_AUTOMATION)
            false
        }
    }
}

class SubmitActionExecutor : NavigationActionExecutor {
    override suspend fun execute(browserActions: BrowserActions, step: NavigationStep): Boolean {
        return step.selector?.let { selector ->
            ClickActionExecutor().execute(browserActions, step)
        } ?: run {
            VowserLogger.error("Selector is null for submit action.", Tags.BROWSER_AUTOMATION)
            false
        }
    }
}
