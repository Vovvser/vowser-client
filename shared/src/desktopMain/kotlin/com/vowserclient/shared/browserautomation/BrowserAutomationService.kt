package com.vowser.client.browserautomation

import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.BrowserContext
import com.microsoft.playwright.options.ViewportSize
import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
import com.microsoft.playwright.PlaywrightException
import com.vowser.client.contribution.ContributionStep
import com.vowser.client.config.AppConfig
import com.vowser.client.contribution.ContributionConstants
import com.vowser.client.desktop.ScreenUtil
import com.google.gson.JsonObject
import io.github.aakira.napier.Napier
import com.vowser.client.logging.Tags
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren

object BrowserAutomationService {

    private lateinit var playwright: Playwright
    private lateinit var browser: Browser
    private lateinit var context: BrowserContext
    private lateinit var page: Page
    private val mutex = Mutex()

    private var contributionRecordingCallback: ((ContributionStep) -> Unit)? = null
    private var browserClosedCallback: (() -> Unit)? = null // contribution-mode specific
    private var generalBrowserClosedCallback: (() -> Unit)? = null
    private var isRecordingContributions = false
    private var pollingJob: Job? = null
    private var memoryCleanupJob: Job? = null
    private val pageTimestamps = mutableMapOf<Page, Pair<Long, Long>>() // (lastClickTimestamp, lastInputTimestamp)
    private val pageLastActivity = mutableMapOf<Page, Long>() // 마지막 활동 시간
    private val trackedPages = mutableSetOf<Page>()
    private val pagePollingMutex = Mutex()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var isExpectingClose = false
    private var isPlaywrightActive = false
    private var isBrowserActive = false
    private var isPageActive = false

    fun setContributionRecordingCallback(callback: (ContributionStep) -> Unit) {
        contributionRecordingCallback = callback
    }

    fun setContributionBrowserClosedCallback(callback: (() -> Unit)?) {
        browserClosedCallback = callback
    }

    fun setGeneralBrowserClosedCallback(callback: (() -> Unit)?) {
        generalBrowserClosedCallback = callback
    }

    suspend fun initialize() = withContext(Dispatchers.IO) {
        mutex.withLock {

            try {
                // Check Playwright initialization
                if (!::playwright.isInitialized || !isPlaywrightActive) {
                    if (::playwright.isInitialized) {
                        runCatching { playwright.close() }
                    }
                    playwright = Playwright.create()
                    isPlaywrightActive = true
                }

                // Check browser initialization
                if (!::browser.isInitialized || browser.isConnected.not() || !isBrowserActive) {
                    if (::browser.isInitialized) {
                        runCatching { browser.close() }
                    }
                    val config = AppConfig.getInstance()
                    val launchArgs = mutableListOf(
                        "--no-first-run",
                        "--no-default-browser-check",
                        "--disable-background-timer-throttling",
                        "--disable-backgrounding-occluded-windows",
                        "--disable-renderer-backgrounding",
                        "--disable-features=TranslateUI",
                        "--disable-ipc-flooding-protection",
                        "--disable-accessibility-api",
                        "--disable-dev-shm-usage",
                        "--no-sandbox",
                        "--disable-web-security",
                        "--disable-blink-features=AutomationControlled"
                    )

                    if (config.chromiumStartFullscreen) {
                        launchArgs.add("--start-fullscreen")
                    }

                    val dsf = config.chromiumDeviceScaleFactor
                    if (dsf != 1.0) {
                        launchArgs.add("--high-dpi-support=1")
                        launchArgs.add("--force-device-scale-factor=$dsf")
                    }

                    val (w, h) = config.chromiumWindowSize
                    val screen = ScreenUtil.currentPointerScreenBounds()
                    launchArgs.add("--window-position=${screen.x},${screen.y}")
                    launchArgs.add("--window-size=${w},${h}")

                    browser = playwright.chromium().launch(
                        BrowserType.LaunchOptions()
                            .setHeadless(false)
                            .setArgs(launchArgs)
                    )
                    isBrowserActive = true
                    browser.onDisconnected {
                        Napier.w("Browser disconnected", tag = Tags.BROWSER_AUTOMATION)
                        isBrowserActive = false
                        isPlaywrightActive = false
                        notifyBrowserClosed("Browser disconnected")
                    }
                }

                // Check page/context initialization
                var needNewPage = false
                if (!::page.isInitialized || !isPageActive) {
                    needNewPage = true
                } else {
                    try {
                        // 페이지 상태 체크
                        if (page.isClosed) {
                            needNewPage = true
                        }
                    } catch (e: Exception) {
                        needNewPage = true
                        Napier.w("BrowserAutomationService: Page status check failed, creating new page: ${e.message}", tag = Tags.BROWSER_AUTOMATION)
                    }
                }

                if (needNewPage) {
                    // Close previous context if any
                    runCatching { if (::context.isInitialized) context.close() }

                    // Create context with no fixed viewport so viewport follows window size
                    val ctxOptions = Browser.NewContextOptions().setViewportSize(null as ViewportSize?)
                    context = browser.newContext(ctxOptions)
                    page = context.newPage()
                    registerPageCloseWatcher(page)
                    setupContributionRecording()
                    page.waitForLoadState()
                    // Apply configured zoom after initial load
                    applyConfiguredPageZoom()
                    isPageActive = true
                    isExpectingClose = false
                }

                Napier.i("Browser automation initialized successfully", tag = Tags.BROWSER_AUTOMATION)

            } catch (e: Exception) {
                Napier.e("BrowserAutomationService: Critical initialization failure: ${e.message}", e, tag = Tags.BROWSER_AUTOMATION)
                // Complete cleanup and restart
                try {
                    if (::browser.isInitialized) browser.close()
                    if (::playwright.isInitialized) playwright.close()
                } catch (cleanupError: Exception) {
                    Napier.w("Cleanup error: ${cleanupError.message}", tag = Tags.BROWSER_AUTOMATION)
                }
                throw e
            }
        }
    }

    private fun applyConfiguredPageZoom(targetPage: Page? = null) {
        val p = targetPage ?: runCatching { page }.getOrNull() ?: return
        val config = AppConfig.getInstance()


        if (config.browserFitToWindow) {
            installFitToWindow(p)
            return
        }

        val zoom = config.browserZoom
        if (zoom == 1.0) return

        runCatching {
            val session = p.context().newCDPSession(p)
            val params = JsonObject().apply { addProperty("pageScaleFactor", zoom) }
            session.send("Emulation.setPageScaleFactor", params)
            Napier.i("Applied page scale via CDP: $zoom", tag = Tags.BROWSER_AUTOMATION)
        }.onFailure { cdpErr ->
            Napier.w("CDP page scale failed: ${cdpErr.message}. Falling back to CSS zoom.", tag = Tags.BROWSER_AUTOMATION)
            runCatching {
                p.evaluate("(z) => { try { document.documentElement.style.zoom = z; } catch (e) {} }", zoom)
                Napier.i("Applied page zoom via CSS: $zoom", tag = Tags.BROWSER_AUTOMATION)
            }.onFailure { cssErr ->
                Napier.w("CSS zoom apply failed: ${cssErr.message}", tag = Tags.BROWSER_AUTOMATION)
            }
        }
    }

    private fun installFitToWindow(targetPage: Page) {
        val script = """
            (() => {
              function computeContentSize() {
                const de = document.documentElement;
                const b = document.body || document.createElement('body');
                const w = Math.max(de.scrollWidth, de.clientWidth, b.scrollWidth || 0);
                const h = Math.max(de.scrollHeight, de.clientHeight, b.scrollHeight || 0);
                return { w, h };
              }

              function applyFit() {
                try {
                  const ww = window.innerWidth;
                  const wh = window.innerHeight;
                  const s = computeContentSize();
                  if (!ww || !wh || !s.w || !s.h) return;
                  const zx = ww / s.w;
                  const zy = wh / s.h;
                  const z = Math.max(0.25, Math.min(3.0, Math.min(zx, zy)));
                  // Prefer CSS zoom for simplicity
                  document.documentElement.style.zoom = z;
                  document.body && (document.body.style.zoom = z);
                } catch (e) {}
              }

              // Debounce to avoid thrashing
              let __vowserFitTimer = null;
              function scheduleFit() {
                if (__vowserFitTimer) cancelAnimationFrame(__vowserFitTimer);
                __vowserFitTimer = requestAnimationFrame(applyFit);
              }

              window.addEventListener('resize', scheduleFit, { passive: true });
              const ro = new ResizeObserver(scheduleFit);
              try { ro.observe(document.documentElement); } catch (e) {}
              try { document.body && ro.observe(document.body); } catch (e) {}

              // Initial application
              scheduleFit();
            })();
        """.trimIndent()

        runCatching {
            targetPage.evaluate(script)
            Napier.i("Installed fit-to-window zoom script", tag = Tags.BROWSER_AUTOMATION)
        }.onFailure { e ->
            Napier.w("Failed to install fit-to-window: ${e.message}", tag = Tags.BROWSER_AUTOMATION)
        }
    }



    suspend fun cleanup() = withContext(Dispatchers.IO) {
        mutex.withLock {
            isExpectingClose = true

            // Stop contribution mode
            stopContributionRecording()
            serviceScope.coroutineContext.cancelChildren()

            try {
                if (::page.isInitialized && !page.isClosed) { page.close() }
                if (::context.isInitialized) { context.close() }
            } catch (e: Exception) {
                Napier.w("Failed to close page: ${e.message}", tag = Tags.BROWSER_AUTOMATION)
            }
            isPageActive = false

            try {
                if (::browser.isInitialized && browser.isConnected) {
                    browser.close()
                }
            } catch (e: Exception) {
                Napier.w("Failed to close browser: ${e.message}", tag = Tags.BROWSER_AUTOMATION)
            }
            isBrowserActive = false

            try {
                if (::playwright.isInitialized) {
                    playwright.close()
                }
            } catch (e: Exception) {
                Napier.w("Failed to close playwright: ${e.message}", tag = Tags.BROWSER_AUTOMATION)
            }
            isPlaywrightActive = false
            isExpectingClose = false

            Napier.i("Browser automation cleanup completed", tag = Tags.BROWSER_AUTOMATION)
        }
    }

    suspend fun navigate(url: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!::page.isInitialized) {
                Napier.e("Cannot navigate: page not initialized", tag = Tags.BROWSER_AUTOMATION)
                return@withContext
            }
            try {
                page.navigate(url)
                AdaptiveWaitManager.waitForPageLoad(page, "navigation to $url")
                // about: 및 빈 URL은 기록하지 않음
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    recordContributionStep(url, page.title(), "navigate", null, null)
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                Napier.d("Navigation cancelled to $url", tag = Tags.BROWSER_AUTOMATION)
                throw e
            } catch (e: PlaywrightException) {
                Napier.e("Navigation failed to $url: ${e.message}", e, tag = Tags.BROWSER_AUTOMATION)
            } catch (e: Exception) {
                val msg = e.message ?: ""
                if (msg.contains("Target page, context or browser has been closed", ignoreCase = true)) {
                    Napier.w("Navigation aborted (target closed) to $url", tag = Tags.BROWSER_AUTOMATION)
                } else {
                    Napier.e("Navigation error: ${e.message}", e, tag = Tags.BROWSER_AUTOMATION)
                }
            }
        }
    }

    suspend fun goBack() = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!::page.isInitialized) {
                Napier.e("Cannot go back: page not initialized", tag = Tags.BROWSER_AUTOMATION)
                return@withContext
            }
            try {
                val response = page.goBack()
                if (response == null) {
                } else {
                    AdaptiveWaitManager.waitForPageLoad(page, "go back")
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: PlaywrightException) {
                Napier.e("Go back failed: ${e.message}", e, tag = Tags.BROWSER_AUTOMATION)
            } catch (e: Exception) {
                val msg = e.message ?: ""
                if (msg.contains("Target page, context or browser has been closed", ignoreCase = true)) {
                    Napier.w("Go back aborted (target closed)", tag = Tags.BROWSER_AUTOMATION)
                } else {
                    Napier.e("Go back error: ${e.message}", e, tag = Tags.BROWSER_AUTOMATION)
                }
            }
        }
    }

    suspend fun goForward() = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!::page.isInitialized) {
                Napier.e("Cannot go forward: page not initialized", tag = Tags.BROWSER_AUTOMATION)
                return@withContext
            }
            try {
                val response = page.goForward()
                if (response == null) {
                } else {
                    AdaptiveWaitManager.waitForPageLoad(page, "go forward")
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: PlaywrightException) {
                Napier.e("Go forward failed: ${e.message}", e, tag = Tags.BROWSER_AUTOMATION)
            } catch (e: Exception) {
                val msg = e.message ?: ""
                if (msg.contains("Target page, context or browser has been closed", ignoreCase = true)) {
                    Napier.w("Go forward aborted (target closed)", tag = Tags.BROWSER_AUTOMATION)
                } else {
                    Napier.e("Go forward error: ${e.message}", e, tag = Tags.BROWSER_AUTOMATION)
                }
            }
        }
    }

    suspend fun waitForNetworkIdle(timeout: Double = 3000.0) = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!::page.isInitialized) {
                Napier.e("Cannot waitForNetworkIdle: page not initialized", tag = Tags.BROWSER_AUTOMATION)
                return@withContext
            }
            try {
                Napier.d("Waiting for network idle state with ${timeout}ms timeout...", tag = Tags.BROWSER_AUTOMATION)
                page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE, Page.WaitForLoadStateOptions().setTimeout(timeout))
                Napier.i("Network is idle.", tag = Tags.BROWSER_AUTOMATION)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Napier.w("waitForNetworkIdle failed or timed out: ${e.message}", tag = Tags.BROWSER_AUTOMATION)
            }
        }
    }


    suspend fun hoverAndClickElement(selector: String, timeout: Double? = null): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!::page.isInitialized) {
                Napier.e("Cannot click element: page not initialized", tag = Tags.BROWSER_AUTOMATION)
                return@withContext false
            }
            Napier.d("--- hoverAndClickElement START ---", tag = Tags.BROWSER_AUTOMATION)
            Napier.d("Attempting to click selector: '$selector' on page: ${page.title()} (${page.url()})", tag = Tags.BROWSER_AUTOMATION)

            try {
                // 1. 메인 프레임에서 요소 찾기
                var locator = page.locator(selector)

                // 2. 메인 프레임에서 못 찾으면 iframe 탐색
                if (locator.count() == 0) {
                    Napier.i("Element not found in main frame, searching in iframes...", tag = Tags.BROWSER_AUTOMATION)
                    val iframeLocator = findElementInFrames(selector)
                    if (iframeLocator != null) {
                        locator = iframeLocator
                        Napier.i("Element found in iframe. Locator: ${locator.toString()}", tag = Tags.BROWSER_AUTOMATION)
                    } else {
                        Napier.w("Element with selector $selector not found in any frame", tag = Tags.BROWSER_AUTOMATION)
                        Napier.d("--- hoverAndClickElement END ---", tag = Tags.BROWSER_AUTOMATION)
                        return@withContext false
                    }
                }

                if (!AdaptiveWaitManager.waitForElement(locator, page, "element with selector: $selector", timeout)) {
                    Napier.w("Element with selector $selector not found or not visible after adaptive waiting.", tag = Tags.BROWSER_AUTOMATION)
                    Napier.d("--- hoverAndClickElement END ---", tag = Tags.BROWSER_AUTOMATION)
                    return@withContext false
                }

                val result = executeHoverHighlightClick(locator, selector)
                Napier.d("--- hoverAndClickElement END ---", tag = Tags.BROWSER_AUTOMATION)
                return@withContext result
            } catch (e: kotlinx.coroutines.CancellationException) {
                Napier.d("hoverAndClickElement cancelled for selector: $selector", tag = Tags.BROWSER_AUTOMATION)
                throw e
            } catch (e: PlaywrightException) {
                Napier.e("Failed to hoverAndClickElement $selector: ${e.message}", e, tag = Tags.BROWSER_AUTOMATION)
                Napier.d("--- hoverAndClickElement END ---", tag = Tags.BROWSER_AUTOMATION)
                return@withContext false
            }
        }
    }

    /**
     * 모든 iframe을 순회하며 요소 찾기
     */
    private suspend fun findElementInFrames(selector: String): Locator? = withContext(Dispatchers.IO) {
        if (!::page.isInitialized) return@withContext null

        try {
            val frames = page.frames()
            Napier.d("Searching in ${frames.size} frames for selector: '$selector'", tag = Tags.BROWSER_AUTOMATION)

            for ((index, frame) in frames.withIndex()) {
                try {
                    if (frame == page.mainFrame()) continue
                    Napier.d("  [Frame $index] Checking frame with name '${frame.name()}' and URL '${frame.url()}'", tag = Tags.BROWSER_AUTOMATION)

                    val frameLocator = frame.locator(selector)
                    if (frameLocator.count() > 0) {
                        Napier.i("Found element in iframe (name: '${frame.name()}', url: '${frame.url()}'). Locator: ${frameLocator.toString()}", tag = Tags.BROWSER_AUTOMATION)
                        return@withContext frameLocator
                    }
                } catch (e: Exception) {
                    Napier.d("Failed to access or check frame ${frame.name()} (${frame.url()}): ${e.message}", tag = Tags.BROWSER_AUTOMATION)
                }
            }

            // 정부24 처리(모듈화 예정)
            if (page.url().contains("gov.kr")) {
                Napier.i("Detected gov.kr domain, trying nested iframe search...", tag = Tags.BROWSER_AUTOMATION)
                return@withContext findElementInGovKrIframes(selector)
            }

        } catch (e: Exception) {
            Napier.w("Error during iframe search: ${e.message}", tag = Tags.BROWSER_AUTOMATION)
        }

        return@withContext null
    }

    /**
     * 정부24 포털 특화: 중첩된 iframe 구조 처리
     */
    private suspend fun findElementInGovKrIframes(selector: String): Locator? = withContext(Dispatchers.IO) {
        if (!::page.isInitialized) return@withContext null

        try {
            val commonIframeIds = listOf(
                "#mainFrame",
                "#contentFrame",
                "#bodyFrame",
                "[name='mainFrame']",
                "[name='contentFrame']"
            )
            Napier.d("Gov.kr iframe search: Trying common IDs: ${commonIframeIds.joinToString()}", tag = Tags.BROWSER_AUTOMATION)

            for (iframeId in commonIframeIds) {
                try {
                    Napier.d("  [Gov.kr] Checking iframe with selector: '$iframeId'", tag = Tags.BROWSER_AUTOMATION)
                    val frameLocator = page.frameLocator(iframeId)
                    val locator = frameLocator.locator(selector)

                    try {
                        if (locator.count() > 0) {
                            locator.first().waitFor(Locator.WaitForOptions().setState(com.microsoft.playwright.options.WaitForSelectorState.ATTACHED).setTimeout(1000.0)) // 요소가 DOM에 붙을 때까지 잠시 대기
                            Napier.i("Found element in gov.kr iframe '$iframeId'. Locator: ${locator.toString()}", tag = Tags.BROWSER_AUTOMATION)
                            return@withContext locator
                        }
                    } catch (e: Exception) {
                        Napier.d("Element not visible or attached yet in '$iframeId', continuing search.", tag = Tags.BROWSER_AUTOMATION)
                    }
                } catch (e: Exception) {
                    Napier.d("Could not find or access iframe '$iframeId', continuing search.", tag = Tags.BROWSER_AUTOMATION)
                }
            }
        } catch (e: Exception) {
            Napier.w("Error during gov.kr iframe search: ${e.message}", tag = Tags.BROWSER_AUTOMATION)
        }

        return@withContext null
    }

    private suspend fun executeHoverHighlightClick(locator: Locator, selector: String): Boolean = withContext(Dispatchers.IO) {
        Napier.d("--- executeHoverHighlightClick START ---", tag = Tags.BROWSER_AUTOMATION)
        Napier.d("Executing click on final locator: ${locator.toString()}", tag = Tags.BROWSER_AUTOMATION)
        try {
            val element = locator.first()
            var extractedAttributes: Map<String, String>? = null

            val preparationTime = measureTimeMillis {
                element.scrollIntoViewIfNeeded()
                Napier.d("Scrolled to element", tag = Tags.BROWSER_AUTOMATION)

                element.hover()
                Napier.d("Hovered over element", tag = Tags.BROWSER_AUTOMATION)

                val isStandardSelector = !selector.contains(":has-text")
                if (isStandardSelector) {
                    try {
                        page.evaluate(HIGHLIGHT_SCRIPT_CONTENT, selector)
                        Napier.i("Applied highlight to element with selector: $selector", tag = Tags.BROWSER_AUTOMATION)
                    } catch (e: Exception) {
                        Napier.w("Highlight failed: ${e.message}", tag = Tags.BROWSER_AUTOMATION)
                    }
                } else {
                    Napier.w("Skipping highlight for non-standard selector: $selector", tag = Tags.BROWSER_AUTOMATION)
                }

                delay(300)

                try {
                    element.evaluate("el => el.removeAttribute('target')")
                    Napier.i("Removed target attribute for selector: $selector", tag = Tags.BROWSER_AUTOMATION)
                } catch (jsError: Exception) {
                    Napier.w("Failed to remove target attribute: ${jsError.message}", tag = Tags.BROWSER_AUTOMATION)
                }

                val extractionTime = measureTimeMillis {
                    extractedAttributes = extractElementAttributes(element)
                }
                Napier.d("Attribute extraction took ${extractionTime}ms", tag = Tags.BROWSER_AUTOMATION)
            }
            Napier.d("Click preparation took ${preparationTime}ms", tag = Tags.BROWSER_AUTOMATION)

            val clickTime = measureTimeMillis {
                element.click(Locator.ClickOptions().setNoWaitAfter(true))
            }
            Napier.d("Click action took ${clickTime}ms", tag = Tags.BROWSER_AUTOMATION)

            recordContributionStep(
                runCatching { page.url() }.getOrElse { runCatching { page.url() }.getOrElse { "about:blank" } },
                runCatching { page.title() }.getOrElse { runCatching { page.title() }.getOrElse { "" } },
                "click",
                selector,
                extractedAttributes
            )
            Napier.i("Clicked element with selector: $selector", tag = Tags.BROWSER_AUTOMATION)
            Napier.d("--- executeHoverHighlightClick END ---", tag = Tags.BROWSER_AUTOMATION)
            return@withContext true

        } catch (e: Exception) {
            Napier.e("Failed to execute hover-highlight-click for $selector: ${e.message}", e, tag = Tags.BROWSER_AUTOMATION)
            Napier.d("--- executeHoverHighlightClick END ---", tag = Tags.BROWSER_AUTOMATION)
            return@withContext false
        }
    }


    suspend fun typeText(selector: String, text: String, delayMs: Double? = null) = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!::page.isInitialized) {
                Napier.e("Cannot type text: page not initialized", tag = Tags.BROWSER_AUTOMATION)
                return@withContext
            }
            Napier.d("--- typeText START ---", tag = Tags.BROWSER_AUTOMATION)
            Napier.d("Attempting to type '$text' into selector: '$selector' on page: ${page.title()} (${page.url()})", tag = Tags.BROWSER_AUTOMATION)

            try {
                // 1. 메인 프레임에서 요소 찾기
                var locator = page.locator(selector)

                // 2. 메인 프레임에서 못 찾으면 iframe 탐색
                if (locator.count() == 0) {
                    Napier.i("Input element not found in main frame, searching in iframes...", tag = Tags.BROWSER_AUTOMATION)
                    val iframeLocator = findElementInFrames(selector)
                    if (iframeLocator != null) {
                        locator = iframeLocator
                        Napier.i("Input element found in iframe. Locator: ${locator.toString()}", tag = Tags.BROWSER_AUTOMATION)
                    } else {
                        Napier.w("Element with selector $selector not found in any frame for typing.", tag = Tags.BROWSER_AUTOMATION)
                        Napier.d("--- typeText END ---", tag = Tags.BROWSER_AUTOMATION)
                        return@withContext
                    }
                }

                if (locator.count() > 0 && locator.first().isVisible) {
                    if (delayMs != null) {
                        locator.first().pressSequentially(text, Locator.PressSequentiallyOptions().setDelay(delayMs).setNoWaitAfter(true))
                    } else {
                        locator.first().fill(text)
                    }
                    recordContributionStep(
                        page.url(),
                        page.title(),
                        "type",
                        selector,
                        mapOf("text" to text)
                    )
                    Napier.i("Typed text '$text' into element with selector: $selector", tag = Tags.BROWSER_AUTOMATION)
                } else {
                    Napier.w("Element with selector $selector not found or not visible for typing.", tag = Tags.BROWSER_AUTOMATION)
                }
            } catch (e: PlaywrightException) {
                Napier.e("Failed to type text into element $selector: ${e.message}", e, tag = Tags.BROWSER_AUTOMATION)
            } catch (e: Exception) {
                Napier.e("Unexpected error typing text into element $selector: ${e.message}", e, tag = Tags.BROWSER_AUTOMATION)
            }
            Napier.d("--- typeText END ---", tag = Tags.BROWSER_AUTOMATION)
        }
    }

    private val HIGHLIGHT_SCRIPT_CONTENT = """
    (function(selector) {
        const styleId = 'wtg-highlight-styles';
        if (!document.getElementById(styleId)) {
            const style = document.createElement('style');
            style.id = styleId;
            // CSS 문자열은 JavaScript 템플릿 리터럴이 아닌 일반 문자열 연결로 처리
            style.innerHTML = "[data-wtg-highlighted=\"true\"] {" +
                "box-shadow: 0 0 0 5px rgba(0, 123, 255, 0.7) !important;" +
                "background-color: rgba(0, 123, 255, 0.1) !important;" +
                "transition: box-shadow 0.2s ease-in-out, background-color 0.2s ease-in-out !important;" +
            "}" +
            "#wtg-click-indicator {" +
                "position: absolute !important;" +
                "background-color: #007bff !important;" +
                "color: white !important;" +
                "padding: 5px 10px !important;" +
                "border-radius: 20px !important;" +
                "font-size: 12px !important;" +
                "font-weight: bold !important;" +
                "white-space: nowrap !important;" +
                "max-width: 80px !important;" +
                "overflow: hidden !important;" +
                "text-overflow: ellipsis !important;" +
                "z-index: 2147483647 !important;" +
                "pointer-events: none !important;" +
                "box-shadow: 0 2px 5px rgba(0,0,0,0.2) !important;" +
                "font-family: 'Inter', sans-serif, Arial, sans-serif !important;" +
                "opacity: 0;" +
                "animation: wtg-fade-in 0.3s forwards !important;" +
            "}" +
            "@keyframes wtg-fade-in {" +
                "from { opacity: 0; transform: translateY(10px); }" +
                "to { opacity: 1; transform: translateY(0); }" +
            "}";
            document.head.appendChild(style);
        }

        document.querySelectorAll('[data-wtg-highlighted]').forEach(el => {
            el.style.boxShadow = '';
            el.style.backgroundColor = '';
            el.removeAttribute('data-wtg-highlighted');
        });
        const existingIndicator = document.getElementById('wtg-click-indicator');
        if (existingIndicator) {
            existingIndicator.remove();
        }

        const element = document.querySelector(selector);
        if (!element) {
            return 'element_not_found';
        }
        element.setAttribute('data-wtg-highlighted', 'true');

        const indicator = document.createElement('div');
        indicator.id = 'wtg-click-indicator';
        indicator.textContent = 'Click Target';
        document.body.appendChild(indicator);
        indicator.offsetWidth;

        const rect = element.getBoundingClientRect();
        let indicatorLeft = rect.right + window.scrollX + 10;
        let indicatorTop = rect.top + window.scrollY - indicator.offsetHeight - 10;

        if (indicatorTop < window.scrollY) {
            indicatorTop = rect.bottom + window.scrollY + 10;
        }
        if (indicatorLeft + indicator.offsetWidth > window.innerWidth + window.scrollX) {
            indicatorLeft = rect.left + window.scrollX - indicator.offsetWidth - 10;
            if (indicatorLeft < window.scrollX) {
                indicatorLeft = window.scrollX + 10;
            }
        }

        indicator.style.left = indicatorLeft + 'px';
        indicator.style.top = indicatorTop + 'px';
        return 'highlight_added';
    })""".trimIndent()

    suspend fun startContributionRecording() {
        Napier.i("Starting contribution recording...", tag = Tags.BROWSER_AUTOMATION)

        try {
            initialize()
            isExpectingClose = false

            // Additional check after initialization
            if (!::page.isInitialized || page.isClosed) {
                throw Exception("Page initialization failed")
            }

            isRecordingContributions = true
            startUserInteractionPolling()
            startMemoryCleanupJob()

            // 리스너 주입 확인
            injectUserInteractionListeners()

            Napier.i("Contribution recording started successfully", tag = Tags.BROWSER_AUTOMATION)

        } catch (e: Exception) {
            Napier.e("Failed to start contribution recording: ${e.message}", e, tag = Tags.BROWSER_AUTOMATION)
            isRecordingContributions = false
            throw e
        }
    }

    fun stopContributionRecording() {
        isRecordingContributions = false
        contributionRecordingCallback = null
        pollingJob?.cancel()
        pollingJob = null
        memoryCleanupJob?.cancel()
        memoryCleanupJob = null
        trackedPages.clear()
        pageTimestamps.clear()
        pageLastActivity.clear()
        Napier.i("Stopped contribution recording", tag = Tags.BROWSER_AUTOMATION)
    }

    private fun setupContributionRecording() {
        if (!::page.isInitialized) return

        Napier.i("Setting up contribution recording listeners", tag = Tags.BROWSER_AUTOMATION)

        // Detect new tab opening. In contribution mode, redirect popup to same tab; otherwise leave default behavior.
        page.onPopup { newPage ->
            if (!isRecordingContributions) {
                Napier.i("Popup detected (non-contribution): ${runCatching { newPage.url() }.getOrElse { "" }}", tag = Tags.BROWSER_AUTOMATION)
                return@onPopup
            }
            val popupUrl = runCatching { newPage.url() }.getOrElse { "" }
            Napier.i("New tab detected during contribution: $popupUrl — redirecting to same tab", tag = Tags.BROWSER_AUTOMATION)
            try {
                runCatching { newPage.waitForLoadState() }
                val targetUrl = runCatching { newPage.url() }.getOrElse { popupUrl }
                if (targetUrl.isNotBlank() && targetUrl != "about:blank") {
                    runCatching { page.navigate(targetUrl) }
                    val title = runCatching { page.title() }.getOrElse { "" }
                    runCatching {
                        recordContributionStep(
                            targetUrl,
                            title,
                            "navigate",
                            null,
                            mapOf("from_popup" to "true")
                        )
                    }
                }
            } catch (e: Exception) {
                Napier.w("Failed to redirect popup into same tab: ${e.message}", tag = Tags.BROWSER_AUTOMATION)
            } finally {
                runCatching { newPage.close() }
            }
        }

        // Detect page navigation (URL change)
        page.onFrameNavigated { frame ->
            try {
                if (frame == page.mainFrame()) {
                    val url = runCatching { frame.url() }.getOrElse { "" }
                    Napier.i("Frame navigated to: $url", tag = Tags.BROWSER_AUTOMATION)
                    // about:blank 등은 기록을 생략하여 노이즈 감소
                    if (isRecordingContributions && (url.startsWith("http://") || url.startsWith("https://"))) {
                        val title = runCatching { frame.page().title() }.getOrElse { "" }
                        runCatching {
                            recordContributionStep(
                                url,
                                title,
                                "navigate",
                                null,
                                null
                            )
                        }
                    }
                    // 네비 직후에도 리스너가 즉시 준비되도록 재주입
                    runCatching { injectUserInteractionListeners(frame.page()) }
                }
            } catch (e: Exception) {
                Napier.w("onFrameNavigated handler error: ${e.message}", tag = Tags.BROWSER_AUTOMATION)
            }
        }

        // Inject user interaction listeners when page load completes
        page.onLoad {
            try {
                Napier.i("Page loaded, injecting listeners for: ${page.url()}", tag = Tags.BROWSER_AUTOMATION)
                injectUserInteractionListeners()
                applyConfiguredPageZoom()
            } catch (e: Exception) {
                Napier.w("Failed to inject user interaction listeners: ${e.message}", tag = Tags.BROWSER_AUTOMATION)
            }
        }

        // Inject earlier at DOMContentLoaded to avoid missing initial clicks
        page.onDOMContentLoaded {
            try {
                Napier.d("DOMContentLoaded, injecting listeners for: ${page.url()}", tag = Tags.BROWSER_AUTOMATION)
                injectUserInteractionListeners()
                applyConfiguredPageZoom()
            } catch (e: Exception) {
                Napier.w("Failed to inject listeners on DOMContentLoaded: ${e.message}", tag = Tags.BROWSER_AUTOMATION)
            }
        }

        // 콘솔 로그 리스너 (디버깅용)
        page.onConsoleMessage { message ->
            if (message.text().contains("Vowser") || message.text().contains("🖱️") || message.text().contains("⌨️")) {
                Napier.i("Browser Console: ${message.text()}", tag = Tags.BROWSER_AUTOMATION)
            }
        }

        // 페이지가 이미 로드된 경우 즉시 리스너 주입
        try {
            injectUserInteractionListeners()
            trackedPages.add(page)
            pageTimestamps[page] = Pair(0L, 0L)
            updatePageActivity(page)
        } catch (e: Exception) {
            Napier.w("Failed to inject initial user interaction listeners: ${e.message}", tag = Tags.BROWSER_AUTOMATION)
        }
    }

    private fun setupNewPageTracking(newPage: Page) {
        try {
            registerPageCloseWatcher(newPage)
            trackedPages.add(newPage)
            pageTimestamps[newPage] = Pair(0L, 0L)
            updatePageActivity(newPage)

            // 새 페이지 로드 시 리스너 주입
            newPage.onLoad {
                try {
                    Napier.i("New page loaded, injecting listeners for: ${newPage.url()}", tag = Tags.BROWSER_AUTOMATION)
                    injectUserInteractionListeners(newPage)
                    applyConfiguredPageZoom(newPage)
                } catch (e: Exception) {
                    Napier.w("Failed to inject listeners for new page: ${e.message}", tag = Tags.BROWSER_AUTOMATION)
                }
            }

            // 새 페이지의 콘솔 로그 리스너
            newPage.onConsoleMessage { message ->
                if (message.text().contains("Vowser") || message.text().contains("🖱️") || message.text().contains("⌨️")) {
                    Napier.i("New Tab Console: ${message.text()}", tag = Tags.BROWSER_AUTOMATION)
                }
            }

            // 새 페이지에 즉시 리스너 주입 시도
            try {
                injectUserInteractionListeners(newPage)
            } catch (e: Exception) {
                Napier.w("Failed to inject initial listeners for new page: ${e.message}", tag = Tags.BROWSER_AUTOMATION)
            }

        } catch (e: Exception) {
            Napier.e("Failed to setup new page tracking: ${e.message}", e, tag = Tags.BROWSER_AUTOMATION)
        }
    }

    private fun injectUserInteractionListeners(targetPage: Page = page): Boolean {
        if (!isRecordingContributions) return false

        val injected = targetPage.evaluate("""
            (function() {
                // 이미 리스너가 설정되어 있으면 중복 설정 방지
                if (window.__vowserContributionListenersSetup) return false;
                window.__vowserContributionListenersSetup = true;
                
                console.log('Vowser contribution listeners injected');
                
                // Prevent opening new tabs: force anchors to stay in same tab and override window.open
                if (!window.__vowserNoNewTabs) {
                    window.__vowserNoNewTabs = true;
                    try {
                        const __origOpen = window.open;
                        window.open = function(url, target, features) {
                            try {
                                if (url) {
                                    window.location.href = url;
                                    return null;
                                }
                            } catch (e) {}
                            return __origOpen ? __origOpen.apply(window, arguments) : null;
                        };
                    } catch (e) { /* ignore */ }

                    document.addEventListener('click', function(ev) {
                        try {
                            const el = ev.target;
                            if (!el || !el.closest) return;
                            const a = el.closest('a[target]');
                            if (a) {
                                a.setAttribute('target', '_self');
                            }
                            const anchor = el.closest('a[href]');
                            if (anchor && (ev.metaKey || ev.ctrlKey)) {
                                ev.preventDefault();
                                ev.stopImmediatePropagation();
                                try { anchor.setAttribute('target', '_self'); } catch (e) {}
                                window.location.href = anchor.href;
                            }
                        } catch (e) { /* ignore */ }
                    }, true);

                    // Middle-click(auxclick) opens new tab by default; force same-tab navigation
                    document.addEventListener('auxclick', function(ev) {
                        try {
                            if (ev.button !== 1) return; // only middle button
                            const el = ev.target;
                            if (!el || !el.closest) return;
                            const anchor = el.closest('a[href]');
                            if (anchor) {
                                ev.preventDefault();
                                ev.stopImmediatePropagation();
                                try { anchor.setAttribute('target', '_self'); } catch (e) {}
                                window.location.href = anchor.href;
                            }
                        } catch (e) { /* ignore */ }
                    }, true);
                }

                // Click event detection (prefer closest anchor/button target)
                document.addEventListener('click', function(event) {
                    const original = event.target;
                    let element = original;
                    try {
                        if (original && original.closest) {
                            const candidate = original.closest('a,button,[role="button"]');
                            if (candidate) element = candidate;
                        }
                    } catch (e) { /* ignore */ }

                    const selector = generateSelector(element);
                    const isAnchor = element && element.tagName && element.tagName.toLowerCase() === 'a';
                    const href = isAnchor ? (element.href || element.getAttribute('href') || '') : (element.getAttribute ? (element.getAttribute('href') || '') : '');
                    const attributes = {
                        'text': (element.innerText || element.textContent || '').trim(),
                        'tag': element.tagName?.toLowerCase() || '',
                        'id': element.id || '',
                        'class': element.className || '',
                        'href': href,
                        'type': element.type || '',
                        'name': element.name || '',
                        'value': (element.value !== undefined ? (element.value || '') : '')
                    };
                    
                    window.__vowserLastClick = {
                        selector: selector,
                        attributes: attributes,
                        timestamp: Date.now()
                    };
                    try { localStorage.setItem('__vowser_last_click', JSON.stringify(window.__vowserLastClick)); } catch (e) {}
                    
                    console.log('Click detected:', selector, attributes);
                }, true);
                
                // 입력 이벤트 감지
                document.addEventListener('input', function(event) {
                    const element = event.target;
                    if (element.tagName === 'INPUT' || element.tagName === 'TEXTAREA') {
                        const selector = generateSelector(element);
                        const attributes = {
                            'text': element.value || '',
                            'tag': element.tagName?.toLowerCase() || '',
                            'id': element.id || '',
                            'class': element.className || '',
                            'type': element.type || '',
                            'placeholder': element.placeholder || ''
                        };
                        
                        window.__vowserLastInput = {
                            selector: selector,
                            attributes: attributes,
                            timestamp: Date.now()
                        };
                        
                        console.log('Input detected:', selector, attributes);
                    }
                }, true);
                
                // Enter 키 감지 (keydown 이벤트)
                document.addEventListener('keydown', function(event) {
                    if (event.key === 'Enter' || event.keyCode === 13) {
                        const element = event.target;
                        if (element.tagName === 'INPUT' || element.tagName === 'TEXTAREA') {
                            const selector = generateSelector(element);
                            const attributes = {
                                'text': element.value || '',
                                'tag': element.tagName?.toLowerCase() || '',
                                'id': element.id || '',
                                'class': element.className || '',
                                'type': element.type || '',
                                'placeholder': element.placeholder || '',
                                'key': 'enter',
                                'keyCode': '13'
                            };
                            
                            window.__vowserLastEnterKey = {
                                selector: selector,
                                attributes: attributes,
                                timestamp: Date.now()
                            };
                            
                            console.log('Enter key detected:', selector, attributes);
                        }
                    }
                }, true);
                
                // 셀렉터 생성 함수 (간단한 nth-of-type 포함 + CSS.escape 활용)
                function generateSelector(element) {
                    if (!element) return '';
                    const esc = (window.CSS && CSS.escape) ? CSS.escape : (s => String(s).replace(/[^a-zA-Z0-9_-]/g, '\\$&'));
                    if (element.id) {
                        return '#' + esc(element.id);
                    }

                    const tag = element.tagName.toLowerCase();
                    let base = tag;

                    if (element.className) {
                        const classes = element.className.split(' ').filter(c => c);
                        if (classes.length > 0) {
                            const escClasses = classes.map(c => esc(c));
                            base += '.' + escClasses.join('.');
                        }
                    }

                    const parent = element.parentElement;
                    if (parent && parent !== document.body) {
                        let part = base;
                        try {
                            const siblings = Array.from(parent.children).filter(n => n.tagName && n.tagName.toLowerCase() === tag);
                            if (siblings.length > 1) {
                                const index = siblings.indexOf(element) + 1;
                                if (index > 0) part += ':nth-of-type(' + index + ')';
                            }
                        } catch (e) { /* ignore */ }
                        const parentSelector = parent.id ? ('#' + esc(parent.id)) : parent.tagName.toLowerCase();
                        return parentSelector + ' > ' + part;
                    }

                    return base;
                }
                return true;
            })();
        """) as? Boolean ?: false

        if (injected) {
            Napier.i("User interaction listeners injected", tag = Tags.BROWSER_AUTOMATION)
        } else {
            Napier.d("Listeners already present for: ${targetPage.url()}", tag = Tags.BROWSER_AUTOMATION)
        }
        return injected
    }

    private fun startUserInteractionPolling() {
        pollingJob?.cancel()
        pollingJob = serviceScope.launch {
            Napier.i("Starting user interaction polling", tag = Tags.BROWSER_AUTOMATION)
            while (isRecordingContributions && ::page.isInitialized) {
                try {
                    checkForUserInteractions()
                    delay(ContributionConstants.POLLING_INTERVAL_MS)
                } catch (e: Exception) {
                    Napier.w("Error polling user interactions: ${e.message}", tag = Tags.BROWSER_AUTOMATION)
                    delay(2000) // 에러 시 2초 대기
                }
            }
            Napier.i("User interaction polling stopped", tag = Tags.BROWSER_AUTOMATION)
        }
    }

    private suspend fun checkForUserInteractions() {
        if (!isRecordingContributions) return

        pagePollingMutex.withLock {
            // 모든 추적된 페이지에서 상호작용 체크
            val pagesToCheck = trackedPages.toList()

            for (targetPage in pagesToCheck) {
                try {
                    if (targetPage.isClosed) {
                        cleanupPage(targetPage)
                        continue
                    }

                    checkPageInteractions(targetPage)

                } catch (e: Exception) {
                    Napier.w("Error checking interactions for page: ${e.message}", tag = Tags.BROWSER_AUTOMATION)
                    // 페이지 접근 실패 시 추적에서 제거
                    cleanupPage(targetPage)
                }
            }
        }
    }

    private fun notifyBrowserClosed(reason: String) {
        if (isExpectingClose) return

        // Always notify general callback (non-contribution) if set
        serviceScope.launch {
            try {
                generalBrowserClosedCallback?.invoke()
            } catch (_: Exception) { }
        }

        // Contribution-mode specific handling and callback
        if (isRecordingContributions) {
            Napier.w("Contribution browser closed unexpectedly: $reason", tag = Tags.BROWSER_AUTOMATION)
            isPageActive = false
            isRecordingContributions = false
            serviceScope.launch {
                try {
                    browserClosedCallback?.invoke()
                } catch (_: Exception) { }
            }
        } else {
            Napier.w("Browser window closed: $reason", tag = Tags.BROWSER_AUTOMATION)
            isPageActive = false
        }
    }

    private fun registerPageCloseWatcher(targetPage: Page) {
        targetPage.onClose {
            isPageActive = false
            notifyBrowserClosed("Page closed: ${targetPage.url()}")
            // Ensure playwright/browser resources are stopped when the window is closed
            serviceScope.launch {
                runCatching { cleanup() }
                    .onFailure { Napier.w("Cleanup after window close failed: ${it.message}", it, tag = Tags.BROWSER_AUTOMATION) }
            }
        }
    }

    private fun cleanupPage(targetPage: Page) {
        trackedPages.remove(targetPage)
        pageTimestamps.remove(targetPage)
        pageLastActivity.remove(targetPage)
    }

    private fun checkPageInteractions(targetPage: Page) {
        try {
            // 리스너 상태 체크
            val listenersSetup = targetPage.evaluate("window.__vowserContributionListenersSetup")
            if (listenersSetup != true) {
                Napier.d("Listeners not setup for ${targetPage.url()}, re-injecting...", tag = Tags.BROWSER_AUTOMATION)
                injectUserInteractionListeners(targetPage)
                return
            }

            // Check click events
            val clickData = targetPage.evaluate("window.__vowserLastClick")

            if (clickData != null) {
                val clickMap = clickData as? Map<*, *>
                val timestamp = (clickMap?.get("timestamp") as? Number)?.toLong() ?: 0L
                val pageTimestamp = pageTimestamps[targetPage]?.first ?: 0L

                if (timestamp > pageTimestamp) {
                    val selector = clickMap?.get("selector") as? String ?: ""
                    val attributesMap = clickMap?.get("attributes") as? Map<*, *> ?: emptyMap<String, String>()
                    val attributes = attributesMap.mapKeys { it.key.toString() }.mapValues { it.value.toString() }

                    val buttonText = attributes["text"]?.take(ContributionConstants.MAX_ELEMENT_TEXT_LENGTH) ?: "No text"
                    Napier.i("Button clicked: [$buttonText] on $selector (${targetPage.url()})", tag = Tags.BROWSER_AUTOMATION)

                    recordContributionStep(
                        targetPage.url(),
                        targetPage.title(),
                        "click",
                        selector,
                        attributes
                    )

                    // 페이지별 타임스탬프 업데이트
                    val currentInputTimestamp = pageTimestamps[targetPage]?.second ?: 0L
                    pageTimestamps[targetPage] = Pair(timestamp, currentInputTimestamp)
                    updatePageActivity(targetPage)

                    // Clear processed click data
                    targetPage.evaluate("window.__vowserLastClick = null;")
                }
            }

            // 네비 직후 손실 방지를 위해 localStorage에 임시 저장된 클릭 이벤트 복구
            val persistedClick = targetPage.evaluate(
                """
                (() => {
                    try {
                        const raw = localStorage.getItem('__vowser_last_click');
                        if (!raw) return null;
                        const obj = JSON.parse(raw);
                        return {
                            selector: obj && obj.selector ? obj.selector : '',
                            attributes: obj && obj.attributes ? obj.attributes : {},
                            timestamp: obj && obj.timestamp ? obj.timestamp : Date.now()
                        };
                    } catch (e) { return null }
                })()
                """
            )
            if (persistedClick != null) {
                try {
                    val persistedMap = persistedClick as? Map<*, *>
                    val timestamp = (persistedMap?.get("timestamp") as? Number)?.toLong() ?: 0L
                    val pageTimestamp = pageTimestamps[targetPage]?.first ?: 0L
                    if (timestamp > pageTimestamp) {
                        val selector = persistedMap?.get("selector") as? String ?: ""
                        val attributesMap = persistedMap?.get("attributes") as? Map<*, *> ?: emptyMap<String, String>()
                        val attributes = attributesMap.mapKeys { it.key.toString() }.mapValues { it.value.toString() }

                        recordContributionStep(
                            targetPage.url(),
                            targetPage.title(),
                            "click",
                            selector,
                            attributes
                        )

                        val currentInputTimestamp = pageTimestamps[targetPage]?.second ?: 0L
                        pageTimestamps[targetPage] = Pair(timestamp, currentInputTimestamp)
                        updatePageActivity(targetPage)
                    }
                } catch (_: Exception) {
                    // ignore parse errors
                } finally {
                    runCatching { targetPage.evaluate("localStorage.removeItem('__vowser_last_click')") }
                }
            }

            // 입력 이벤트 체크
            val inputData = targetPage.evaluate("window.__vowserLastInput")
            if (inputData != null) {
                val inputMap = inputData as? Map<*, *>
                val timestamp = (inputMap?.get("timestamp") as? Number)?.toLong() ?: 0L
                val pageInputTimestamp = pageTimestamps[targetPage]?.second ?: 0L

                if (timestamp > pageInputTimestamp) {
                    val selector = inputMap?.get("selector") as? String ?: ""
                    val attributesMap = inputMap?.get("attributes") as? Map<*, *> ?: emptyMap<String, String>()
                    val attributes = attributesMap.mapKeys { it.key.toString() }.mapValues { it.value.toString() }

                    recordContributionStep(
                        targetPage.url(),
                        targetPage.title(),
                        "type",
                        selector,
                        attributes
                    )

                    // 페이지별 입력 타임스탬프 업데이트
                    val currentClickTimestamp = pageTimestamps[targetPage]?.first ?: 0L
                    pageTimestamps[targetPage] = Pair(currentClickTimestamp, timestamp)
                    updatePageActivity(targetPage)

                    // 처리된 입력 데이터 클리어
                    targetPage.evaluate("window.__vowserLastInput = null;")
                }
            }

            // Enter 키 이벤트 체크 (타이핑 완료 시그널)
            val enterKeyData = targetPage.evaluate("window.__vowserLastEnterKey")
            if (enterKeyData != null) {
                val enterMap = enterKeyData as? Map<*, *>
                val timestamp = (enterMap?.get("timestamp") as? Number)?.toLong() ?: 0L
                val pageInputTimestamp = pageTimestamps[targetPage]?.second ?: 0L

                if (timestamp > pageInputTimestamp) {
                    val selector = enterMap?.get("selector") as? String ?: ""
                    val attributesMap = enterMap?.get("attributes") as? Map<*, *> ?: emptyMap<String, String>()
                    val attributes = attributesMap.mapKeys { it.key.toString() }.mapValues { it.value.toString() }

                    val inputText = attributes["text"]?.take(ContributionConstants.MAX_ELEMENT_TEXT_LENGTH) ?: "No text"
                    Napier.i("Enter key pressed: [$inputText] on $selector (${targetPage.url()})", tag = Tags.BROWSER_AUTOMATION)

                    recordContributionStep(
                        targetPage.url(),
                        targetPage.title(),
                        "type",
                        selector,
                        attributes
                    )

                    // Enter 키는 별도 타임스탬프로 처리하지 않고 입력 타임스탬프 업데이트
                    val currentClickTimestamp = pageTimestamps[targetPage]?.first ?: 0L
                    pageTimestamps[targetPage] = Pair(currentClickTimestamp, timestamp)
                    updatePageActivity(targetPage)

                    // 처리된 Enter 키 데이터 클리어
                    targetPage.evaluate("window.__vowserLastEnterKey = null;")
                }
            }
        } catch (e: Exception) {
            Napier.w("Error checking interactions for page: ${e.message}", tag = Tags.BROWSER_AUTOMATION)
        }
    }

    private fun recordContributionStep(
        url: String,
        title: String,
        action: String,
        selector: String?,
        htmlAttributes: Map<String, String>?
    ) {
        if (!isRecordingContributions || contributionRecordingCallback == null) return
        // about: 및 빈 URL 내비게이션은 기록하지 않음
        if (action == "navigate" && (url.isBlank() || url.startsWith("about:"))) return

        val step = ContributionStep(
            url = url,
            title = title,
            action = action,
            selector = selector,
            htmlAttributes = htmlAttributes
        )

        contributionRecordingCallback?.invoke(step)
        Napier.i("Contribution Step Recorded: [${step.action}] ${step.title} (${step.url})", tag = Tags.BROWSER_AUTOMATION)
    }

    suspend fun getSelectOptions(selector: String): List<SelectOption> = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!::page.isInitialized) {
                initialize()
            }

            var locator = page.locator(selector)
            if (locator.count() == 0) {
                val frameLocator = findElementInFrames(selector)
                if (frameLocator != null) {
                    locator = frameLocator
                } else {
                    Napier.w("Select element not found for selector: $selector", tag = Tags.BROWSER_AUTOMATION)
                    return@withLock emptyList()
                }
            }

            val selectElement = locator.first()
            val optionLocator = selectElement.locator("option")
            val count = optionLocator.count()

            (0 until count).mapNotNull { index ->
                runCatching {
                    val option = optionLocator.nth(index)
                    val value = option.getAttribute("value")?.trim().orEmpty()
                    val label = option.textContent()?.trim().orEmpty()
                    val selected = runCatching { option.evaluate("opt => opt.selected") as? Boolean }
                        .getOrNull() ?: false

                    if (value.isBlank() && label.isBlank()) {
                        null
                    } else {
                        SelectOption(
                            value = if (value.isNotBlank()) value else label,
                            label = if (label.isNotBlank()) label else value,
                            isSelected = selected
                        )
                    }
                }.getOrElse { error ->
                    Napier.w("Failed to read option at index $index for selector $selector: ${error.message}", tag = Tags.BROWSER_AUTOMATION)
                    null
                }
            }
        }
    }

    suspend fun selectOption(selector: String, value: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!::page.isInitialized) {
                initialize()
            }

            var locator = page.locator(selector)
            if (locator.count() == 0) {
                val frameLocator = findElementInFrames(selector)
                if (frameLocator != null) {
                    locator = frameLocator
                } else {
                    throw PlaywrightException("Select element not found for selector: $selector")
                }
            }

            try {
                locator.first().selectOption(value)
                Napier.i("Selected option value '$value' on selector $selector", tag = Tags.BROWSER_AUTOMATION)
            } catch (e: Exception) {
                Napier.e("Failed to select option '$value' on $selector: ${e.message}", e, tag = Tags.BROWSER_AUTOMATION)
                throw e
            }
        }
    }

    suspend fun setInputValue(selector: String, value: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!::page.isInitialized) {
                initialize()
            }

            var locator = page.locator(selector)
            if (locator.count() == 0) {
                val frameLocator = findElementInFrames(selector)
                if (frameLocator != null) {
                    locator = frameLocator
                } else {
                    throw PlaywrightException("Input element not found for selector: $selector")
                }
            }

            val element = locator.first()
            if (!element.isVisible) {
                throw PlaywrightException("Input element not visible for selector: $selector")
            }

            try {
                element.focus()
                element.evaluate("el => { el.value = ''; el.dispatchEvent(new Event('input', { bubbles: true })); }")
                element.fill(value)
                element.evaluate("el => el.dispatchEvent(new Event('change', { bubbles: true }))")
                Napier.i("Input value set for selector $selector", tag = Tags.BROWSER_AUTOMATION)
            } catch (e: Exception) {
                Napier.e("Failed to set input value for $selector: ${e.message}", e, tag = Tags.BROWSER_AUTOMATION)
                throw e
            }
        }
    }

    private fun extractElementAttributes(locator: Locator): Map<String, String> {
        return try {
            val element = locator.first()

            @Suppress("UNCHECKED_CAST")
            val attributes = element.evaluate(
                """(element) => {
                    const result = {};
                    const textContent = element.textContent || "";
                    const trimmedText = textContent.trim();
                    if (trimmedText.length > 0) {
                        result.text = trimmedText;
                    }

                    const attributeNames = ["id", "class", "name", "type", "href", "alt", "title", "aria-label", "placeholder", "value"];
                    for (const name of attributeNames) {
                        const value = element.getAttribute(name);
                        if (value && value.trim().length > 0) {
                            result[name] = value;
                        }
                    }

                    if (element instanceof HTMLInputElement || element instanceof HTMLTextAreaElement) {
                        if (!result.type && element.type && element.type.trim().length > 0) {
                            result.type = element.type;
                        }
                        if (!result.placeholder && element.placeholder && element.placeholder.trim().length > 0) {
                            result.placeholder = element.placeholder;
                        }
                        if (!result.value && element.value && element.value.trim().length > 0) {
                            result.value = element.value;
                        }
                    }

                    return result;
                }"""
            ) as? Map<String, Any?>

            attributes
                ?.mapNotNull { (key, value) ->
                    val stringValue = value?.toString()?.trim()
                    if (stringValue.isNullOrEmpty()) null else key to stringValue
                }
                ?.toMap()
                ?: emptyMap()
        } catch (e: Exception) {
            Napier.w("Failed to extract element attributes via script: ${e.message}", tag = Tags.BROWSER_AUTOMATION)

            try {
                val element = locator.first()
                val map = mutableMapOf<String, String>()

                element.textContent()
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { map["text"] = it }

                listOf("id", "class", "name", "type", "href", "alt", "title", "aria-label", "placeholder", "value")
                    .forEach { attr ->
                        runCatching { element.getAttribute(attr) }
                            .getOrNull()
                            ?.trim()
                            ?.takeIf { it.isNotEmpty() }
                            ?.let { map[attr] = it }
                    }

                map
            } catch (fallbackError: Exception) {
                Napier.w("Fallback attribute extraction failed: ${fallbackError.message}", tag = Tags.BROWSER_AUTOMATION)
                emptyMap()
            }
        }
    }

    private fun startMemoryCleanupJob() {
        memoryCleanupJob?.cancel()
        memoryCleanupJob = serviceScope.launch {
            Napier.i("Starting contribution memory cleanup job", tag = Tags.BROWSER_AUTOMATION)
            while (isRecordingContributions) {
                try {
                    delay(ContributionConstants.MEMORY_CLEANUP_INTERVAL_MS)
                    performMemoryCleanup()
                } catch (e: Exception) {
                    Napier.w("Memory cleanup error: ${e.message}", tag = Tags.BROWSER_AUTOMATION)
                }
            }
        }
    }

    private suspend fun performMemoryCleanup() {
        pagePollingMutex.withLock {
            val currentTime = System.currentTimeMillis()
            val inactivePages = mutableListOf<Page>()

            // 비활성 페이지 찾기
            pageLastActivity.forEach { (page, lastActivity) ->
                if (currentTime - lastActivity > ContributionConstants.PAGE_INACTIVE_TIMEOUT_MS) {
                    try {
                        // 페이지가 여전히 유효한지 확인
                        if (page.isClosed) {
                            inactivePages.add(page)
                        } else {
                            // 페이지가 살아있지만 오래 비활성 상태면 정리
                            inactivePages.add(page)
                        }
                    } catch (e: Exception) {
                        // Add to cleanup list if page access fails
                        inactivePages.add(page)
                    }
                }
            }

            // 최대 페이지 수 초과 시 오래된 페이지부터 정리
            if (trackedPages.size > ContributionConstants.MAX_TRACKED_PAGES) {
                val sortedByActivity = pageLastActivity.toList()
                    .sortedBy { it.second }
                    .map { it.first }
                val pagesToRemove = sortedByActivity.take(trackedPages.size - ContributionConstants.MAX_TRACKED_PAGES)
                inactivePages.addAll(pagesToRemove)
            }

            // 비활성 페이지 정리
            inactivePages.forEach { page ->
                cleanupPage(page)
            }

            if (inactivePages.isNotEmpty()) {
                Napier.i("Cleaned up ${inactivePages.size} inactive pages. Remaining: ${trackedPages.size}", tag = Tags.BROWSER_AUTOMATION)
            }
        }
    }

    private fun updatePageActivity(page: Page) {
        pageLastActivity[page] = System.currentTimeMillis()
    }
}
