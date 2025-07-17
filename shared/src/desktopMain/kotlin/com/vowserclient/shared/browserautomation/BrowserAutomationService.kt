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

// 간단한 테스트를 위해서 싱글톤으로 구현 -> object 사용
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

    private suspend fun findVisibleElement(selectors: List<String>): Locator? = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!::page.isInitialized) {
                Napier.e("BrowserAutomationService: Page is not initialized. Cannot find element.", tag = "BrowserAutomationService")
                return@withContext null
            }
            for (selector in selectors) {
                val locator = page.locator(selector)
                if (locator.count() > 0 && locator.first().isVisible) {
                    Napier.i { "'$selector' 선택자로 요소를 찾았습니다."}
                    return@withContext locator
                }
            }
            Napier.w { "No visible element found for selectors: $selectors" }
            return@withContext null
        }
    }

    suspend fun clickElement(selector: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!::page.isInitialized) {
                Napier.e("BrowserAutomationService: Page is not initialized. Cannot click element: $selector", tag = "BrowserAutomationService")
                return@withContext
            }
            try {
                val locator = page.locator(selector)
                if (locator.count() > 0 && locator.first().isVisible) {
                    locator.first().click()
                    Napier.i { "Clicked element with selector: $selector" }
                } else {
                    Napier.w { "Element with selector $selector not found or not visible for clicking." }
                }
            } catch (e: PlaywrightException) {
                Napier.e("Failed to click element $selector: ${e.message}", e, tag = "BrowserAutomationService")
            } catch (e: Exception) {
                Napier.e("Unexpected error clicking element $selector: ${e.message}", e, tag = "BrowserAutomationService")
            }
        }
    }

    suspend fun typeText(selector: String, text: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!::page.isInitialized) {
                Napier.e("BrowserAutomationService: Page is not initialized. Cannot type text into element: $selector", tag = "BrowserAutomationService")
                return@withContext
            }
            try {
                val locator = page.locator(selector)
                if (locator.count() > 0 && locator.first().isVisible) {
                    locator.first().fill(text)
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
}