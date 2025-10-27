@file:OptIn(ExperimentalSerializationApi::class)

package com.vowser.client.api.dto

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

// ===== 경로 저장 (POST /api/v1/paths) =====

@Serializable
data class PathSubmission(
    @SerialName("sessionId") @JsonNames("session_id")
    val sessionId: String,
    @SerialName("taskIntent") @JsonNames("task_intent")
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
    @SerialName("textLabels") @JsonNames("text_labels")
    val textLabels: List<String>? = emptyList(),

    @SerialName("isInput") @JsonNames("is_input")
    val isInput: Boolean = false,

    @SerialName("shouldWait") @JsonNames("should_wait")
    val shouldWait: Boolean = false,

    @SerialName("inputType") @JsonNames("input_type")
    val inputType: String? = null,

    @SerialName("inputPlaceholder") @JsonNames("input_placeholder")
    val inputPlaceholder: String? = null,

    @SerialName("waitMessage") @JsonNames("wait_message")
    val waitMessage: String? = null
)

@Serializable
data class PathSaveResponse(
    val type: String = "path_save_result",
    val status: String,
    val data: PathSaveResult? = null,
    val error: PathSaveError? = null
)

@Serializable
data class PathSaveResult(
    val result: PathSaveDetails? = null
)

@Serializable
data class PathSaveDetails(
    @SerialName("sessionId") @JsonNames("session_id")
    val sessionId: String? = null,

    @SerialName("taskIntent") @JsonNames("task_intent")
    val taskIntent: String? = null,

    val domain: String? = null,

    @SerialName("stepsSaved") @JsonNames("steps_saved")
    val stepsSaved: Int? = 0
)

@Serializable
data class PathSaveError(
    val message: String? = null,
    val code: String? = null
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

    @SerialName("totalMatched") @JsonNames("total_matched")
    val totalMatched: Int? = 0,

    @SerialName("matchedPaths") @JsonNames("matched_paths")
    val matchedPaths: List<MatchedPathDetail>? = emptyList(),

    val performance: PerformanceMetrics
)

@Serializable
data class MatchedPathDetail(
    val domain: String,

    @SerialName("taskIntent") @JsonNames("task_intent")
    val taskIntent: String? = null,

    @SerialName("relevanceScore") @JsonNames("relevance_score")
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

    @SerialName("isInput") @JsonNames("is_input")
    val isInput: Boolean? = null,

    @SerialName("shouldWait") @JsonNames("should_wait")
    val shouldWait: Boolean? = null,

    @SerialName("textLabels") @JsonNames("text_labels")
    val textLabels: List<String>? = emptyList(),

    @SerialName("inputType") @JsonNames("input_type")
    val inputType: String? = null,

    @SerialName("inputPlaceholder") @JsonNames("input_placeholder")
    val inputPlaceholder: String? = null,

    @SerialName("waitMessage") @JsonNames("wait_message")
    val waitMessage: String? = null
)

@Serializable
data class PerformanceMetrics(
    @SerialName("searchTime") @JsonNames("search_time")
    val searchTime: Long? = 0L
)
