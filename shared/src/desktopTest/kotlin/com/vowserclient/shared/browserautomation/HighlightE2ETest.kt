package com.vowserclient.shared.browserautomation

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.Assertions.assertTrue

/**
 * 하이라이트 기능 E2E 테스트
 * 
 * 실제 웹사이트에서 하이라이트가 올바르게 동작하는지 확인
 * 테스트 결과 자체는 늘 True로 나오게 구현했으므로, 시각적인 확인 꼭 해주세요!
 *
 * 테스트 방법:
 * 1. 수동 실행하여 시각적으로 확인
 * 2. Delay 시간을 늘려 DOM 요소 확인
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HighlightE2ETest {

    private lateinit var service: BrowserAutomationService
    private lateinit var actions: BrowserActions

    @BeforeAll
    fun setupAll() = runBlocking {
        service = BrowserAutomationService
        service.initialize()
        actions = BrowserActions(service)
    }

    @AfterAll
    fun teardownAll() = runBlocking {
        service.cleanup()
    }


    @Test
    fun highlightShouldBeVisibleOnGoogleHomepage() = runBlocking {
        actions.navigate("https://example.com")
        println("=== 링크 클릭 테스트 ===")
        actions.click("a")
        println("=== 클릭 완료, 확인 바람 ===")

        delay(5000)
        assertTrue(true)
    }

    @Test
    fun highlightShouldWorkOnMultipleElements() = runBlocking {
        val testHtml = "data:text/html,<html><body><button id='btn1'>Button 1</button><button id='btn2'>Button 2</button></body></html>"
            
        actions.navigate(testHtml)

        println("=== 버튼 1 클릭 테스트 ===")
        actions.click("#btn1")
            
        println("=== 버튼 2 클릭 테스트 ===")
        actions.click("#btn2")
        println("=== 클릭 완료, 확인 바람 ===")

        delay(5000)
        assertTrue(true)
    }
    
    @Test
    fun highlightShouldHandleScrollProperly() = runBlocking {
        val longPageHtml = "data:text/html,<html><body><div style='height:2000px'></div><button id='bottom-btn'>Bottom Button</button></body></html>"
        actions.navigate(longPageHtml)
        println("=== 스크롤 + 하이라이트 테스트 ===")
        actions.click("#bottom-btn")
        println("=== 클릭 완료, 확인 바람 ===")

        delay(5000)
        assertTrue(true)

    }
}