package com.vowser.client.api

import com.vowser.client.api.dto.MatchedPathDetail
import com.vowser.client.api.dto.PathStepDetail
import com.vowser.client.browserautomation.BrowserAutomationBridge
import com.vowser.client.browserautomation.SelectOption
import com.vowser.client.model.MemberResponse
import com.vowser.client.websocket.dto.NavigationPath
import io.github.aakira.napier.Napier
import com.vowser.client.logging.Tags
import com.vowser.client.websocket.dto.NavigationStep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlin.coroutines.cancellation.CancellationException

/**
 * 경로 실행 결과
 */
data class PathExecutionResult(
    val success: Boolean,
    val stepsCompleted: Int,
    val totalSteps: Int,
    val failedAt: Int? = null,
    val error: String? = null,
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
    private var currentOnWaitForUser: (suspend (String) -> Unit)? = null
    private var currentGetUserSelect: (suspend (PathStepDetail, List<SelectOption>) -> String)? = null
    private var executionJob: Job? = null

    /**
     * 현재 실행 중인 경로를 취소
     */
    suspend fun cancelExecution() {
        if (isExecuting && executionJob?.isActive == true) {
            Napier.i("🛑 Cancelling current path execution...", tag = Tags.BROWSER_AUTOMATION)
            currentOnLog?.invoke("🛑 이전 명령을 중단하고 새 명령을 실행합니다")
            executionJob?.cancel()
            executionJob = null
            isExecuting = false
            currentPath = null
            currentUserInfo = null
            currentOnLog = null
            currentOnWaitForUser = null
            delay(300)
            Napier.i("Path execution cancelled successfully", tag = Tags.BROWSER_AUTOMATION)
        }
    }

    /**
     * 경로 실행 (사용자 정보를 통한 자동 입력 지원)
     * @param path 실행할 경로
     * @param userInfo 자동 입력에 사용할 사용자 정보 (옵션)
     * @param onStepComplete 각 단계 완료 시 호출되는 콜백 (stepIndex, totalSteps, description)
     * @param getUserInput Input 액션 시 사용자 입력을 받는 함수 (자동 입력 실패 시 fallback)
     * @param onLog UI 로그 출력 콜백 (message: String)
     * @param onWaitForUser Wait 액션 시 사용자 확인을 기다리는 함수 (waitMessage: String)
     */
    suspend fun executePath(
        path: MatchedPathDetail,
        userInfo: MemberResponse? = null,
        onStepComplete: ((Int, Int, String) -> Unit)? = null,
        getUserInput: (suspend (PathStepDetail) -> String)? = null,
        getUserSelect: (suspend (PathStepDetail, List<SelectOption>) -> String)? = null,
        onLog: ((String) -> Unit)? = null,
        onWaitForUser: (suspend (String) -> Unit)? = null
    ): PathExecutionResult {

        if (isExecuting) {
            Napier.w("⚠️ Another path is executing. Cancelling it...", tag = Tags.BROWSER_AUTOMATION)
            cancelExecution()
        }

        isExecuting = true
        currentPath = path
        currentStepIndex = 0
        currentUserInfo = userInfo
        currentOnLog = onLog
        currentOnWaitForUser = onWaitForUser
        currentGetUserSelect = getUserSelect

        if (userInfo != null) {
            Napier.i(
                "🚀 Executing path with auto-fill: ${path.taskIntent} (${path.steps.size} steps)",
                tag = Tags.BROWSER_AUTOMATION
            )
        } else {
            Napier.i("🚀 Executing path: ${path.taskIntent} (${path.steps.size} steps)", tag = Tags.BROWSER_AUTOMATION)
        }

        return withContext(Dispatchers.Default) {
            executionJob = coroutineContext[Job]

            try {
                // 단계별 실행
                for (i in path.steps.indices) {
                    ensureActive()

                    currentStepIndex = i
                    val step = path.steps[i]

                    try {
                        onStepComplete?.invoke(i + 1, path.steps.size, step.description)
                        Napier.i(
                            "▶️  Step ${i + 1}/${path.steps.size} starting: ${step.description}",
                            tag = Tags.BROWSER_AUTOMATION
                        )

                        executeStep(step, getUserInput)
                        Napier.i(
                            "✅ Step ${i + 1}/${path.steps.size} completed: ${step.description}",
                            tag = Tags.BROWSER_AUTOMATION
                        )

                        // 카카오톡 인증 완료 스텝 후 추가 딜레이
                        if (isKakaoAuthCompleteStep(step)) {
                            Napier.i("⏱카카오톡 인증 완료 후 5초 추가 대기...", tag = Tags.BROWSER_AUTOMATION)
                            currentOnLog?.invoke("⏱카카오톡 인증 완료 - 5초 대기 중...")
//                            delay(5000)
                        }

                        delay(500)
                    } catch (e: CancellationException) {
                        Napier.w("🛑 Path execution cancelled at step ${i + 1}", tag = Tags.BROWSER_AUTOMATION)
                        throw e
                    } catch (e: Exception) {
                        Napier.e(
                            "❌ Step ${i + 1}/${path.steps.size} failed: ${e.message}",
                            e,
                            tag = Tags.BROWSER_AUTOMATION
                        )
                        return@withContext PathExecutionResult(
                            success = false,
                            stepsCompleted = i,
                            totalSteps = path.steps.size,
                            failedAt = i,
                            error = e.message
                        )
                    }
                }

                Napier.i("✅ Path execution completed successfully: ${path.taskIntent}", tag = Tags.BROWSER_AUTOMATION)
                PathExecutionResult(
                    success = true,
                    stepsCompleted = path.steps.size,
                    totalSteps = path.steps.size
                )
            } catch (e: CancellationException) {
                Napier.i("🛑 Path execution was cancelled", tag = Tags.BROWSER_AUTOMATION)
                PathExecutionResult(
                    success = false,
                    stepsCompleted = currentStepIndex,
                    totalSteps = path.steps.size,
                    error = "Execution cancelled"
                )
            } finally {
                isExecuting = false
                currentPath = null
                currentUserInfo = null
                currentOnLog = null
                currentOnWaitForUser = null
                currentGetUserSelect = null
                executionJob = null
            }
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

        val previousStep = currentPath?.steps?.getOrNull(currentStepIndex - 1)
        val isPrevStepNavClick = if (previousStep != null) willNavigateOnClick(previousStep) else false

        // 1. Navigate if necessary
        if (shouldNavigateBeforeAction(step, isPrevStepNavClick)) {
            Napier.d("Step requires navigation to: ${step.url}", tag = Tags.BROWSER_AUTOMATION)
            executeNavigateStep(step)
            BrowserAutomationBridge.waitForNetworkIdle()
        }

        // 2. Execute action
        when (step.action) {
            "click" -> {
                executeClickStep(step)
                Napier.d(
                    "Click succeeded, unconditionally waiting for network to be idle...",
                    tag = Tags.BROWSER_AUTOMATION
                )
                BrowserAutomationBridge.waitForNetworkIdle()
            }

            "input", "type" -> executeInputStep(step, getUserInput)
            "wait" -> executeWaitStep(step)
            "navigate" -> {
                Napier.d(
                    "Explicit navigate action, handled by pre-step navigation check.",
                    tag = Tags.BROWSER_AUTOMATION
                )
                executeNavigateStep(step) // ???
            }
            "select" -> {
                executeSelectStep(step)
                Napier.d("Select action detected, but not implemented. Skipping.", tag = Tags.BROWSER_AUTOMATION)
            }

            else -> {
                Napier.w("Unknown action type: ${step.action}", tag = Tags.BROWSER_AUTOMATION)
            }
        }
    }

    /**
     * 클릭 후 페이지 전환이 일어날지 판단
     * - 다음 스텝의 URL이 현재와 다르면 페이지 전환 예상
     */
    private fun willNavigateOnClick(step: PathStepDetail): Boolean {
        if (step.action != "click") return false
        val steps = currentPath?.steps ?: return false
        val currentIndex = steps.indexOf(step)
        if (currentIndex == -1 || currentIndex + 1 >= steps.size) {
            return false
        }
        val nextStep = steps[currentIndex + 1]
        val currentBaseUrl = extractBaseUrl(step.url)
        val nextBaseUrl = extractBaseUrl(nextStep.url)
        return currentBaseUrl != nextBaseUrl
    }

    /**
     * 액션 수행 전에 navigate가 필요한지 판단
     */
    private fun shouldNavigateBeforeAction(step: PathStepDetail, isPrevStepNavClick: Boolean): Boolean {
        // 이전 스텝이 페이지 전환을 유발하는 클릭이었다면, navigate하지 않음
        if (isPrevStepNavClick) {
            Napier.d(
                "Skipping navigation because previous step was a navigation-causing click.",
                tag = Tags.BROWSER_AUTOMATION
            )
            return false
        }

        // 첫 스텝이거나, 이전 스텝과 base URL이 다르면 navigate 필요
        val previousStep = currentPath?.steps?.getOrNull(currentStepIndex - 1) ?: // 첫 스텝
        return true

        val previousBaseUrl = extractBaseUrl(previousStep.url)
        val currentBaseUrl = extractBaseUrl(step.url)
        if (previousBaseUrl != currentBaseUrl) {
            Napier.d("Base URL changed: $previousBaseUrl -> $currentBaseUrl", tag = Tags.BROWSER_AUTOMATION)
            return true
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
     * Navigate 액션 실행
     */
    private suspend fun executeNavigateStep(step: PathStepDetail) {
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

        val success = BrowserAutomationBridge.executeNavigationPath(navigationPath)
        if (success) {
            Napier.d("Navigate succeeded to: ${step.url}", tag = Tags.BROWSER_AUTOMATION)
        } else {
            Napier.e("Navigate failed to ${step.url}", tag = Tags.BROWSER_AUTOMATION)
            throw Exception("Navigate action failed for URL: ${step.url}")
        }
    }

    /**
     * Click 액션 실행
     */
    private suspend fun executeClickStep(step: PathStepDetail) {
        val selectors = step.selectors
        val lastIndex = selectors.lastIndex

        // 다중 셀렉터 fallback - 각 셀렉터를 NavigationPath로 변환하여 실행
        for ((index, selector) in selectors.withIndex()) {
            val isLastSelector = index == lastIndex
            // 마지막 셀렉터가 아니면 빠른 타임아웃 적용, 마지막이면 기본 타임아웃(null)
            val timeout = if (!isLastSelector) 2000.0 else null

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

            val success = BrowserAutomationBridge.executeNavigationPath(navigationPath, timeout)
            if (success) {
                Napier.d("Click succeeded with selector[$index]: $convertedSelector", tag = Tags.BROWSER_AUTOMATION)
                return
            } else {
                Napier.d("Click failed with selector[$index]: $selector", tag = Tags.BROWSER_AUTOMATION)
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

        if (currentUserInfo != null && step.isInput == true) {
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
        step.selectors.forEachIndexed { index, selector ->
            try {
                BrowserAutomationBridge.setInputValue(selector, inputValue)
                currentOnLog?.invoke("입력 완료: ${step.description} → $inputValue")
                Napier.d("Input succeeded with selector[$index]: $selector", tag = Tags.BROWSER_AUTOMATION)
                if (step.shouldWait == true) {
                    BrowserAutomationBridge.waitForNetworkIdle()
                }
                return
            } catch (e: Exception) {
                Napier.d("Input failed with selector[$index]: $selector - ${e.message}", tag = Tags.BROWSER_AUTOMATION)
                if (index == step.selectors.lastIndex) {
                    throw e
                }
            }
        }

        throw Exception("Failed to input text: no selector matched (tried ${step.selectors.size} selectors)")
    }

    /**
     * Wait 액션 실행 (사용자 대기)
     */
    private suspend fun executeWaitStep(step: PathStepDetail) {
        val message = step.waitMessage ?: "작업을 완료한 후 계속하세요"
        Napier.i("⏸️  Waiting for user action: $message", tag = Tags.BROWSER_AUTOMATION)

        currentOnLog?.invoke("⏸️ 사용자 작업 대기 중: $message")

        if (currentOnWaitForUser != null) {
            try {
                currentOnWaitForUser?.invoke(message)
                Napier.i("✅ User confirmed completion of: $message", tag = Tags.BROWSER_AUTOMATION)
                currentOnLog?.invoke("✅ 사용자 확인 완료")
            } catch (e: Exception) {
                Napier.e("User wait step failed: ${e.message}", e, tag = Tags.BROWSER_AUTOMATION)
                throw e
            }
        } else {
            Napier.w("No onWaitForUser callback provided, using default 10s wait", tag = Tags.BROWSER_AUTOMATION)
            currentOnLog?.invoke("⚠️ 대기 콜백 없음 - 10초 자동 대기")
            delay(10000)
        }
    }

    private suspend fun executeSelectStep(step: PathStepDetail) {
        Napier.i("🍰 Select step detected: ${step.description}", tag = Tags.BROWSER_AUTOMATION)

        val selectors = step.selectors
        if (selectors.isEmpty()) {
            throw IllegalStateException("Select step has no selectors: ${step.description}")
        }

        var lastError: Exception? = null

        for (selector in selectors) {
            try {
                val options = BrowserAutomationBridge.getSelectOptions(selector)
                if (options.isEmpty()) {
                    Napier.w("No options found for selector: $selector", tag = Tags.BROWSER_AUTOMATION)
                    continue
                }

                val resolvedValue = resolveSelectValue(step, options)
                BrowserAutomationBridge.selectOption(selector, resolvedValue.value)
                if (step.shouldWait == true) {
                    BrowserAutomationBridge.waitForNetworkIdle()
                }
                currentOnLog?.invoke("선택 완료: ${resolvedValue.label}")
                Napier.i("Selected option '${resolvedValue.label}' (value=${resolvedValue.value}) for selector $selector", tag = Tags.BROWSER_AUTOMATION)
                return
            } catch (e: CancellationException) {
                Napier.w("Select step cancelled by user", tag = Tags.BROWSER_AUTOMATION)
                throw e
            } catch (e: Exception) {
                Napier.e("Failed to select option for selector $selector: ${e.message}", e, tag = Tags.BROWSER_AUTOMATION)
                lastError = e
            }
        }

        throw lastError ?: IllegalStateException("다음 요소에서 선택할 수 있는 옵션을 찾지 못했습니다: ${step.description}")
    }

    private suspend fun resolveSelectValue(
        step: PathStepDetail,
        options: List<SelectOption>
    ): SelectOption {
        val placeholder = step.inputPlaceholder?.trim()?.ifEmpty { null }
        val placeholderMatch = placeholder?.let { hint ->
            options.firstOrNull {
                it.label.trim() == hint || it.value.trim() == hint
            }
        }

        if (step.isInput == true && currentGetUserSelect != null) {
            val userSelectedValue = currentGetUserSelect?.invoke(step, options)
            if (!userSelectedValue.isNullOrBlank()) {
                return options.firstOrNull { it.value == userSelectedValue } ?: options.first()
            }
            throw CancellationException("User cancelled select input")
        }

        return placeholderMatch ?: options.firstOrNull { it.isSelected } ?: options.first()
    }

    /**
     * 카카오톡 인증 완료 스텝인지 확인
     * - description에 "카카오톡" + "인증" + "완료" 포함
     * - textLabels에 "인증 완료" 포함
     * - selectors에 "인증 완료" 버튼 포함
     */
    private fun isKakaoAuthCompleteStep(step: PathStepDetail): Boolean {
        val description = step.description.lowercase()
        val textLabels = step.textLabels?.map { it.lowercase() }
        val selectors = step.selectors.map { it.lowercase() }

        val hasKakaoAuthComplete = description.contains("카카오톡") &&
                description.contains("인증") &&
                description.contains("완료")

        val hasAuthCompleteLabel = textLabels?.any {
            it.contains("인증") && it.contains("완료")
        }

        val hasAuthCompleteSelector = selectors.any {
            it.contains("인증") && it.contains("완료")
        }

        return hasKakaoAuthComplete || hasAuthCompleteLabel == true || hasAuthCompleteSelector
    }
}
