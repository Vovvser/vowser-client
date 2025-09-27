package com.vowserclient.shared.browserautomation

import com.vowser.client.websocket.dto.NavigationPath
import com.vowser.client.contribution.ContributionStep
import com.vowser.client.contribution.ContributionConstants
import com.vowser.client.logging.VowserLogger
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

    actual suspend fun executeNavigationPath(path: NavigationPath) {
        VowserLogger.info("Executing navigation path: ${path.pathId}", Tags.BROWSER_AUTOMATION)

        BrowserAutomationService.initialize()
        // 첫 번째 스텝이 navigate인 경우, 항상 해당 URL로 이동 (루트 페이지로 초기화)
        val firstStep = path.steps.firstOrNull()
        if (firstStep?.action == "navigate") {
            VowserLogger.info("Initializing root page: ${firstStep.url}", Tags.BROWSER_AUTOMATION)
            browserActions.navigate(firstStep.url)
            delay(ContributionConstants.PAGE_LOAD_WAIT_MS)
            
            var currentUrl = firstStep.url
            var hasNavigatedToFirstWebsite = false
            
            for ((index, step) in path.steps.drop(1).withIndex()) {
                val actualIndex = index + 1
                VowserLogger.info("Executing step ${actualIndex + 1}/${path.steps.size}: ${step.action} on ${step.url} with selector ${step.selector}", Tags.BROWSER_AUTOMATION)
                
                val actionResult = actionExecutors[step.action]?.execute(browserActions, step) ?: false
                
                // 클릭 실패 시 첫 번째 웹사이트 전환만 처리
                if (!actionResult && step.action == "click" && !hasNavigatedToFirstWebsite) {
                    // 다음 스텝이 다른 도메인인지 확인
                    val nextStep = if (actualIndex < path.steps.size - 1) path.steps[actualIndex + 1] else null
                    if (nextStep != null && isDifferentDomain(currentUrl, nextStep.url)) {
                        VowserLogger.info("Click failed and domain change detected, navigating to first website: ${nextStep.url}", Tags.BROWSER_AUTOMATION)
                        browserActions.navigate(nextStep.url)
                        currentUrl = nextStep.url
                        hasNavigatedToFirstWebsite = true
                        delay(ContributionConstants.PAGE_LOAD_WAIT_MS)
                    }
                }
                
                when (step.action) {
                    "navigate" -> {
                        VowserLogger.info("Waiting for page load after navigation...", Tags.BROWSER_AUTOMATION)
                        currentUrl = step.url
                        delay(ContributionConstants.PAGE_LOAD_WAIT_MS)
                    }
                    "click" -> {
                        VowserLogger.info("Waiting after click action...", Tags.BROWSER_AUTOMATION)
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
                VowserLogger.info("Executing step ${index + 1}/${path.steps.size}: ${step.action} on ${step.url} with selector ${step.selector}", Tags.BROWSER_AUTOMATION)
                
                actionExecutors[step.action]?.execute(browserActions, step)
                    ?: VowserLogger.warn("Unknown navigation action or no executor found: ${step.action}", Tags.BROWSER_AUTOMATION)
                
                when (step.action) {
                    "navigate" -> {
                        VowserLogger.info("Waiting for page load after navigation...", Tags.BROWSER_AUTOMATION)
                        delay(ContributionConstants.PAGE_LOAD_WAIT_MS)
                    }
                    "click" -> {
                        VowserLogger.info("Waiting after click action...", Tags.BROWSER_AUTOMATION)
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
        VowserLogger.info("Navigation path ${path.pathId} execution completed.", Tags.BROWSER_AUTOMATION)
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
        VowserLogger.info("Starting contribution recording", Tags.BROWSER_AUTOMATION)
        BrowserAutomationService.startContributionRecording()
    }
    
    actual fun stopContributionRecording() {
        VowserLogger.info("Stopping contribution recording", Tags.BROWSER_AUTOMATION)
        BrowserAutomationService.stopContributionRecording()
    }
    
    actual suspend fun navigate(url: String) {
        VowserLogger.info("Navigating to: $url", Tags.BROWSER_AUTOMATION)
        BrowserAutomationService.navigate(url)
    }
    
    actual fun setContributionRecordingCallback(callback: (ContributionStep) -> Unit) {
        VowserLogger.info("Setting contribution recording callback", Tags.BROWSER_AUTOMATION)
        BrowserAutomationService.setContributionRecordingCallback(callback)
    }
}