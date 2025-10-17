package com.vowser.client.browserautomation

import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
import com.microsoft.playwright.PlaywrightException
import com.vowser.client.contribution.ContributionStep
import com.vowser.client.contribution.ContributionConstants
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

object BrowserAutomationService {

    private lateinit var playwright: Playwright
    private lateinit var browser: Browser
    private lateinit var page: Page
    private val mutex = Mutex()

    // Memory management uses ContributionConstants

    private var contributionRecordingCallback: ((ContributionStep) -> Unit)? = null
    private var isRecordingContributions = false
    private var pollingJob: Job? = null
    private var memoryCleanupJob: Job? = null
    private val pageTimestamps = mutableMapOf<Page, Pair<Long, Long>>() // (lastClickTimestamp, lastInputTimestamp)
    private val pageLastActivity = mutableMapOf<Page, Long>() // 마지막 활동 시간
    private val trackedPages = mutableSetOf<Page>()
    private val pagePollingMutex = Mutex()

    suspend fun initialize() = withContext(Dispatchers.IO) {
        mutex.withLock {
            
            try {
                // Check Playwright initialization
                if (!::playwright.isInitialized) {
                    playwright = Playwright.create()
                }
                
                // Check browser initialization
                if (!::browser.isInitialized || browser.isConnected.not()) {
                    if (::browser.isInitialized) {
                        try { browser.close() } catch (e: Exception) { /* 이미 닫힌 경우 무시 */ }
                    }
                    browser = playwright.chromium().launch(
                        BrowserType.LaunchOptions()
                            .setHeadless(false)
                            .setArgs(listOf(
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
                            ))
                    )
                }
                
                // Check page initialization
                var needNewPage = false
                if (!::page.isInitialized) {
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
                    page = browser.newPage()
                    setupContributionRecording()
                    page.waitForLoadState()
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

    suspend fun cleanup() = withContext(Dispatchers.IO) {
        mutex.withLock {
            
            // Stop contribution mode
            stopContributionRecording()
            
            try {
                if (::page.isInitialized && !page.isClosed) {
                    page.close()
                }
            } catch (e: Exception) {
                Napier.w("Failed to close page: ${e.message}", tag = Tags.BROWSER_AUTOMATION)
            }
            
            try {
                if (::browser.isInitialized && browser.isConnected) {
                    browser.close()
                }
            } catch (e: Exception) {
                Napier.w("Failed to close browser: ${e.message}", tag = Tags.BROWSER_AUTOMATION)
            }
            
            try {
                if (::playwright.isInitialized) {
                    playwright.close()
                }
            } catch (e: Exception) {
                Napier.w("Failed to close playwright: ${e.message}", tag = Tags.BROWSER_AUTOMATION)
            }
            
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
                recordContributionStep(url, page.title(), "navigate", null, null)
            } catch (e: PlaywrightException) {
                Napier.e("Navigation failed to $url: ${e.message}", e, tag = Tags.BROWSER_AUTOMATION)
            } catch (e: Exception) {
                Napier.e("Navigation error: ${e.message}", e, tag = Tags.BROWSER_AUTOMATION)
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
            } catch (e: PlaywrightException) {
                Napier.e("Go back failed: ${e.message}", e, tag = Tags.BROWSER_AUTOMATION)
            } catch (e: Exception) {
                Napier.e("Go back error: ${e.message}", e, tag = Tags.BROWSER_AUTOMATION)
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
            } catch (e: PlaywrightException) {
                Napier.e("Go forward failed: ${e.message}", e, tag = Tags.BROWSER_AUTOMATION)
            } catch (e: Exception) {
                Napier.e("Go forward error: ${e.message}", e, tag = Tags.BROWSER_AUTOMATION)
            }
        }
    }

    suspend fun waitForNetworkIdle(timeout: Double = 10000.0) = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!::page.isInitialized) {
                Napier.e("Cannot waitForNetworkIdle: page not initialized", tag = Tags.BROWSER_AUTOMATION)
                return@withContext
            }
            try {
                Napier.d("Waiting for network idle state with ${timeout}ms timeout...", tag = Tags.BROWSER_AUTOMATION)
                page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE, Page.WaitForLoadStateOptions().setTimeout(timeout))
                Napier.i("Network is idle.", tag = Tags.BROWSER_AUTOMATION)
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
                element.click()
            }
            Napier.d("Click action took ${clickTime}ms", tag = Tags.BROWSER_AUTOMATION)

            recordContributionStep(
                page.url(),
                page.title(),
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
                        locator.first().pressSequentially(text, Locator.PressSequentiallyOptions().setDelay(delayMs))
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
    
    // Contribution recording functions
    fun setContributionRecordingCallback(callback: (ContributionStep) -> Unit) {
        contributionRecordingCallback = callback
    }
    
    suspend fun startContributionRecording() {
        Napier.i("Starting contribution recording...", tag = Tags.BROWSER_AUTOMATION)

        try {
            initialize()
            
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
        
        // Detect new tab opening and start tracking
        page.onPopup { newPage ->
            Napier.i("New tab detected: ${newPage.url()}", tag = Tags.BROWSER_AUTOMATION)
            recordContributionStep(
                newPage.url(),
                newPage.title(),
                "new_tab",
                null,
                mapOf("from_url" to page.url())
            )
            
            // 새 탭도 추적 시작
            setupNewPageTracking(newPage)
        }
        
        // Detect page navigation (URL change)
        page.onFrameNavigated { frame ->
            if (frame == page.mainFrame()) {
                Napier.i("Frame navigated to: ${frame.url()}", tag = Tags.BROWSER_AUTOMATION)
                recordContributionStep(
                    frame.url(),
                    frame.page().title(),
                    "navigate",
                    null,
                    null
                )
            }
        }
        
        // Inject user interaction listeners when page load completes
        page.onLoad { 
            try {
                Napier.i("Page loaded, injecting listeners for: ${page.url()}", tag = Tags.BROWSER_AUTOMATION)
                injectUserInteractionListeners()
            } catch (e: Exception) {
                Napier.w("Failed to inject user interaction listeners: ${e.message}", tag = Tags.BROWSER_AUTOMATION)
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
            trackedPages.add(newPage)
            pageTimestamps[newPage] = Pair(0L, 0L)
            updatePageActivity(newPage)
            
            // 새 페이지 로드 시 리스너 주입
            newPage.onLoad {
                try {
                    Napier.i("New page loaded, injecting listeners for: ${newPage.url()}", tag = Tags.BROWSER_AUTOMATION)
                    injectUserInteractionListeners(newPage)
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
    
    private fun injectUserInteractionListeners(targetPage: Page = page) {
        if (!isRecordingContributions) return
        
        targetPage.evaluate("""
            (function() {
                // 이미 리스너가 설정되어 있으면 중복 설정 방지
                if (window.__vowserContributionListenersSetup) return;
                window.__vowserContributionListenersSetup = true;
                
                console.log('Vowser contribution listeners injected');
                
                // Click event detection
                document.addEventListener('click', function(event) {
                    const element = event.target;
                    const selector = generateSelector(element);
                    const attributes = {
                        'text': element.textContent?.trim() || '',
                        'tag': element.tagName?.toLowerCase() || '',
                        'id': element.id || '',
                        'class': element.className || '',
                        'href': element.href || '',
                        'type': element.type || ''
                    };
                    
                    window.__vowserLastClick = {
                        selector: selector,
                        attributes: attributes,
                        timestamp: Date.now()
                    };
                    
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
                
                // 셀렉터 생성 함수
                function generateSelector(element) {
                    if (element.id) {
                        return '#' + element.id;
                    }
                    
                    let selector = element.tagName.toLowerCase();
                    
                    if (element.className) {
                        const classes = element.className.split(' ').filter(c => c);
                        if (classes.length > 0) {
                            selector += '.' + classes.join('.');
                        }
                    }
                    
                    const parent = element.parentElement;
                    if (parent && parent !== document.body) {
                        const parentSelector = parent.id ? '#' + parent.id : parent.tagName.toLowerCase();
                        selector = parentSelector + ' > ' + selector;
                    }
                    
                    return selector;
                }
            })();
        """)
        
        Napier.i("User interaction listeners injected", tag = Tags.BROWSER_AUTOMATION)
    }
    
    private fun startUserInteractionPolling() {
        pollingJob?.cancel()
        pollingJob = CoroutineScope(Dispatchers.IO).launch {
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
    
    private fun cleanupPage(targetPage: Page) {
        trackedPages.remove(targetPage)
        pageTimestamps.remove(targetPage)
        pageLastActivity.remove(targetPage)
    }
    
    private suspend fun checkPageInteractions(targetPage: Page) {
        try {
            // 리스너 상태 체크
            val listenersSetup = targetPage.evaluate("window.__vowserContributionListenersSetup")
            if (listenersSetup != true) {
                Napier.w("Listeners not setup for ${targetPage.url()}, re-injecting...", tag = Tags.BROWSER_AUTOMATION)
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
    
    private fun extractElementAttributes(locator: Locator): Map<String, String> {
        return try {
            val element = locator.first()
            val attributes = mutableMapOf<String, String>()
            
            // 텍스트 내용 추출
            element.textContent()?.let { text ->
                if (text.isNotBlank()) attributes["text"] = text.trim()
            }
            
            // 주요 속성들 추출
            listOf("id", "class", "name", "type", "href", "alt", "title", "aria-label").forEach { attr ->
                try {
                    element.getAttribute(attr)?.let { value ->
                        if (value.isNotBlank()) attributes[attr] = value
                    }
                } catch (e: Exception) {
                    // 속성이 없는 경우 무시
                }
            }
            
            attributes
        } catch (e: Exception) {
            Napier.w("Failed to extract element attributes: ${e.message}", tag = Tags.BROWSER_AUTOMATION)
            emptyMap()
        }
    }
    
    private fun startMemoryCleanupJob() {
        memoryCleanupJob = CoroutineScope(Dispatchers.IO).launch {
            while (isRecordingContributions) {
                try {
                    delay(ContributionConstants.MEMORY_CLEANUP_INTERVAL_MS)
                    performMemoryCleanup()
                } catch (e: Exception) {
                    Napier.w("Memory cleanup error: ${e.message}", tag = Tags.BROWSER_AUTOMATION)
                }
            }
        }
        Napier.i("Started memory cleanup job", tag = Tags.BROWSER_AUTOMATION)
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