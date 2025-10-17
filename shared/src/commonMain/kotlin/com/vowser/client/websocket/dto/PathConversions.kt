package com.vowser.client.websocket.dto

import com.vowser.client.api.dto.MatchedPathDetail
import com.vowser.client.api.dto.PathStepDetail

/**
 * MatchedPath를 MatchedPathDetail로 변환
 * (WebSocket 응답 → PathExecutor 입력)
 */
fun MatchedPath.toMatchedPathDetail(): MatchedPathDetail {
    return MatchedPathDetail(
        domain = this.domain,
        taskIntent = this.taskIntent,
        relevanceScore = this.relevanceScore,
        weight = this.weight,
        steps = this.steps.map { it.toPathStepDetail() }
    )
}

/**
 * PathStep을 PathStepDetail로 변환
 */
fun PathStep.toPathStepDetail(): PathStepDetail {
    return PathStepDetail(
        order = this.order,
        url = this.url,
        action = this.action,
        selectors = this.selectors,
        description = this.description,
        isInput = this.isInput,
        shouldWait = this.shouldWait,
        textLabels = this.textLabels,
        inputType = this.inputType,
        inputPlaceholder = this.inputPlaceholder,
        waitMessage = this.waitMessage
    )
}