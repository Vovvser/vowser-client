package com.vowserclient.shared.browserautomation

import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
import com.microsoft.playwright.PlaywrightException
import com.vowser.client.contribution.ContributionStep
import com.vowser.client.contribution.ContributionConstants
import com.vowser.client.logging.VowserLogger
import com.vowser.client.logging.Tags
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

object BrowserAutomationService {

    private lateinit var playwright: Playwright
    private lateinit var browser: Browser
    private lateinit var page: Page
    private val mutex = Mutex()
    
    // 메모리 관리는 ContributionConstants 사용
    
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
            VowserLogger.info("BrowserAutomationService: Starting initialization...", Tags.BROWSER_AUTOMATION)
            
            try {
                // Playwright 초기화 확인
                if (!::playwright.isInitialized) {
                    VowserLogger.info("BrowserAutomationService: Initializing Playwright...", Tags.BROWSER_AUTOMATION)
                    playwright = Playwright.create()
                }
                
                // Browser 초기화 체크
                if (!::browser.isInitialized || browser.isConnected.not()) {
                    VowserLogger.info("BrowserAutomationService: Launching browser...", Tags.BROWSER_AUTOMATION)
                    if (::browser.isInitialized) {
                        try { browser.close() } catch (e: Exception) { /* 이미 닫힌 경우 무시 */ }
                    }
                    browser = playwright.chromium().launch(
                        BrowserType.LaunchOptions()
                            .setHeadless(false)
                            .setChannel("chrome")
                    )
                }
                
                // Page 초기화 체크
                var needNewPage = false
                if (!::page.isInitialized) {
                    needNewPage = true
                    VowserLogger.info("BrowserAutomationService: Page not initialized", Tags.BROWSER_AUTOMATION)
                } else {
                    try {
                        // 페이지 상태 체크
                        val isClosed = page.isClosed
                        if (isClosed) {
                            needNewPage = true
                            VowserLogger.info("BrowserAutomationService: Page is closed", Tags.BROWSER_AUTOMATION)
                        } else {
                            // 페이지가 살아있는지 추가 확인
                            page.title() // 접근 가능성 테스트
                            VowserLogger.info("BrowserAutomationService: Existing page is active", Tags.BROWSER_AUTOMATION)
                        }
                    } catch (e: Exception) {
                        needNewPage = true
                        VowserLogger.warn("BrowserAutomationService: Page status check failed, creating new page: ${e.message}", Tags.BROWSER_AUTOMATION)
                    }
                }
                
                if (needNewPage) {
                    VowserLogger.info("BrowserAutomationService: Creating new page...", Tags.BROWSER_AUTOMATION)
                    page = browser.newPage()
                    setupContributionRecording()
                    page.waitForLoadState()
                    VowserLogger.info("BrowserAutomationService: New page created and ready", Tags.BROWSER_AUTOMATION)
                }
                
                VowserLogger.info("BrowserAutomationService: Initialization completed successfully", Tags.BROWSER_AUTOMATION)
                
            } catch (e: Exception) {
                VowserLogger.error("BrowserAutomationService: Critical initialization failure: ${e.message}", Tags.BROWSER_AUTOMATION)
                // 전체 정리 및 재시작
                try {
                    if (::browser.isInitialized) browser.close()
                    if (::playwright.isInitialized) playwright.close()
                } catch (cleanupError: Exception) {
                    VowserLogger.warn("Cleanup error: ${cleanupError.message}", Tags.BROWSER_AUTOMATION)
                }
                throw e
            }
        }
    }

    suspend fun cleanup() = withContext(Dispatchers.IO) {
        mutex.withLock {
            VowserLogger.info("BrowserAutomationService: Starting cleanup...", Tags.BROWSER_AUTOMATION)
            
            // 기여모드 중지
            stopContributionRecording()
            
            try {
                if (::page.isInitialized && !page.isClosed) {
                    page.close()
                }
            } catch (e: Exception) {
                VowserLogger.warn("Failed to close page: ${e.message}", Tags.BROWSER_AUTOMATION)
            }
            
            try {
                if (::browser.isInitialized && browser.isConnected) {
                    browser.close()
                }
            } catch (e: Exception) {
                VowserLogger.warn("Failed to close browser: ${e.message}", Tags.BROWSER_AUTOMATION)
            }
            
            try {
                if (::playwright.isInitialized) {
                    playwright.close()
                }
            } catch (e: Exception) {
                VowserLogger.warn("Failed to close playwright: ${e.message}", Tags.BROWSER_AUTOMATION)
            }
            
            VowserLogger.info("BrowserAutomationService: Cleanup completed", Tags.BROWSER_AUTOMATION)
        }
    }

    suspend fun navigate(url: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!::page.isInitialized) {
                VowserLogger.error("BrowserAutomationService: Page is not initialized. Cannot navigate to $url", Tags.BROWSER_AUTOMATION)
                return@withContext
            }
            VowserLogger.info("BrowserAutomationService: Navigating to $url", Tags.BROWSER_AUTOMATION)
            try {
                page.navigate(url)
                AdaptiveWaitManager.waitForPageLoad(page, "navigation to $url")
                recordContributionStep(url, page.title(), "navigate", null, null)
                VowserLogger.info("BrowserAutomationService: Navigation to $url completed.", Tags.BROWSER_AUTOMATION)
            } catch (e: PlaywrightException) {
                VowserLogger.error("BrowserAutomationService: Navigation failed to $url: ${e.message}", Tags.BROWSER_AUTOMATION)
            } catch (e: Exception) {
                VowserLogger.error("BrowserAutomationService: Unexpected error during navigation to $url: ${e.message}", Tags.BROWSER_AUTOMATION)
            }
        }
    }

    suspend fun goBack() = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!::page.isInitialized) {
                VowserLogger.error("BrowserAutomationService: Page is not initialized. Cannot go back.", Tags.BROWSER_AUTOMATION)
                return@withContext
            }
            VowserLogger.info("BrowserAutomationService: Going back", Tags.BROWSER_AUTOMATION)
            try {
                val response = page.goBack()
                if (response == null) {
                    VowserLogger.warn("BrowserAutomationService: No previous page to go back to.", Tags.BROWSER_AUTOMATION)
                } else {
                    AdaptiveWaitManager.waitForPageLoad(page, "go back")
                    VowserLogger.info("BrowserAutomationService: Go back completed.", Tags.BROWSER_AUTOMATION)
                }
            } catch (e: PlaywrightException) {
                VowserLogger.error("BrowserAutomationService: Go back failed: ${e.message}", Tags.BROWSER_AUTOMATION)
            } catch (e: Exception) {
                VowserLogger.error("BrowserAutomationService: Unexpected error during go back: ${e.message}", Tags.BROWSER_AUTOMATION)
            }
        }
    }

    suspend fun goForward() = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!::page.isInitialized) {
                VowserLogger.error("BrowserAutomationService: Page is not initialized. Cannot go forward.", Tags.BROWSER_AUTOMATION)
                return@withContext
            }
            VowserLogger.info("BrowserAutomationService: Going forward", Tags.BROWSER_AUTOMATION)
            try {
                val response = page.goForward()
                if (response == null) {
                    VowserLogger.warn("BrowserAutomationService: No next page to go forward to.", Tags.BROWSER_AUTOMATION)
                } else {
                    AdaptiveWaitManager.waitForPageLoad(page, "go forward")
                    VowserLogger.info("BrowserAutomationService: Go forward completed.", Tags.BROWSER_AUTOMATION)
                }
            } catch (e: PlaywrightException) {
                VowserLogger.error("BrowserAutomationService: Go forward failed: ${e.message}", Tags.BROWSER_AUTOMATION)
            } catch (e: Exception) {
                VowserLogger.error("BrowserAutomationService: Unexpected error during go forward: ${e.message}", Tags.BROWSER_AUTOMATION)
            }
        }
    }


    suspend fun hoverAndClickElement(selector: String): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!::page.isInitialized) {
                VowserLogger.error("BrowserAutomationService: Page is not initialized. Cannot hover hoverAndClickElement element: $selector", Tags.BROWSER_AUTOMATION)
                return@withContext false
            }
            try {
                val locator = page.locator(selector)
                
                // 먼저 요소 존재 여부를 빠르게 확인
                if (locator.count() == 0) {
                    VowserLogger.warn("Element with selector $selector not found on page, trying alternatives.", Tags.BROWSER_AUTOMATION)
                    return@withContext tryAlternativeSelectors(selector)
                }
                
                if (!AdaptiveWaitManager.waitForElement(locator, page, "element with selector: $selector")) {
                    VowserLogger.warn("Element with selector $selector not found or not visible after adaptive waiting.", Tags.BROWSER_AUTOMATION)
                    return@withContext tryAlternativeSelectors(selector)
                }
                
                return@withContext executeHoverHighlightClick(locator, selector)
            } catch (e: PlaywrightException) {
                VowserLogger.error("Failed to hoverAndClickElement $selector: ${e.message}", Tags.BROWSER_AUTOMATION)
                return@withContext tryAlternativeSelectors(selector)
            } catch (e: Exception) {
                VowserLogger.error("Unexpected error hoverAndClickElement $selector: ${e.message}", Tags.BROWSER_AUTOMATION)
                return@withContext false
            }
        }
    }
    
    private suspend fun tryAlternativeSelectors(originalSelector: String): Boolean = withContext(Dispatchers.IO) {
        val alternativeSelectors = when (originalSelector) {
            "a[href='/webtoon?tab=mon']" -> listOf(
                "a[href*='tab=mon']",
                ".tab_list a:contains('월요웹툰')",
                "[data-tab='mon']",
                ".tab_mon"
            )
            "ul.img_list li:first-child a" -> listOf(
                ".img_list li:first-child a",
                ".thumb_area:first-child a",
                ".daily_img:first-child a"
            )
            // YouTube 검색창 alternative selectors
            "input#search" -> listOf(
                "input[name='search_query']",
                "#search input",
                "[aria-label*='검색']",
                "[aria-label*='Search']",
                "#search-input input",
                "input[type='text']"
            )
            // YouTube 필터 버튼 alternative selectors  
            "button[aria-label='검색 필터']" -> listOf(
                "button[aria-label*='필터']",
                "button[aria-label*='Filter']",
                ".filter-button",
                "#filter-menu button",
                "button[title*='필터']"
            )
            else -> emptyList()
        }
        
        if (alternativeSelectors.isEmpty()) {
            VowserLogger.warn("No alternative selectors available for: $originalSelector. Skipping this step.", Tags.BROWSER_AUTOMATION)
            return@withContext false
        }
        
        for (altSelector in alternativeSelectors) {
            try {
                VowserLogger.info("Trying alternative selector: $altSelector", Tags.BROWSER_AUTOMATION)
                val locator = page.locator(altSelector)
                
                // 빠른 존재 확인
                if (locator.count() == 0) {
                    VowserLogger.warn("Alternative selector $altSelector not found", Tags.BROWSER_AUTOMATION)
                    continue
                }
                
                if (AdaptiveWaitManager.waitForElement(locator, page, "alternative selector: $altSelector")) {
                    val success = executeHoverHighlightClick(locator, altSelector)
                    if (success) {
                        VowserLogger.info("Successfully clicked using alternative selector: $altSelector", Tags.BROWSER_AUTOMATION)
                        return@withContext true
                    }
                }
            } catch (e: Exception) {
                VowserLogger.warn("Alternative selector $altSelector also failed: ${e.message}", Tags.BROWSER_AUTOMATION)
            }
        }
        VowserLogger.warn("All alternative selectors failed for original selector: $originalSelector. Continuing anyway.", Tags.BROWSER_AUTOMATION)
        return@withContext false
    }

    private suspend fun executeHoverHighlightClick(locator: Locator, selector: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val element = locator.first()
            
            element.scrollIntoViewIfNeeded()
            element.hover()
            page.evaluate(HIGHLIGHT_SCRIPT_CONTENT, selector)
            VowserLogger.info("Applied highlight to element with selector: $selector", Tags.BROWSER_AUTOMATION)
            
            delay(3000)

            try {
                page.evaluate("""
                    (function() {
                        const selector = arguments[0];
                        const element = document.querySelector(selector);
                        if (element) {
                            element.removeAttribute('target');
                            return true;
                        }
                        return false;
                    })
                """.trimIndent(), selector)
                VowserLogger.info("Removed target attribute for selector: $selector", Tags.BROWSER_AUTOMATION)
            } catch (jsError: Exception) {
                VowserLogger.warn("Failed to remove target attribute: ${jsError.message}", Tags.BROWSER_AUTOMATION)
            }
            
            element.click()
            recordContributionStep(
                page.url(), 
                page.title(), 
                "click", 
                selector, 
                extractElementAttributes(element)
            )
            VowserLogger.info("Clicked element with selector: $selector", Tags.BROWSER_AUTOMATION)
            return@withContext true

        } catch (e: Exception) {
            VowserLogger.error("Failed to execute hover-highlight-click for $selector: ${e.message}", Tags.BROWSER_AUTOMATION)
            try {
                try {
                    page.evaluate("""
                        (function() {
                            const selector = arguments[0];
                            const element = document.querySelector(selector);
                            if (element) {
                                element.removeAttribute('target');
                                return true;
                            }
                            return false;
                        })
                    """.trimIndent(), selector)
                } catch (jsError: Exception) {
                    VowserLogger.warn("Failed to remove target attribute in fallback: ${jsError.message}", Tags.BROWSER_AUTOMATION)
                }
                
                locator.first().click()
                recordContributionStep(
                    page.url(), 
                    page.title(), 
                    "click", 
                    selector, 
                    extractElementAttributes(locator.first())
                )
                VowserLogger.info("Fallback click succeeded for selector: $selector", Tags.BROWSER_AUTOMATION)
                return@withContext true
            } catch (fallbackError: Exception) {
                VowserLogger.error("Fallback click also failed for $selector: ${fallbackError.message}", Tags.BROWSER_AUTOMATION)
                return@withContext false
            }
        }
    }


    suspend fun typeText(selector: String, text: String, delayMs: Double? = null) = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!::page.isInitialized) {
                VowserLogger.error("BrowserAutomationService: Page is not initialized. Cannot type text into element: $selector", Tags.BROWSER_AUTOMATION)
                return@withContext
            }
            try {
                val locator = page.locator(selector)
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
                    VowserLogger.info("Typed text '$text' into element with selector: $selector", Tags.BROWSER_AUTOMATION)
                } else {
                    VowserLogger.warn("Element with selector $selector not found or not visible for typing.", Tags.BROWSER_AUTOMATION)
                }
            } catch (e: PlaywrightException) {
                VowserLogger.error("Failed to type text into element $selector: ${e.message}", Tags.BROWSER_AUTOMATION)
            } catch (e: Exception) {
                VowserLogger.error("Unexpected error typing text into element $selector: ${e.message}", Tags.BROWSER_AUTOMATION)
            }
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
        indicator.textContent = '클릭 대상';
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
        VowserLogger.info("Starting contribution recording...", Tags.BROWSER_AUTOMATION)

        try {
            initialize()
            
            // 초기화 후 추가 확인
            if (!::page.isInitialized || page.isClosed) {
                throw Exception("Page initialization failed")
            }
            
            isRecordingContributions = true
            startUserInteractionPolling()
            startMemoryCleanupJob()
            
            // 리스너 주입 확인
            injectUserInteractionListeners()
            
            VowserLogger.info("Contribution recording started successfully", Tags.BROWSER_AUTOMATION)
            
        } catch (e: Exception) {
            VowserLogger.error("Failed to start contribution recording: ${e.message}", Tags.BROWSER_AUTOMATION)
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
        VowserLogger.info("Stopped contribution recording", Tags.BROWSER_AUTOMATION)
    }
    
    private fun setupContributionRecording() {
        if (!::page.isInitialized) return
        
        VowserLogger.info("Setting up contribution recording listeners", Tags.BROWSER_AUTOMATION)
        
        // 새 탭 열림 감지 및 추적 시작
        page.onPopup { newPage ->
            VowserLogger.info("New tab detected: ${newPage.url()}", Tags.BROWSER_AUTOMATION)
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
        
        // 페이지 네비게이션 감지 (URL 변경)
        page.onFrameNavigated { frame ->
            if (frame == page.mainFrame()) {
                VowserLogger.info("Frame navigated to: ${frame.url()}", Tags.BROWSER_AUTOMATION)
                recordContributionStep(
                    frame.url(),
                    frame.page().title(),
                    "navigate",
                    null,
                    null
                )
            }
        }
        
        // 페이지 로드 완료 시 사용자 상호작용 리스너 주입
        page.onLoad { 
            try {
                VowserLogger.info("Page loaded, injecting listeners for: ${page.url()}", Tags.BROWSER_AUTOMATION)
                injectUserInteractionListeners()
            } catch (e: Exception) {
                VowserLogger.warn("Failed to inject user interaction listeners: ${e.message}", Tags.BROWSER_AUTOMATION)
            }
        }
        
        // 콘솔 로그 리스너 (디버깅용)
        page.onConsoleMessage { message ->
            if (message.text().contains("Vowser") || message.text().contains("🖱️") || message.text().contains("⌨️")) {
                VowserLogger.info("Browser Console: ${message.text()}", Tags.BROWSER_AUTOMATION)
            }
        }
        
        // 페이지가 이미 로드된 경우 즉시 리스너 주입
        try {
            injectUserInteractionListeners()
            trackedPages.add(page)
            pageTimestamps[page] = Pair(0L, 0L)
            updatePageActivity(page)
        } catch (e: Exception) {
            VowserLogger.warn("Failed to inject initial user interaction listeners: ${e.message}", Tags.BROWSER_AUTOMATION)
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
                    VowserLogger.info("New page loaded, injecting listeners for: ${newPage.url()}", Tags.BROWSER_AUTOMATION)
                    injectUserInteractionListeners(newPage)
                } catch (e: Exception) {
                    VowserLogger.warn("Failed to inject listeners for new page: ${e.message}", Tags.BROWSER_AUTOMATION)
                }
            }
            
            // 새 페이지의 콘솔 로그 리스너
            newPage.onConsoleMessage { message ->
                if (message.text().contains("Vowser") || message.text().contains("🖱️") || message.text().contains("⌨️")) {
                    VowserLogger.info("New Tab Console: ${message.text()}", Tags.BROWSER_AUTOMATION)
                }
            }
            
            // 새 페이지에 즉시 리스너 주입 시도
            try {
                injectUserInteractionListeners(newPage)
            } catch (e: Exception) {
                VowserLogger.warn("Failed to inject initial listeners for new page: ${e.message}", Tags.BROWSER_AUTOMATION)
            }
            
        } catch (e: Exception) {
            VowserLogger.error("Failed to setup new page tracking: ${e.message}", Tags.BROWSER_AUTOMATION)
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
                
                // 클릭 이벤트 감지
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
        
        VowserLogger.info("User interaction listeners injected", Tags.BROWSER_AUTOMATION)
    }
    
    private fun startUserInteractionPolling() {
        pollingJob?.cancel()
        pollingJob = CoroutineScope(Dispatchers.IO).launch {
            VowserLogger.info("Starting user interaction polling", Tags.BROWSER_AUTOMATION)
            while (isRecordingContributions && ::page.isInitialized) {
                try {
                    checkForUserInteractions()
                    delay(ContributionConstants.POLLING_INTERVAL_MS)
                } catch (e: Exception) {
                    VowserLogger.warn("Error polling user interactions: ${e.message}", Tags.BROWSER_AUTOMATION)
                    delay(2000) // 에러 시 2초 대기
                }
            }
            VowserLogger.info("User interaction polling stopped", Tags.BROWSER_AUTOMATION)
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
                    VowserLogger.warn("Error checking interactions for page: ${e.message}", Tags.BROWSER_AUTOMATION)
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
        VowserLogger.debug("Cleaned up page: ${try { targetPage.url() } catch (e: Exception) { "unknown" }}", Tags.BROWSER_AUTOMATION)
    }
    
    private suspend fun checkPageInteractions(targetPage: Page) {
        try {
            // 리스너 상태 체크
            val listenersSetup = targetPage.evaluate("window.__vowserContributionListenersSetup")
            if (listenersSetup != true) {
                VowserLogger.warn("Listeners not setup for ${targetPage.url()}, re-injecting...", Tags.BROWSER_AUTOMATION)
                injectUserInteractionListeners(targetPage)
                return
            }
            
            // 클릭 이벤트 체크
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
                    VowserLogger.info("Button clicked: [$buttonText] on $selector (${targetPage.url()})", Tags.BROWSER_AUTOMATION)
                    
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
                    
                    // 처리된 클릭 데이터 클리어
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
                    VowserLogger.info("Enter key pressed: [$inputText] on $selector (${targetPage.url()})", Tags.BROWSER_AUTOMATION)
                    
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
            VowserLogger.warn("Error checking interactions for page: ${e.message}", Tags.BROWSER_AUTOMATION)
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
        VowserLogger.info("Contribution Step Recorded: [${step.action}] ${step.title} (${step.url})", Tags.BROWSER_AUTOMATION)
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
            VowserLogger.warn("Failed to extract element attributes: ${e.message}", Tags.BROWSER_AUTOMATION)
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
                    VowserLogger.warn("Memory cleanup error: ${e.message}", Tags.BROWSER_AUTOMATION)
                }
            }
        }
        VowserLogger.info("Started memory cleanup job", Tags.BROWSER_AUTOMATION)
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
                        // 페이지 접근 실패 시 정리 대상에 추가
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
                VowserLogger.info("Cleaned up ${inactivePages.size} inactive pages. Remaining: ${trackedPages.size}", Tags.BROWSER_AUTOMATION)
            }
        }
    }
    
    private fun updatePageActivity(page: Page) {
        pageLastActivity[page] = System.currentTimeMillis()
    }
}