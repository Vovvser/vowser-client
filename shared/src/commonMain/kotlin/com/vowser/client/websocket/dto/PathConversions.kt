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
        task_intent = this.taskIntent,
        relevance_score = this.relevanceScore,
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
        is_input = this.isInput,
        should_wait = this.shouldWait,
        text_labels = this.textLabels,
        input_type = this.inputType,
        input_placeholder = this.inputPlaceholder,
        wait_message = this.waitMessage
    )
}