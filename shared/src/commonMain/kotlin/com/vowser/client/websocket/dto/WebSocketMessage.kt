package com.vowser.client.websocket.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ErrorData(val message: String)

@Serializable
data class PathStep(
    val title: String,
    val action: String,
    val url: String,
    val selector: String = ""
)

@Serializable
data class MatchedPath(
    val pathId: String,
    val description: String,
    val score: Double,
    val startCommand: String,
    val startUrl: String,
    val total_weight: Int,
    val steps: List<PathStep>
)

@Serializable
data class PerformanceData(
    val query_time: Long,
    val search_time: Long
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
    @SerialName("graph_update")
    data class GraphUpdateWrapper(val data: GraphUpdateData) : WebSocketMessage
    
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