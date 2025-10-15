package com.vowser.client.websocket.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ErrorData(val message: String)

@Serializable
data class PathStep(
    @SerialName("order") val order: Int,
    @SerialName("url") val url: String,
    @SerialName("action") val action: String,
    @SerialName("selectors") val selectors: List<String>,
    @SerialName("description") val description: String,
    @SerialName("isInput") val isInput: Boolean = false,
    @SerialName("inputType") val inputType: String? = null,
    @SerialName("inputPlaceholder") val inputPlaceholder: String? = null,
    @SerialName("shouldWait") val shouldWait: Boolean = false,
    @SerialName("waitMessage") val waitMessage: String? = null,
    @SerialName("textLabels") val textLabels: List<String> = emptyList()
)

@Serializable
data class MatchedPath(
    @SerialName("domain") val domain: String,
    @SerialName("taskIntent") val taskIntent: String,
    @SerialName("relevance_score") val relevanceScore: Double,
    @SerialName("weight") val weight: Int,
    @SerialName("steps") val steps: List<PathStep>
)

@Serializable
data class PerformanceData(
    @SerialName("search_time") val searchTime: Long
)

@Serializable
data class SearchPathResultData(
    val query: String,
    val total_matched: Int,
    val matched_paths: List<MatchedPath> = emptyList(),
    val performance: PerformanceData
)

@Serializable
sealed interface WebSocketMessage {
    @Serializable
    @SerialName("browser_command")
    data class BrowserCommandWrapper(val data: BrowserCommand) : WebSocketMessage
    
    @Serializable
    @SerialName("voice_processing_result")
    data class VoiceProcessingResultWrapper(val data: VoiceProcessingResult) : WebSocketMessage
    
    @Serializable
    @SerialName("error")
    data class ErrorWrapper(val status: String, val data: ErrorData) : WebSocketMessage
    
    @Serializable
    @SerialName("search_path_result")
    data class SearchPathResultWrapper(val status: String, val data: SearchPathResultData) : WebSocketMessage

    @Serializable
    @SerialName("all_navigation_paths")
    data class AllPathsWrapper(val data: AllPathsResponse) : WebSocketMessage // 1단계에서 만든 DTO 사용

}