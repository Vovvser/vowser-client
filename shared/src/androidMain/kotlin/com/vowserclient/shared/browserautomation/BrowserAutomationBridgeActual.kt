package com.vowser.client.browserautomation

import com.vowser.client.websocket.dto.NavigationPath
import com.vowser.client.contribution.ContributionStep
import io.github.aakira.napier.Napier
import com.vowser.client.logging.Tags

actual object BrowserAutomationBridge {
    actual suspend fun executeNavigationPath(path: NavigationPath) {
        // TODO: Android 브라우저 자동화 구현
        Napier.i("Android: BrowserAutomationBridge.executeNavigationPath(${path.pathId}) - Not implemented yet", tag = Tags.BROWSER_AUTOMATION)
    }

    actual suspend fun startContributionRecording() {
    }

    actual fun stopContributionRecording() {
    }

    actual suspend fun navigate(url: String) {
    }

    actual fun setContributionRecordingCallback(callback: (ContributionStep) -> Unit) {
    }
}