package com.vowserclient.shared.browserautomation

import io.github.aakira.napier.Napier
import kotlinx.coroutines.delay
import kotlin.random.Random

class BrowserActions(private val browserAutomationService: BrowserAutomationService) {

    // 무작위 지연을 위한 최소/최대 시간 (밀리초)
    private val MIN_DELAY_MS = 500L
    private val MAX_DELAY_MS = 1000L

    // 무작위 스크롤을 위한 최소/최대 픽셀
    private val MIN_SCROLL_PX = 100
    private val MAX_SCROLL_PX = 1000

    suspend fun delayRandomly() {
        val delayTime = Random.nextLong(MIN_DELAY_MS, MAX_DELAY_MS + 1)
        Napier.i { "Applying random delay: ${delayTime}ms" }
        delay(delayTime)
    }

    suspend fun scrollRandomly() {
        val scrollAmount = Random.nextInt(MIN_SCROLL_PX, MAX_SCROLL_PX + 1)
        val direction = if (Random.nextBoolean()) 1 else -1
        Napier.i { "Applying random scroll: ${scrollAmount * direction}px" }
        browserAutomationService.scrollPage(0.0, (scrollAmount * direction).toDouble())
        delayRandomly()
    }

    suspend fun navigate(url: String) {
        delayRandomly()
        browserAutomationService.navigate(url)
        scrollRandomly()
        delayRandomly()
    }

    suspend fun click(selector: String) {
        delayRandomly()
        browserAutomationService.hoverElement(selector)
        delayRandomly()
        browserAutomationService.clickElement(selector)
        delayRandomly()
    }

    suspend fun type(selector: String, text: String) {
        delayRandomly()
        browserAutomationService.typeText(selector, text, Random.nextLong(50, 151).toDouble()) // 각 문자 입력 사이에 무작위 지연 추가
        delayRandomly()
    }

    suspend fun goBack() {
        delayRandomly()
        browserAutomationService.goBack()
        delayRandomly()
    }

    suspend fun goForward() {
        delayRandomly()
        browserAutomationService.goForward()
        delayRandomly()
    }
}
