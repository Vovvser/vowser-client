package com.vowser.client.api

import com.vowser.client.api.dto.MatchedPathDetail
import com.vowser.client.api.dto.PathStepDetail
import com.vowser.client.browserautomation.BrowserAutomationBridge
import com.vowser.client.model.MemberResponse
import com.vowser.client.websocket.dto.NavigationPath
import com.vowser.client.websocket.dto.NavigationStep
import io.github.aakira.napier.Napier
import com.vowser.client.logging.Tags
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock

/**
 * 경로 실행 결과
 */
data class PathExecutionResult(
    val success: Boolean,
    val stepsCompleted: Int,
    val totalSteps: Int,
    val failedAt: Int? = null,
    val error: String? = null
)

/**
 * 경로 실행기 - 검색된 경로를 브라우저에서 실행
 */
class PathExecutor {
    private var currentStepIndex = 0
    private var currentPath: MatchedPathDetail? = null
    private var isExecuting = false
    private var currentUserInfo: MemberResponse? = null
    private var currentOnLog: ((String) -> Unit)? = null

    /**
     * 경로 실행 (사용자 정보를 통한 자동 입력 지원)
     * @param path 실행할 경로
     * @param userInfo 자동 입력에 사용할 사용자 정보 (옵션)
     * @param onStepComplete 각 단계 완료 시 호출되는 콜백 (stepIndex, totalSteps, description)
     * @param getUserInput Input 액션 시 사용자 입력을 받는 함수 (자동 입력 실패 시 fallback)
     * @param onLog UI 로그 출력 콜백 (message: String)
     */
    suspend fun executePath(
        path: MatchedPathDetail,
        userInfo: MemberResponse? = null,
        onStepComplete: ((Int, Int, String) -> Unit)? = null,
        getUserInput: (suspend (PathStepDetail) -> String)? = null,
        onLog: ((String) -> Unit)? = null
    ): PathExecutionResult {
        if (isExecuting) {
            return PathExecutionResult(
                success = false,
                stepsCompleted = 0,
                totalSteps = 0,
                error = "Another path is currently executing"
            )
        }

        isExecuting = true
        currentPath = path
        currentStepIndex = 0
        currentUserInfo = userInfo
        currentOnLog = onLog

        if (userInfo != null) {
            Napier.i("🚀 Executing path with auto-fill: ${path.task_intent} (${path.steps.size} steps)", tag = Tags.BROWSER_AUTOMATION)
        } else {
            Napier.i("🚀 Executing path: ${path.task_intent} (${path.steps.size} steps)", tag = Tags.BROWSER_AUTOMATION)
        }

        try {
            // 단계별 실행
            for (i in path.steps.indices) {
                currentStepIndex = i
                val step = path.steps[i]

                try {
                    executeStep(step, getUserInput)
                    onStepComplete?.invoke(i + 1, path.steps.size, step.description)
                    Napier.i("✅ Step ${i + 1}/${path.steps.size} completed: ${step.description}", tag = Tags.BROWSER_AUTOMATION)

                    // 단계 간 대기
                    delay(500)
                } catch (e: Exception) {
                    Napier.e("❌ Step ${i + 1}/${path.steps.size} failed: ${e.message}", e, tag = Tags.BROWSER_AUTOMATION)
                    return PathExecutionResult(
                        success = false,
                        stepsCompleted = i,
                        totalSteps = path.steps.size,
                        failedAt = i,
                        error = e.message
                    )
                }
            }

            Napier.i("✅ Path execution completed successfully: ${path.task_intent}", tag = Tags.BROWSER_AUTOMATION)
            return PathExecutionResult(
                success = true,
                stepsCompleted = path.steps.size,
                totalSteps = path.steps.size
            )
        } finally {
            isExecuting = false
            currentPath = null
            currentUserInfo = null
            currentOnLog = null
        }
    }

    /**
     * 개별 단계 실행
     */
    private suspend fun executeStep(
        step: PathStepDetail,
        getUserInput: (suspend (PathStepDetail) -> String)?
    ) {
        Napier.d("▶ Executing step: ${step.description} (${step.action})", tag = Tags.BROWSER_AUTOMATION)

        val needsNavigate = shouldNavigateBeforeAction(step)
        if (needsNavigate) {
            Napier.d("Step requires navigation to different page: ${step.url}", tag = Tags.BROWSER_AUTOMATION)
            executeNavigateStep(step)
            delay(1000)
        }

        when (step.action) {
            "click" -> executeClickStep(step)
            "input", "type" -> executeInputStep(step, getUserInput)
            "wait" -> executeWaitStep(step)
            "navigate" -> {
                if (!needsNavigate) {
                    executeNavigateStep(step)
                }
            }
            else -> {
                Napier.w("Unknown action type: ${step.action}", tag = Tags.BROWSER_AUTOMATION)
            }
        }
    }

    /**
     * 액션 수행 전에 navigate가 필요한지 판단
     * - 첫 스텝이면서 루트 URL인 경우
     * - 또는 이전 스텝과 base URL(도메인+경로)이 다른 경우
     */
    private fun shouldNavigateBeforeAction(step: PathStepDetail): Boolean {
        // 첫 스텝이고 루트 URL이면 항상 navigate
        if (currentStepIndex == 0 && isRootUrl(step.url)) {
            return true
        }

        // 이전 스텝과 base URL이 다르면 navigate 필요
        val previousStep = currentPath?.steps?.getOrNull(currentStepIndex - 1)
        if (previousStep != null) {
            val previousBaseUrl = extractBaseUrl(previousStep.url)
            val currentBaseUrl = extractBaseUrl(step.url)
            if (previousBaseUrl != currentBaseUrl) {
                Napier.d("Base URL changed: $previousBaseUrl -> $currentBaseUrl", tag = Tags.BROWSER_AUTOMATION)
                return true
            }
        }

        return false
    }

    /**
     * Base URL 추출 (도메인 + 경로, 쿼리 파라미터 제외)
     * 예: https://dict.naver.com/search.dict?query=... -> https://dict.naver.com/search.dict
     */
    private fun extractBaseUrl(url: String): String {
        return try {
            url.substringBefore("?").substringBefore("#")
        } catch (e: Exception) {
            url
        }
    }

    /**
     * URL이 루트 URL인지 확인 (도메인만 있거나 / 뒤에 아무것도 없는 경우)
     */
    private fun isRootUrl(url: String): Boolean {
        return try {
            val withoutProtocol = url.substringAfter("://")
            val path = withoutProtocol.substringAfter("/", "")
            path.isEmpty() || path == "/"
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Navigate 액션 실행
     */
    private suspend fun executeNavigateStep(step: PathStepDetail) {
        try {
            val navigationStep = NavigationStep(
                url = step.url,
                title = step.description,
                action = "navigate",
                selector = ""
            )

            val navigationPath = NavigationPath(
                pathId = "navigate_${Clock.System.now().toEpochMilliseconds()}",
                steps = listOf(navigationStep),
                description = step.description
            )

            BrowserAutomationBridge.executeNavigationPath(navigationPath)
            Napier.d("Navigate succeeded to: ${step.url}", tag = Tags.BROWSER_AUTOMATION)
        } catch (e: Exception) {
            Napier.e("Navigate failed to ${step.url}: ${e.message}", e, tag = Tags.BROWSER_AUTOMATION)
            throw e
        }
    }

    /**
     * Click 액션 실행
     */
    private suspend fun executeClickStep(step: PathStepDetail) {
        // 다중 셀렉터 fallback - 각 셀렉터를 NavigationPath로 변환하여 실행
        for ((index, selector) in step.selectors.withIndex()) {
            try {
                // jQuery 스타일 셀렉터를 Playwright 호환 형식으로 변환
                val convertedSelector = convertSelector(selector)

                val navigationStep = NavigationStep(
                    url = step.url,
                    title = step.description,
                    action = "click",
                    selector = convertedSelector
                )

                val navigationPath = NavigationPath(
                    pathId = "single_step_${Clock.System.now().toEpochMilliseconds()}",
                    steps = listOf(navigationStep),
                    description = step.description
                )

                BrowserAutomationBridge.executeNavigationPath(navigationPath)
                Napier.d("Click succeeded with selector[$index]: $convertedSelector", tag = Tags.BROWSER_AUTOMATION)
                return
            } catch (e: Exception) {
                Napier.d("Click failed with selector[$index]: $selector - ${e.message}", tag = Tags.BROWSER_AUTOMATION)
                // 다음 셀렉터 시도
            }
        }

        // 모든 셀렉터 실패 → URL 기반 fallback 시도
        Napier.w("All selectors failed, trying URL-based fallback", tag = Tags.BROWSER_AUTOMATION)

        if (tryClickByTargetUrl(step)) {
            Napier.i("✅ Click succeeded using URL-based fallback", tag = Tags.BROWSER_AUTOMATION)
            return
        }

        // URL fallback도 실패 → 최후의 수단: 직접 navigate
        Napier.w("URL-based click failed, navigating directly to target", tag = Tags.BROWSER_AUTOMATION)

        if (tryDirectNavigateFromSelectors(step)) {
            Napier.i("✅ Direct navigation succeeded", tag = Tags.BROWSER_AUTOMATION)
            return
        }

        // 모든 방법 실패
        throw Exception("Failed to click element: no selector matched (tried ${step.selectors.size} selectors + URL fallback)")
    }

    /**
     * URL 기반 클릭 시도 - href 속성에서 목표 URL 찾기
     */
    private suspend fun tryClickByTargetUrl(step: PathStepDetail): Boolean {
        // 셀렉터에서 목표 URL 추출 시도
        val targetUrls = mutableSetOf<String>()

        // 1. 셀렉터에서 href 패턴 추출
        step.selectors.forEach { selector ->
            // a[href='https://news.naver.com'] → https://news.naver.com
            val hrefMatch = Regex("""href=['"]([^'"]+)['"]""").find(selector)
            if (hrefMatch != null) {
                targetUrls.add(hrefMatch.groupValues[1])
            }

            // a[href*='news.naver.com'] → news.naver.com (부분 매칭)
            val partialMatch = Regex("""href\*=['"]([^'"]+)['"]""").find(selector)
            if (partialMatch != null) {
                targetUrls.add(partialMatch.groupValues[1])
            }
        }

        // 2. 추출된 URL로 클릭 시도
        for (targetUrl in targetUrls) {
            try {
                val selector = if (targetUrl.startsWith("http")) {
                    // 절대 URL
                    "a[href='$targetUrl']"
                } else {
                    // 부분 매칭
                    "a[href*='$targetUrl']"
                }

                val navigationStep = NavigationStep(
                    url = step.url,
                    title = step.description,
                    action = "click",
                    selector = selector
                )

                val navigationPath = NavigationPath(
                    pathId = "url_fallback_${Clock.System.now().toEpochMilliseconds()}",
                    steps = listOf(navigationStep),
                    description = "${step.description} (URL fallback)"
                )

                BrowserAutomationBridge.executeNavigationPath(navigationPath)
                Napier.d("URL-based click succeeded: $selector", tag = Tags.BROWSER_AUTOMATION)
                return true
            } catch (e: Exception) {
                Napier.d("URL-based click failed for $targetUrl: ${e.message}", tag = Tags.BROWSER_AUTOMATION)
            }
        }

        return false
    }

    /**
     * 최후의 수단: 셀렉터에서 추출한 URL로 직접 navigate
     */
    private suspend fun tryDirectNavigateFromSelectors(step: PathStepDetail): Boolean {
        // 셀렉터에서 완전한 URL 추출
        val targetUrls = mutableSetOf<String>()

        step.selectors.forEach { selector ->
            val hrefMatch = Regex("""href=['"]([^'"]+)['"]""").find(selector)
            if (hrefMatch != null) {
                val url = hrefMatch.groupValues[1]
                if (url.startsWith("http")) {
                    targetUrls.add(url)
                }
            }
        }

        // 추출된 URL이 있으면 직접 이동
        val targetUrl = targetUrls.firstOrNull()
        if (targetUrl != null) {
            try {
                Napier.i("Navigating directly to extracted URL: $targetUrl", tag = Tags.BROWSER_AUTOMATION)

                val navigationStep = NavigationStep(
                    url = targetUrl,
                    title = step.description,
                    action = "navigate",
                    selector = ""
                )

                val navigationPath = NavigationPath(
                    pathId = "direct_nav_${Clock.System.now().toEpochMilliseconds()}",
                    steps = listOf(navigationStep),
                    description = "${step.description} (direct navigation)"
                )

                BrowserAutomationBridge.executeNavigationPath(navigationPath)
                return true
            } catch (e: Exception) {
                Napier.e("Direct navigation failed: ${e.message}", e, tag = Tags.BROWSER_AUTOMATION)
            }
        }

        return false
    }

    /**
     * jQuery/CSS 선택자를 Playwright 호환 형식으로 변환
     * - a:contains('텍스트') → a:has-text('텍스트')
     * - button:contains('텍스트') → button:has-text('텍스트')
     */
    private fun convertSelector(selector: String): String {
        // :contains('텍스트') → :has-text('텍스트')
        val containsRegex = Regex("""(:contains\(['"])(.*?)(['"]\))""")
        val converted = containsRegex.replace(selector) { matchResult ->
            val text = matchResult.groupValues[2]
            ":has-text('$text')"
        }

        if (converted != selector) {
            Napier.d("Converted selector: $selector → $converted", tag = Tags.BROWSER_AUTOMATION)
        }

        return converted
    }

    /**
     * Input 액션 실행
     */
    private suspend fun executeInputStep(
        step: PathStepDetail,
        getUserInput: (suspend (PathStepDetail) -> String)?
    ) {
        Napier.d("📝 Input step detected: ${step.description}", tag = Tags.BROWSER_AUTOMATION)

        var inputValue: String? = null

        if (currentUserInfo != null && step.is_input) {
            inputValue = UserInputMatcher.getAutoFillValue(step, currentUserInfo!!)
            if (inputValue != null) {
                Napier.i("✅ Auto-filled: ${step.description} → $inputValue", tag = Tags.BROWSER_AUTOMATION)
                currentOnLog?.invoke("✅ 자동 입력: ${step.description} → $inputValue")
            } else {
                Napier.w("⚠️ Auto-fill failed for: ${step.description}", tag = Tags.BROWSER_AUTOMATION)
            }
        }

        // 2. 자동 입력 실패 시, getUserInput 콜백 사용
        if (inputValue == null) {
            if (getUserInput == null) {
                Napier.w("❌ Skipping input step: ${step.description}", tag = Tags.BROWSER_AUTOMATION)
                return
            }
            Napier.d("Waiting for user input", tag = Tags.BROWSER_AUTOMATION)
            inputValue = getUserInput.invoke(step)
        }

        if (inputValue.isEmpty()) {
            Napier.w("Empty input value provided for step: ${step.description}", tag = Tags.BROWSER_AUTOMATION)
            return
        }

        // 다중 셀렉터 fallback
        for ((index, selector) in step.selectors.withIndex()) {
            try {
                val navigationStep = NavigationStep(
                    url = step.url,
                    title = step.description,
                    action = "type",
                    selector = selector,
                    htmlAttributes = mapOf("value" to inputValue)
                )

                val navigationPath = NavigationPath(
                    pathId = "single_step_${Clock.System.now().toEpochMilliseconds()}",
                    steps = listOf(navigationStep),
                    description = step.description
                )

                BrowserAutomationBridge.executeNavigationPath(navigationPath)
                Napier.d("Input succeeded with selector[$index]: $selector", tag = Tags.BROWSER_AUTOMATION)
                return
            } catch (e: Exception) {
                Napier.d("Input failed with selector[$index]: $selector - ${e.message}", tag = Tags.BROWSER_AUTOMATION)
            }
        }

        // 모든 셀렉터 실패
        throw Exception("Failed to input text: no selector matched (tried ${step.selectors.size} selectors)")
    }

    /**
     * Wait 액션 실행 (사용자 대기)
     */
    private suspend fun executeWaitStep(step: PathStepDetail) {
        val message = step.wait_message ?: "작업을 완료한 후 계속하세요"
        Napier.i("⏸️ Waiting for user action: $message", tag = Tags.BROWSER_AUTOMATION)

        // TODO: UI에 대기 메시지 표시 및 사용자 확인 대기
        // 현재는 5초 자동 대기
        delay(5000)
    }
}