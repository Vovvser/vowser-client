package com.vowserclient.shared.browserautomation

import com.vowser.client.websocket.dto.NavigationPath
import com.vowser.client.contribution.ContributionStep

expect object BrowserAutomationBridge {
    suspend fun executeNavigationPath(path: NavigationPath)
    suspend fun startContributionRecording()
    fun stopContributionRecording()
    suspend fun navigate(url: String)
    fun setContributionRecordingCallback(callback: (ContributionStep) -> Unit)
}