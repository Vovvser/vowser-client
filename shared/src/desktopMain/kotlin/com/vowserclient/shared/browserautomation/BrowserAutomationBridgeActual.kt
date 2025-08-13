package com.vowserclient.shared.browserautomation

import com.vowser.client.websocket.dto.NavigationPath
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

    actual suspend fun goBackInBrowser() {
        Napier.i { "Desktop: BrowserAutomationBridge.goBackInBrowser() 호출됨" }
        browserActions.goBack()
    }

    actual suspend fun goForwardInBrowser() {
        Napier.i { "Desktop: BrowserAutomationBridge.goForwardInBrowser() 호출됨" }
        browserActions.goForward()
    }

    actual suspend fun navigateInBrowser(url: String) {
        Napier.i { "Desktop: BrowserAutomationBridge.navigateInBrowser($url) 호출됨" }
        browserActions.navigate(url)
    }

    actual suspend fun executeNavigationPath(path: NavigationPath) {
        Napier.i { "Desktop: BrowserAutomationBridge.executeNavigationPath(${path.pathId}) 호출됨" }
        
        // 첫 번째 스텝이 navigate인 경우, 항상 해당 URL로 이동 (루트 페이지로 초기화)
        val firstStep = path.steps.firstOrNull()
        if (firstStep?.action == "navigate") {
            Napier.i { "루트 페이지로 초기화: ${firstStep.url}" }
            browserActions.navigate(firstStep.url)
            delay(3000) // 페이지 로드 대기
            
            // 첫 번째 navigate 스텝은 건너뛰고 나머지 스텝들 실행
            for ((index, step) in path.steps.drop(1).withIndex()) {
                val actualIndex = index + 1 // 실제 인덱스 (첫 번째 스텝 건너뛰었으므로 +1)
                Napier.i { "Executing step ${actualIndex + 1}/${path.steps.size}: ${step.action} on ${step.url} with selector ${step.selector}" }
                
                actionExecutors[step.action]?.execute(browserActions, step)
                    ?: Napier.w { "Unknown navigation action or no executor found: ${step.action}" }
                
                when (step.action) {
                    "navigate" -> {
                        Napier.i { "Waiting for page load after navigation..." }
                        delay(3000) // 페이지 로드 대기
                    }
                    "click" -> {
                        Napier.i { "Waiting after click action..." }
                        delay(2000) // 클릭 후 페이지 변화 대기
                    }
                    "type" -> {
                        delay(1000) // 타이핑 후 대기
                    }
                    else -> {
                        delay(500) // 기본 대기 시간
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
                        delay(3000) // 페이지 로드 대기
                    }
                    "click" -> {
                        Napier.i { "Waiting after click action..." }
                        delay(2000) // 클릭 후 페이지 변화 대기
                    }
                    "type" -> {
                        delay(1000) // 타이핑 후 대기
                    }
                    else -> {
                        delay(500) // 기본 대기 시간
                    }
                }
            }
        }
        Napier.i { "Navigation path ${path.pathId} execution completed." }
    }
}