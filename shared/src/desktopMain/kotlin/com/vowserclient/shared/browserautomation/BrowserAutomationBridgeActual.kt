package com.vowser.client.browserautomation

import com.vowser.client.websocket.dto.NavigationPath
import com.vowser.client.contribution.ContributionStep
import com.vowser.client.contribution.ContributionConstants
import io.github.aakira.napier.Napier
import com.vowser.client.logging.Tags
import kotlinx.coroutines.delay

actual object BrowserAutomationBridge {

    private val browserActions = BrowserActions(BrowserAutomationService)

    private val actionExecutors = mapOf(
        "navigate" to NavigateActionExecutor(),
        "click" to ClickActionExecutor(),
        "type" to TypeActionExecutor(),
        "submit" to SubmitActionExecutor()
    )

    actual suspend fun executeNavigationPath(path: NavigationPath, timeout: Double?): Boolean {
        Napier.i("Executing single step for path: ${path.pathId}", tag = Tags.BROWSER_AUTOMATION)

        BrowserAutomationService.initialize()

        val step = path.steps.firstOrNull()
        if (step == null) {
            Napier.e("No step found in NavigationPath", tag = Tags.BROWSER_AUTOMATION)
            return false
        }

        Napier.i("Executing step: ${step.action} on ${step.url} with selector ${step.selector}", tag = Tags.BROWSER_AUTOMATION)

        val executor = actionExecutors[step.action]
        if (executor == null) {
            Napier.w("Unknown navigation action or no executor found: ${step.action}", tag = Tags.BROWSER_AUTOMATION)
            return false
        }

        val success = executor.execute(browserActions, step, timeout)

        if (!success) {
            Napier.e("Step (${step.action} on ${step.selector}) failed.", tag = Tags.BROWSER_AUTOMATION)
            return false
        }

        Napier.i("Single step execution completed successfully.", tag = Tags.BROWSER_AUTOMATION)
        return true
    }

    // 기여 모드 관련 메서드들
    actual suspend fun startContributionRecording() {
        Napier.i("Starting contribution recording", tag = Tags.BROWSER_AUTOMATION)
        BrowserAutomationService.startContributionRecording()
    }
    
    actual fun stopContributionRecording() {
        Napier.i("Stopping contribution recording", tag = Tags.BROWSER_AUTOMATION)
        BrowserAutomationService.stopContributionRecording()
    }

    actual fun setContributionBrowserClosedCallback(callback: (() -> Unit)?) {
        Napier.i("Setting contribution browser closed callback", tag = Tags.BROWSER_AUTOMATION)
        BrowserAutomationService.setContributionBrowserClosedCallback(callback)
    }

    actual suspend fun cleanupContribution() {
        Napier.i("Cleaning up contribution browser resources", tag = Tags.BROWSER_AUTOMATION)
        BrowserAutomationService.cleanup()
    }

    actual suspend fun navigate(url: String) {
        Napier.i("Navigating to: $url", tag = Tags.BROWSER_AUTOMATION)
        BrowserAutomationService.navigate(url)
    }
    
    actual fun setContributionRecordingCallback(callback: (ContributionStep) -> Unit) {
        Napier.i("Setting contribution recording callback", tag = Tags.BROWSER_AUTOMATION)
        BrowserAutomationService.setContributionRecordingCallback(callback)
    }

    actual suspend fun waitForNetworkIdle() {
        Napier.i("Waiting for network idle", tag = Tags.BROWSER_AUTOMATION)
        BrowserAutomationService.waitForNetworkIdle()
    }
}