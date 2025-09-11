package com.vowserclient.shared.browserautomation

import com.vowser.client.contribution.ContributionStep
import com.vowser.client.websocket.dto.NavigationPath
import io.github.aakira.napier.Napier

actual object BrowserAutomationBridge {
    actual suspend fun executeNavigationPath(path: NavigationPath) {
        Napier.i { "iOS: BrowserAutomationBridge.executeNavigationPath(${path.pathId}) - Not implemented yet" }
        // TODO: iOS 브라우저 자동화 구현
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