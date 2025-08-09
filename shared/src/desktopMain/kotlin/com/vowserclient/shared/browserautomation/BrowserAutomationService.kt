package com.vowserclient.shared.browserautomation

import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
import com.microsoft.playwright.PlaywrightException
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

object BrowserAutomationService {

    private lateinit var playwright: Playwright
    private lateinit var browser: Browser
    private lateinit var page: Page
    private val mutex = Mutex()

    suspend fun initialize() = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!::playwright.isInitialized) {
                Napier.i("BrowserAutomationService: Initializing Playwright...", tag = "BrowserAutomationService")
                try {
                    playwright = Playwright.create()
                    browser = playwright.chromium().launch(
                        BrowserType.LaunchOptions()
                            .setHeadless(false)
                            .setChannel("chrome")
                    )
                    page = browser.newPage()
                    page.waitForLoadState()
                    // TODO 초기화 실패 시 예외 발생
                    Napier.i("BrowserAutomationService: Playwright initialized successfully.", tag = "BrowserAutomationService")
                } catch (e: Exception) {
                    Napier.e("BrowserAutomationService: Failed to initialize Playwright: ${e.message}", e, tag = "BrowserAutomationService")
                    throw e
                }
            }
        }
    }

    suspend fun cleanup() = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (::playwright.isInitialized) {
                Napier.i("BrowserAutomationService: Cleaning up Playwright resources.", tag = "BrowserAutomationService")
                try {
                    page.close()
                    browser.close()
                    playwright.close()
                } catch (e: Exception) {
                    Napier.e("BrowserAutomationService: Failed to clean up Playwright resources: ${e.message}", e, tag = "BrowserAutomationService")
                }
            }
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


    suspend fun hoverAndClickElement(selector: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!::page.isInitialized) {
                Napier.e("BrowserAutomationService: Page is not initialized. Cannot hover hoverAndClickElement element: $selector", tag = "BrowserAutomationService")
                return@withContext
            }
            try {
                val locator = page.locator(selector)
                
                // 먼저 요소 존재 여부를 빠르게 확인
                if (locator.count() == 0) {
                    Napier.w { "Element with selector $selector not found on page, trying alternatives." }
                    tryAlternativeSelectors(selector)
                    return@withContext
                }
                
                // 타임아웃을 3초로 단축, 여러 요소 있으면 첫 번째 선택
                locator.first().waitFor(Locator.WaitForOptions().setTimeout(3000.0))
                
                if (locator.count() > 0 && locator.first().isVisible) {
                    executeHoverHighlightClick(locator, selector)
                } else {
                    Napier.w { "Element with selector $selector not found or not visible after waiting." }
                    tryAlternativeSelectors(selector)
                }
            } catch (e: PlaywrightException) {
                Napier.e("Failed to hoverAndClickElement $selector: ${e.message}", e, tag = "BrowserAutomationService")
                tryAlternativeSelectors(selector)
            } catch (e: Exception) {
                Napier.e("Unexpected error hoverAndClickElement $selector: ${e.message}", e, tag = "BrowserAutomationService")
            }
        }
    }
    
    private suspend fun tryAlternativeSelectors(originalSelector: String) = withContext(Dispatchers.IO) {
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
            return@withContext
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
                
                // 짧은 타임아웃으로 대기, 여러 요소 있으면 첫 번째 선택
                locator.first().waitFor(Locator.WaitForOptions().setTimeout(2000.0))
                
                if (locator.count() > 0 && locator.first().isVisible) {
                    executeHoverHighlightClick(locator, altSelector)
                    Napier.i { "Successfully clicked using alternative selector: $altSelector" }
                    return@withContext
                }
            } catch (e: Exception) {
                Napier.w { "Alternative selector $altSelector also failed: ${e.message}" }
            }
        }
        Napier.w { "All alternative selectors failed for original selector: $originalSelector. Continuing anyway." }
    }

    private suspend fun executeHoverHighlightClick(locator: Locator, selector: String) = withContext(Dispatchers.IO) {
        try {
            val element = locator.first()
            
            element.scrollIntoViewIfNeeded()
            element.hover()
            page.evaluate(HIGHLIGHT_SCRIPT_CONTENT, selector)
            Napier.i { "Applied highlight to element with selector: $selector" }
            
            kotlinx.coroutines.delay(3000)

            element.click()
            Napier.i { "Clicked element with selector: $selector" }

        } catch (e: Exception) {
            Napier.e("Failed to execute hover-highlight-click for $selector: ${e.message}", e, tag = "BrowserAutomationService")
            try {
                locator.first().click()
                Napier.i { "Fallback click succeeded for selector: $selector" }
            } catch (fallbackError: Exception) {
                Napier.e("Fallback click also failed for $selector: ${fallbackError.message}", fallbackError, tag = "BrowserAutomationService")
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
        indicator.textContent = '📌 클릭 대상';
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
}