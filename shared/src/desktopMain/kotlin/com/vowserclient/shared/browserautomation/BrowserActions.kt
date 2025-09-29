package com.vowserclient.shared.browserautomation

import io.github.aakira.napier.Napier
import com.vowser.client.logging.Tags
import kotlinx.coroutines.delay
import kotlin.random.Random

class BrowserActions(private val browserAutomationService: BrowserAutomationService) {

    // 무작위 지연을 위한 최소/최대 시간 (밀리초)
    private val MIN_DELAY = 500L
    private val MAX_DELAY = 1000L

    suspend fun delayRandomly() {
        val delayTime = Random.nextLong(MIN_DELAY, MAX_DELAY + 1)
        Napier.i("Applying random delay: ${delayTime}ms", tag = Tags.BROWSER_AUTOMATION)
        delay(delayTime)
    }

    suspend fun navigate(url: String) {
        browserAutomationService.navigate(url)
        delayRandomly()
    }

    suspend fun click(selector: String): Boolean {
        delayRandomly()
        return browserAutomationService.hoverAndClickElement(selector)
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
