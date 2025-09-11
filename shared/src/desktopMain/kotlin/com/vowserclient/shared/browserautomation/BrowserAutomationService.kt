package com.vowserclient.shared.browserautomation

import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
import com.microsoft.playwright.PlaywrightException
import com.vowser.client.contribution.ContributionStep
import com.vowser.client.contribution.ContributionConstants
import io.github.aakira.napier.Napier
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
            Napier.i("BrowserAutomationService: Starting initialization...", tag = "BrowserAutomationService")
            
            try {
                // Playwright 초기화 확인
                if (!::playwright.isInitialized) {
                    Napier.i("BrowserAutomationService: Initializing Playwright...", tag = "BrowserAutomationService")
                    playwright = Playwright.create()
                }
                
                // Browser 초기화 체크
                if (!::browser.isInitialized || browser.isConnected.not()) {
                    Napier.i("BrowserAutomationService: Launching browser...", tag = "BrowserAutomationService")
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
                    Napier.i("BrowserAutomationService: Page not initialized", tag = "BrowserAutomationService")
                } else {
                    try {
                        // 페이지 상태 체크
                        val isClosed = page.isClosed
                        if (isClosed) {
                            needNewPage = true
                            Napier.i("BrowserAutomationService: Page is closed", tag = "BrowserAutomationService")
                        } else {
                            // 페이지가 살아있는지 추가 확인
                            page.title() // 접근 가능성 테스트
                            Napier.i("BrowserAutomationService: Existing page is active", tag = "BrowserAutomationService")
                        }
                    } catch (e: Exception) {
                        needNewPage = true
                        Napier.w("BrowserAutomationService: Page status check failed, creating new page: ${e.message}", tag = "BrowserAutomationService")
                    }
                }
                
                if (needNewPage) {
                    Napier.i("BrowserAutomationService: Creating new page...", tag = "BrowserAutomationService")
                    page = browser.newPage()
                    setupContributionRecording()
                    page.waitForLoadState()
                    Napier.i("BrowserAutomationService: New page created and ready", tag = "BrowserAutomationService")
                }
                
                Napier.i("BrowserAutomationService: Initialization completed successfully", tag = "BrowserAutomationService")
                
            } catch (e: Exception) {
                Napier.e("BrowserAutomationService: Critical initialization failure: ${e.message}", e, tag = "BrowserAutomationService")
                // 전체 정리 및 재시작
                try {
                    if (::browser.isInitialized) browser.close()
                    if (::playwright.isInitialized) playwright.close()
                } catch (cleanupError: Exception) {
                    Napier.w("Cleanup error: ${cleanupError.message}", tag = "BrowserAutomationService")
                }
                throw e
            }
        }
    }

    suspend fun cleanup() = withContext(Dispatchers.IO) {
        mutex.withLock {
            Napier.i("BrowserAutomationService: Starting cleanup...", tag = "BrowserAutomationService")
            
            // 기여모드 중지
            stopContributionRecording()
            
            try {
                if (::page.isInitialized && !page.isClosed) {
                    page.close()
                }
            } catch (e: Exception) {
                Napier.w("Failed to close page: ${e.message}", tag = "BrowserAutomationService")
            }
            
            try {
                if (::browser.isInitialized && browser.isConnected) {
                    browser.close()
                }
            } catch (e: Exception) {
                Napier.w("Failed to close browser: ${e.message}", tag = "BrowserAutomationService")
            }
            
            try {
                if (::playwright.isInitialized) {
                    playwright.close()
                }
            } catch (e: Exception) {
                Napier.w("Failed to close playwright: ${e.message}", tag = "BrowserAutomationService")
            }
            
            Napier.i("BrowserAutomationService: Cleanup completed", tag = "BrowserAutomationService")
        }
    }

    suspend fun navigate(url: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!::page.isInitialized) {
                Napier.e("BrowserAutomationService: Page is not initialized. Cannot navigate to $url", tag = "BrowserAutomationService")
                return@withContext
            }
            Napier.i { "BrowserAutomationService: Navigating to $url" }
            try {
                page.navigate(url)
                recordContributionStep(url, page.title(), "navigate", null, null)
                Napier.i { "BrowserAutomationService: Navigation to $url completed." }
            } catch (e: PlaywrightException) {
                Napier.e("BrowserAutomationService: Navigation failed to $url: ${e.message}", e, tag = "BrowserAutomationService")
            } catch (e: Exception) {
                Napier.e("BrowserAutomationService: Unexpected error during navigation to $url: ${e.message}", e, tag = "BrowserAutomationService")
            }
        }
    }

    suspend fun goBack() = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!::page.isInitialized) {
                Napier.e("BrowserAutomationService: Page is not initialized. Cannot go back.", tag = "BrowserAutomationService")
                return@withContext
            }
            Napier.i { "BrowserAutomationService: Going back" }
            try {
                val response = page.goBack()
                if (response == null) {
                    Napier.w { "BrowserAutomationService: No previous page to go back to." }
                } else {
                    page.waitForLoadState()
                    Napier.i { "BrowserAutomationService: Go back completed." }
                }
            } catch (e: PlaywrightException) {
                Napier.e("BrowserAutomationService: Go back failed: ${e.message}", e, tag = "BrowserAutomationService")
            } catch (e: Exception) {
                Napier.e("BrowserAutomationService: Unexpected error during go back: ${e.message}", e, tag = "BrowserAutomationService")
            }
        }
    }

    suspend fun goForward() = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!::page.isInitialized) {
                Napier.e("BrowserAutomationService: Page is not initialized. Cannot go forward.", tag = "BrowserAutomationService")
                return@withContext
            }
            Napier.i { "BrowserAutomationService: Going forward" }
            try {
                val response = page.goForward()
                if (response == null) {
                    Napier.w { "BrowserAutomationService: No next page to go forward to." }
                } else {
                    page.waitForLoadState()
                    Napier.i { "BrowserAutomationService: Go forward completed." }
                }
            } catch (e: PlaywrightException) {
                Napier.e("BrowserAutomationService: Go forward failed: ${e.message}", e, tag = "BrowserAutomationService")
            } catch (e: Exception) {
                Napier.e("BrowserAutomationService: Unexpected error during go forward: ${e.message}", e, tag = "BrowserAutomationService")
            }
        }
    }


    suspend fun hoverAndClickElement(selector: String): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!::page.isInitialized) {
                Napier.e("BrowserAutomationService: Page is not initialized. Cannot hover hoverAndClickElement element: $selector", tag = "BrowserAutomationService")
                return@withContext false
            }
            try {
                val locator = page.locator(selector)
                
                // 먼저 요소 존재 여부를 빠르게 확인
                if (locator.count() == 0) {
                    Napier.w { "Element with selector $selector not found on page, trying alternatives." }
                    return@withContext tryAlternativeSelectors(selector)
                }
                
                locator.first().waitFor(Locator.WaitForOptions().setTimeout(3000.0))
                
                if (locator.count() > 0 && locator.first().isVisible) {
                    return@withContext executeHoverHighlightClick(locator, selector)
                } else {
                    Napier.w { "Element with selector $selector not found or not visible after waiting." }
                    return@withContext tryAlternativeSelectors(selector)
                }
            } catch (e: PlaywrightException) {
                Napier.e("Failed to hoverAndClickElement $selector: ${e.message}", e, tag = "BrowserAutomationService")
                return@withContext tryAlternativeSelectors(selector)
            } catch (e: Exception) {
                Napier.e("Unexpected error hoverAndClickElement $selector: ${e.message}", e, tag = "BrowserAutomationService")
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
            Napier.w { "No alternative selectors available for: $originalSelector. Skipping this step." }
            return@withContext false
        }
        
        for (altSelector in alternativeSelectors) {
            try {
                Napier.i { "Trying alternative selector: $altSelector" }
                val locator = page.locator(altSelector)
                
                // 빠른 존재 확인
                if (locator.count() == 0) {
                    Napier.w { "Alternative selector $altSelector not found" }
                    continue
                }
                
                locator.first().waitFor(Locator.WaitForOptions().setTimeout(2000.0))
                
                if (locator.count() > 0 && locator.first().isVisible) {
                    val success = executeHoverHighlightClick(locator, altSelector)
                    if (success) {
                        Napier.i { "Successfully clicked using alternative selector: $altSelector" }
                        return@withContext true
                    }
                }
            } catch (e: Exception) {
                Napier.w { "Alternative selector $altSelector also failed: ${e.message}" }
            }
        }
        Napier.w { "All alternative selectors failed for original selector: $originalSelector. Continuing anyway." }
        return@withContext false
    }

    private suspend fun executeHoverHighlightClick(locator: Locator, selector: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val element = locator.first()
            
            element.scrollIntoViewIfNeeded()
            element.hover()
            page.evaluate(HIGHLIGHT_SCRIPT_CONTENT, selector)
            Napier.i { "Applied highlight to element with selector: $selector" }
            
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
                Napier.i { "Removed target attribute for selector: $selector" }
            } catch (jsError: Exception) {
                Napier.w { "Failed to remove target attribute: ${jsError.message}" }
            }
            
            element.click()
            recordContributionStep(
                page.url(), 
                page.title(), 
                "click", 
                selector, 
                extractElementAttributes(element)
            )
            Napier.i { "Clicked element with selector: $selector" }
            return@withContext true

        } catch (e: Exception) {
            Napier.e("Failed to execute hover-highlight-click for $selector: ${e.message}", e, tag = "BrowserAutomationService")
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
                    Napier.w { "Failed to remove target attribute in fallback: ${jsError.message}" }
                }
                
                locator.first().click()
                recordContributionStep(
                    page.url(), 
                    page.title(), 
                    "click", 
                    selector, 
                    extractElementAttributes(locator.first())
                )
                Napier.i { "Fallback click succeeded for selector: $selector" }
                return@withContext true
            } catch (fallbackError: Exception) {
                Napier.e("Fallback click also failed for $selector: ${fallbackError.message}", fallbackError, tag = "BrowserAutomationService")
                return@withContext false
            }
        }
    }


    suspend fun typeText(selector: String, text: String, delayMs: Double? = null) = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!::page.isInitialized) {
                Napier.e("BrowserAutomationService: Page is not initialized. Cannot type text into element: $selector", tag = "BrowserAutomationService")
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
                    Napier.i { "Typed text '$text' into element with selector: $selector" }
                } else {
                    Napier.w { "Element with selector $selector not found or not visible for typing." }
                }
            } catch (e: PlaywrightException) {
                Napier.e("Failed to type text into element $selector: ${e.message}", e, tag = "BrowserAutomationService")
            } catch (e: Exception) {
                Napier.e("Unexpected error typing text into element $selector: ${e.message}", e, tag = "BrowserAutomationService")
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
        Napier.i("Starting contribution recording...", tag = "BrowserAutomationService")

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
            
            Napier.i("Contribution recording started successfully", tag = "BrowserAutomationService")
            
        } catch (e: Exception) {
            Napier.e("Failed to start contribution recording: ${e.message}", e, tag = "BrowserAutomationService")
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
        Napier.i("Stopped contribution recording", tag = "BrowserAutomationService")
    }
    
    private fun setupContributionRecording() {
        if (!::page.isInitialized) return
        
        Napier.i("Setting up contribution recording listeners", tag = "BrowserAutomationService")
        
        // 새 탭 열림 감지 및 추적 시작
        page.onPopup { newPage ->
            Napier.i("New tab detected: ${newPage.url()}", tag = "BrowserAutomationService")
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
                Napier.i("Frame navigated to: ${frame.url()}", tag = "BrowserAutomationService")
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
                Napier.i("Page loaded, injecting listeners for: ${page.url()}", tag = "BrowserAutomationService")
                injectUserInteractionListeners()
            } catch (e: Exception) {
                Napier.w("Failed to inject user interaction listeners: ${e.message}", tag = "BrowserAutomationService")
            }
        }
        
        // 콘솔 로그 리스너 (디버깅용)
        page.onConsoleMessage { message ->
            if (message.text().contains("Vowser") || message.text().contains("🖱️") || message.text().contains("⌨️")) {
                Napier.i("Browser Console: ${message.text()}", tag = "BrowserConsole")
            }
        }
        
        // 페이지가 이미 로드된 경우 즉시 리스너 주입
        try {
            injectUserInteractionListeners()
            trackedPages.add(page)
            pageTimestamps[page] = Pair(0L, 0L)
            updatePageActivity(page)
        } catch (e: Exception) {
            Napier.w("Failed to inject initial user interaction listeners: ${e.message}", tag = "BrowserAutomationService")
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
                    Napier.i("New page loaded, injecting listeners for: ${newPage.url()}", tag = "BrowserAutomationService")
                    injectUserInteractionListeners(newPage)
                } catch (e: Exception) {
                    Napier.w("Failed to inject listeners for new page: ${e.message}", tag = "BrowserAutomationService")
                }
            }
            
            // 새 페이지의 콘솔 로그 리스너
            newPage.onConsoleMessage { message ->
                if (message.text().contains("Vowser") || message.text().contains("🖱️") || message.text().contains("⌨️")) {
                    Napier.i("New Tab Console: ${message.text()}", tag = "BrowserConsole")
                }
            }
            
            // 새 페이지에 즉시 리스너 주입 시도
            try {
                injectUserInteractionListeners(newPage)
            } catch (e: Exception) {
                Napier.w("Failed to inject initial listeners for new page: ${e.message}", tag = "BrowserAutomationService")
            }
            
        } catch (e: Exception) {
            Napier.e("Failed to setup new page tracking: ${e.message}", e, tag = "BrowserAutomationService")
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
        
        Napier.i("User interaction listeners injected", tag = "BrowserAutomationService")
    }
    
    private fun startUserInteractionPolling() {
        pollingJob?.cancel()
        pollingJob = CoroutineScope(Dispatchers.IO).launch {
            Napier.i("Starting user interaction polling", tag = "BrowserAutomationService")
            while (isRecordingContributions && ::page.isInitialized) {
                try {
                    checkForUserInteractions()
                    delay(ContributionConstants.POLLING_INTERVAL_MS)
                } catch (e: Exception) {
                    Napier.w("Error polling user interactions: ${e.message}", tag = "BrowserAutomationService")
                    delay(2000) // 에러 시 2초 대기
                }
            }
            Napier.i("User interaction polling stopped", tag = "BrowserAutomationService")
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
                    Napier.w("Error checking interactions for page: ${e.message}", tag = "BrowserAutomationService")
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
        Napier.d("Cleaned up page: ${try { targetPage.url() } catch (e: Exception) { "unknown" }}", tag = "BrowserAutomationService")
    }
    
    private suspend fun checkPageInteractions(targetPage: Page) {
        try {
            // 리스너 상태 체크
            val listenersSetup = targetPage.evaluate("window.__vowserContributionListenersSetup")
            if (listenersSetup != true) {
                Napier.w("Listeners not setup for ${targetPage.url()}, re-injecting...", tag = "BrowserAutomationService")
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
                    Napier.i("Button clicked: [$buttonText] on $selector (${targetPage.url()})", tag = "BrowserAutomationService")
                    
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
                    Napier.i("Enter key pressed: [$inputText] on $selector (${targetPage.url()})", tag = "BrowserAutomationService")
                    
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
            Napier.w("Error checking interactions for page: ${e.message}", tag = "BrowserAutomationService")
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
        Napier.i("Contribution Step Recorded: [${step.action}] ${step.title} (${step.url})", tag = "ContributionRecording")
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
            Napier.w("Failed to extract element attributes: ${e.message}", tag = "BrowserAutomationService")
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
                    Napier.w("Memory cleanup error: ${e.message}", tag = "BrowserAutomationService")
                }
            }
        }
        Napier.i("Started memory cleanup job", tag = "BrowserAutomationService")
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
                Napier.i("Cleaned up ${inactivePages.size} inactive pages. Remaining: ${trackedPages.size}", tag = "BrowserAutomationService")
            }
        }
    }
    
    private fun updatePageActivity(page: Page) {
        pageLastActivity[page] = System.currentTimeMillis()
    }
}