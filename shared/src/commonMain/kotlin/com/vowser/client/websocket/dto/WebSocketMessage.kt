package com.vowser.client.websocket.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ErrorData(val message: String)

@Serializable
data class SearchMetadata(
    val total_found: Int,
    val search_time_ms: Long,
    val vector_search_used: Boolean,
    val min_score_threshold: Double
)

@Serializable
data class SearchPathResultData(
    val query: String,
    val message: String? = null,
    val matched_paths: List<String> = emptyList(),
    val search_metadata: SearchMetadata? = null
)

@Serializable
sealed interface WebSocketMessage {
    @Serializable
    @SerialName("browser_command")
    data class BrowserCommandWrapper(val data: BrowserCommand) : WebSocketMessage

    @Serializable
    @SerialName("navigation_path")
    data class NavigationPathWrapper(val data: NavigationPath) : WebSocketMessage
    
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
}