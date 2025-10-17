package com.vowser.client.browserautomation

import com.vowser.client.websocket.dto.NavigationPath
import com.vowser.client.contribution.ContributionStep

expect object BrowserAutomationBridge {
    suspend fun executeNavigationPath(path: NavigationPath, timeout: Double? = null): Boolean
    suspend fun startContributionRecording()
    fun stopContributionRecording()
    suspend fun navigate(url: String)
    suspend fun waitForNetworkIdle()
    fun setContributionRecordingCallback(callback: (ContributionStep) -> Unit)
}