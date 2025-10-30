package com.vowser.client.browserautomation

import com.vowser.client.contribution.ContributionStep
import com.vowser.client.websocket.dto.NavigationPath
import io.github.aakira.napier.Napier
import com.vowser.client.logging.Tags
import com.vowser.client.browserautomation.SelectOption

actual object BrowserAutomationBridge {
    actual suspend fun executeNavigationPath(path: NavigationPath, timeout: Double?): Boolean {
        Napier.i("iOS: BrowserAutomationBridge.executeNavigationPath(${path.pathId}) - Not implemented yet", tag = Tags.BROWSER_AUTOMATION)
        // TODO: iOS 브라우저 자동화 구현
        return false
    }

    actual suspend fun startContributionRecording() {
    }

    actual fun stopContributionRecording() {
    }

    actual suspend fun navigate(url: String) {
    }

    actual fun setContributionRecordingCallback(callback: (ContributionStep) -> Unit) {
    }

    actual fun setContributionBrowserClosedCallback(callback: (() -> Unit)?) {
        Napier.i("iOS: setContributionBrowserClosedCallback - Not implemented", tag = Tags.BROWSER_AUTOMATION)
    }

    actual fun setBrowserClosedCallback(callback: (() -> Unit)?) {
        Napier.i("iOS: setBrowserClosedCallback - Not implemented", tag = Tags.BROWSER_AUTOMATION)
    }

    actual suspend fun waitForNetworkIdle() {
    }

    actual suspend fun getSelectOptions(selector: String): List<SelectOption> = emptyList()

    actual suspend fun selectOption(selector: String, value: String) {
    }

    actual suspend fun setInputValue(selector: String, value: String) {
    }
}
