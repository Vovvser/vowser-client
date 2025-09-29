package com.vowser.client.browserautomation

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import com.microsoft.playwright.PlaywrightException
import io.github.aakira.napier.Napier
import com.vowser.client.logging.Tags
import kotlinx.coroutines.delay

/**
 * 네트워크 상태와 페이지 로딩 상태에 따라 대기, 실행 여부를 결정
 */
object AdaptiveWaitManager {

    private const val TAG = "AdaptiveWaitManager"

    // 대기 설정
    private const val MIN_TIMEOUT_MS = 2000.0
    private const val MAX_TIMEOUT_MS = 15000.0
    private const val BASE_TIMEOUT_MS = 5000.0

    // 네트워크 상태 기반 대기 시간 계산
    private fun calculateTimeout(page: Page): Double {
        return try {
            // 페이지 성능 메트릭 확인 시도
            val networkState = checkNetworkState(page)
            when (networkState) {
                NetworkState.FAST -> MIN_TIMEOUT_MS
                NetworkState.NORMAL -> BASE_TIMEOUT_MS
                NetworkState.SLOW -> MAX_TIMEOUT_MS
            }
        } catch (e: Exception) {
            Napier.w("Failed to check network state, using base timeout: ${e.message}", tag = Tags.BROWSER_AUTOMATION)
            BASE_TIMEOUT_MS
        }
    }

    private fun checkNetworkState(page: Page): NetworkState {
        return try {
            // 페이지 로딩 상태 확인
            val isNetworkIdle = page.evaluate("""
                () => {
                    return document.readyState === 'complete' &&
                           performance.timing.loadEventEnd > 0;
                }
            """) as Boolean

            if (isNetworkIdle) {
                NetworkState.FAST
            } else {
                // DOM에서 로딩 인디케이터 확인
                val hasLoadingIndicators = page.evaluate("""
                    () => {
                        const loadingSelectors = [
                            '[class*="loading"]',
                            '[class*="spinner"]',
                            '[class*="skeleton"]',
                            '.loading-spinner',
                            '.load-more'
                        ];
                        return loadingSelectors.some(selector =>
                            document.querySelector(selector) !== null
                        );
                    }
                """) as Boolean

                if (hasLoadingIndicators) NetworkState.SLOW else NetworkState.NORMAL
            }
        } catch (e: Exception) {
            Napier.d("Network state check failed, assuming normal: ${e.message}", tag = Tags.BROWSER_AUTOMATION)
            NetworkState.NORMAL
        }
    }

    /**
     * 네트워크 상태에 따라 대기 시간 조정
     */
    suspend fun waitForElement(
        locator: Locator,
        page: Page,
        description: String = "element"
    ): Boolean {
        val adaptiveTimeout = calculateTimeout(page)

        Napier.d("Waiting for $description with adaptive timeout: ${adaptiveTimeout}ms", tag = Tags.BROWSER_AUTOMATION)

        return try {
            locator.first().waitFor(
                Locator.WaitForOptions()
                    .setTimeout(adaptiveTimeout)
            )

            // 추가 가시성 확인
            val isVisible = locator.first().isVisible
            if (isVisible) {
                Napier.d("$description found and visible within ${adaptiveTimeout}ms", tag = Tags.BROWSER_AUTOMATION)
            } else {
                Napier.w("$description found but not visible after ${adaptiveTimeout}ms", tag = Tags.BROWSER_AUTOMATION)
            }

            isVisible
        } catch (e: PlaywrightException) {
            Napier.w("$description not found within ${adaptiveTimeout}ms: ${e.message}", tag = Tags.BROWSER_AUTOMATION)
            false
        }
    }

    /**
     * 페이지 로딩이 완료될 때까지 대기
     */
    suspend fun waitForPageLoad(page: Page, description: String = "page") {
        Napier.d("Waiting for $description load with adaptive approach", tag = Tags.BROWSER_AUTOMATION)

        try {
            // 기본 로드 상태 대기
            page.waitForLoadState()

            // 네트워크 유휴 상태까지 추가 대기
            val networkState = checkNetworkState(page)
            if (networkState == NetworkState.SLOW) {
                Napier.d("Slow network detected, waiting for network idle", tag = Tags.BROWSER_AUTOMATION)
                page.waitForLoadState()
            }

            // DOM 안정화 대기
            waitForDomStable(page)

            Napier.i("$description loading completed", tag = Tags.BROWSER_AUTOMATION)

        } catch (e: Exception) {
            Napier.w("Page load wait completed with warnings: ${e.message}", tag = Tags.BROWSER_AUTOMATION)
        }
    }

    /**
     * DOM이 안정화될 때까지 대기
     */
    private suspend fun waitForDomStable(page: Page, maxAttempts: Int = 5) {
        var previousNodeCount = 0
        var stableCount = 0

        repeat(maxAttempts) { attempt ->
            try {
                val currentNodeCount = page.evaluate("() => document.getElementsByTagName('*').length") as Int

                if (currentNodeCount == previousNodeCount) {
                    stableCount++
                    if (stableCount >= 2) {
                        Napier.d("DOM stable after ${attempt + 1} checks", tag = Tags.BROWSER_AUTOMATION)
                        return
                    }
                } else {
                    stableCount = 0
                }

                previousNodeCount = currentNodeCount
                delay(500)

            } catch (e: Exception) {
                Napier.d("DOM stability check failed: ${e.message}", tag = Tags.BROWSER_AUTOMATION)
                return
            }
        }
    }

    /**
     * 동적 콘텐츠 로딩 대기 (무한 스크롤, AJAX 등)
     */
    suspend fun waitForDynamicContent(
        page: Page,
        checkScript: String,
        description: String = "dynamic content",
        maxAttempts: Int = 10
    ): Boolean {
        Napier.d("Waiting for $description with dynamic checks", tag = Tags.BROWSER_AUTOMATION)

        repeat(maxAttempts) { attempt ->
            try {
                val isReady = page.evaluate(checkScript) as Boolean
                if (isReady) {
                    Napier.i("$description ready after ${attempt + 1} attempts", tag = Tags.BROWSER_AUTOMATION)
                    return true
                }

                // 점진적으로 대기 시간 증가
                val delayMs = minOf(500 + (attempt * 200), 2000)
                delay(delayMs.toLong())

            } catch (e: Exception) {
                Napier.w("Dynamic content check failed (attempt ${attempt + 1}): ${e.message}", tag = Tags.BROWSER_AUTOMATION)
            }
        }

        Napier.w("$description not ready after $maxAttempts attempts", tag = Tags.BROWSER_AUTOMATION)
        return false
    }
}

private enum class NetworkState {
    FAST, NORMAL, SLOW
}