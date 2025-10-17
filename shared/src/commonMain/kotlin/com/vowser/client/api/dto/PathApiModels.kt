package com.vowser.client.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

// ===== 경로 저장 (POST /api/v1/paths) =====

@Serializable
data class PathSubmission(
    @SerialName("session_id") @JsonNames("sessionId")
    val sessionId: String,
    @SerialName("task_intent") @JsonNames("taskIntent")
    val taskIntent: String,
    val domain: String,
    val steps: List<PathStepSubmission>
)

@Serializable
data class PathStepSubmission(
    val url: String,
    val domain: String,
    val action: String,
    val selectors: List<String>,
    val description: String,
    @SerialName("text_labels") @JsonNames("textLabels")
    val textLabels: List<String>? = emptyList(),

    @SerialName("is_input") @JsonNames("isInput")
    val isInput: Boolean = false,

    @SerialName("should_wait") @JsonNames("shouldWait")
    val shouldWait: Boolean = false,

    @SerialName("input_type") @JsonNames("inputType")
    val inputType: String? = null,

    @SerialName("input_placeholder") @JsonNames("inputPlaceholder")
    val inputPlaceholder: String? = null,

    @SerialName("wait_message") @JsonNames("waitMessage")
    val waitMessage: String? = null
)

@Serializable
data class PathSaveResponse(
    val type: String = "path_save_result",
    val status: String,
    val data: PathSaveResult
)

@Serializable
data class PathSaveResult(
    val result: PathSaveDetails
)

@Serializable
data class PathSaveDetails(
    @SerialName("session_id") @JsonNames("sessionId")
    val sessionId: String,

    @SerialName("task_intent") @JsonNames("taskIntent")
    val taskIntent: String,

    val domain: String,

    @SerialName("steps_saved") @JsonNames("stepsSaved")
    val stepsSaved: Int
)

// ===== 경로 검색 (GET /api/v1/paths/search) =====

@Serializable
data class PathSearchResponse(
    val type: String = "search_path_result",
    val status: String,
    val data: PathSearchResult
)

@Serializable
data class PathSearchResult(
    val query: String,

    @SerialName("total_matched") @JsonNames("totalMatched")
    val totalMatched: Int? = 0,

    @SerialName("matched_paths") @JsonNames("matchedPaths")
    val matchedPaths: List<MatchedPathDetail>? = emptyList(),

    val performance: PerformanceMetrics
)

@Serializable
data class MatchedPathDetail(
    val domain: String,

    @SerialName("task_intent") @JsonNames("taskIntent")
    val taskIntent: String? = null,

    @SerialName("relevance_score") @JsonNames("relevanceScore")
    val relevanceScore: Double,

    val weight: Int,

    val steps: List<PathStepDetail>
)

@Serializable
data class PathStepDetail(
    val order: Int,
    val url: String,
    val action: String,
    val selectors: List<String>,
    val description: String,

    @SerialName("is_input") @JsonNames("isInput")
    val isInput: Boolean? = null,

    @SerialName("should_wait") @JsonNames("shouldWait")
    val shouldWait: Boolean? = null,

    @SerialName("text_labels") @JsonNames("textLabels")
    val textLabels: List<String>? = emptyList(),

    @SerialName("input_type") @JsonNames("inputType")
    val inputType: String? = null,

    @SerialName("input_placeholder") @JsonNames("inputPlaceholder")
    val inputPlaceholder: String? = null,

    @SerialName("wait_message") @JsonNames("waitMessage")
    val waitMessage: String? = null
)

@Serializable
data class PerformanceMetrics(
    @SerialName("search_time") @JsonNames("searchTime")
    val searchTime: Long? = 0L
)