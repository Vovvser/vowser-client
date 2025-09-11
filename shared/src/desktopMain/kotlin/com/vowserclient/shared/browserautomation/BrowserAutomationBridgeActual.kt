package com.vowserclient.shared.browserautomation

import com.vowser.client.websocket.dto.NavigationPath
import com.vowser.client.contribution.ContributionStep
import com.vowser.client.contribution.ContributionConstants
import io.github.aakira.napier.Napier
import kotlinx.coroutines.delay

actual object BrowserAutomationBridge {

    private val browserActions = BrowserActions(BrowserAutomationService)

    private val actionExecutors = mapOf(
        "navigate" to NavigateActionExecutor(),
        "click" to ClickActionExecutor(),
        "type" to TypeActionExecutor(),
        "submit" to SubmitActionExecutor()
    )

    actual suspend fun executeNavigationPath(path: NavigationPath) {
        Napier.i { "Desktop: BrowserAutomationBridge.executeNavigationPath(${path.pathId}) 호출됨" }

        BrowserAutomationService.initialize()
        // 첫 번째 스텝이 navigate인 경우, 항상 해당 URL로 이동 (루트 페이지로 초기화)
        val firstStep = path.steps.firstOrNull()
        if (firstStep?.action == "navigate") {
            Napier.i { "루트 페이지로 초기화: ${firstStep.url}" }
            browserActions.navigate(firstStep.url)
            delay(ContributionConstants.PAGE_LOAD_WAIT_MS)
            
            var currentUrl = firstStep.url
            var hasNavigatedToFirstWebsite = false
            
            for ((index, step) in path.steps.drop(1).withIndex()) {
                val actualIndex = index + 1
                Napier.i { "Executing step ${actualIndex + 1}/${path.steps.size}: ${step.action} on ${step.url} with selector ${step.selector}" }
                
                val actionResult = actionExecutors[step.action]?.execute(browserActions, step) ?: false
                
                // 클릭 실패 시 첫 번째 웹사이트 전환만 처리
                if (!actionResult && step.action == "click" && !hasNavigatedToFirstWebsite) {
                    // 다음 스텝이 다른 도메인인지 확인
                    val nextStep = if (actualIndex < path.steps.size - 1) path.steps[actualIndex + 1] else null
                    if (nextStep != null && isDifferentDomain(currentUrl, nextStep.url)) {
                        Napier.i { "Click failed and domain change detected, navigating to first website: ${nextStep.url}" }
                        browserActions.navigate(nextStep.url)
                        currentUrl = nextStep.url
                        hasNavigatedToFirstWebsite = true
                        delay(ContributionConstants.PAGE_LOAD_WAIT_MS)
                    }
                }
                
                when (step.action) {
                    "navigate" -> {
                        Napier.i { "Waiting for page load after navigation..." }
                        currentUrl = step.url
                        delay(ContributionConstants.PAGE_LOAD_WAIT_MS)
                    }
                    "click" -> {
                        Napier.i { "Waiting after click action..." }
                        delay(ContributionConstants.CLICK_WAIT_MS)
                    }
                    "type" -> {
                        delay(ContributionConstants.TYPE_WAIT_MS)
                    }
                    else -> {
                        delay(ContributionConstants.POLLING_INTERVAL_MS)
                    }
                }
            }
        } else {
            // 첫 번째 스텝이 navigate가 아닌 경우 모든 스텝 실행
            for ((index, step) in path.steps.withIndex()) {
                Napier.i { "Executing step ${index + 1}/${path.steps.size}: ${step.action} on ${step.url} with selector ${step.selector}" }
                
                actionExecutors[step.action]?.execute(browserActions, step)
                    ?: Napier.w { "Unknown navigation action or no executor found: ${step.action}" }
                
                when (step.action) {
                    "navigate" -> {
                        Napier.i { "Waiting for page load after navigation..." }
                        delay(ContributionConstants.PAGE_LOAD_WAIT_MS)
                    }
                    "click" -> {
                        Napier.i { "Waiting after click action..." }
                        delay(ContributionConstants.CLICK_WAIT_MS)
                    }
                    "type" -> {
                        delay(ContributionConstants.TYPE_WAIT_MS)
                    }
                    else -> {
                        delay(ContributionConstants.POLLING_INTERVAL_MS)
                    }
                }
            }
        }
        Napier.i { "Navigation path ${path.pathId} execution completed." }
    }

    /**
     * 두 URL의 도메인이 다른지 확인
     */
    private fun isDifferentDomain(currentUrl: String, nextUrl: String): Boolean {
        return try {
            val currentDomain = extractDomain(currentUrl)
            val nextDomain = extractDomain(nextUrl)
            currentDomain != nextDomain
        } catch (e: Exception) {
            false
        }
    }

    /**
     * URL에서 도메인 추출
     */
    private fun extractDomain(url: String): String {
        return try {
            val domain = url.substringAfter("://").substringBefore("/")
            domain.lowercase()
        } catch (e: Exception) {
            url
        }
    }
    
    // 기여 모드 관련 메서드들
    actual suspend fun startContributionRecording() {
        Napier.i { "Desktop: BrowserAutomationBridge.startContributionRecording() 호출됨" }
        BrowserAutomationService.startContributionRecording()
    }
    
    actual fun stopContributionRecording() {
        Napier.i { "Desktop: BrowserAutomationBridge.stopContributionRecording() 호출됨" }
        BrowserAutomationService.stopContributionRecording()
    }
    
    actual suspend fun navigate(url: String) {
        Napier.i { "Desktop: BrowserAutomationBridge.navigate($url) 호출됨" }
        BrowserAutomationService.navigate(url)
    }
    
    actual fun setContributionRecordingCallback(callback: (ContributionStep) -> Unit) {
        Napier.i { "Desktop: BrowserAutomationBridge.setContributionRecordingCallback() 호출됨" }
        BrowserAutomationService.setContributionRecordingCallback(callback)
    }
}