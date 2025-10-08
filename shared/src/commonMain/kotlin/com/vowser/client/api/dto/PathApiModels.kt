package com.vowser.client.api.dto

import kotlinx.serialization.Serializable

// ===== 경로 저장 (POST /api/v1/paths) =====

@Serializable
data class PathSubmission(
    val session_id: String,
    val task_intent: String,  // 필수: 작업 의도
    val domain: String,        // 필수: 도메인
    val steps: List<PathStepSubmission>
)

@Serializable
data class PathStepSubmission(
    val url: String,
    val domain: String,
    val action: String,  // "click" | "input" | "wait"
    val selectors: List<String>,  // 다중 셀렉터 (fallback용)
    val description: String,
    val text_labels: List<String> = emptyList(),
    val is_input: Boolean = false,
    val should_wait: Boolean = false,
    // Input 액션 추가 정보
    val input_type: String? = null,  // "email" | "id" | "password" | "search" | "text"
    val input_placeholder: String? = null,
    // Wait 액션 추가 정보
    val wait_message: String? = null
)

@Serializable
data class PathSaveResponse(
    val type: String = "path_save_result",
    val status: String,  // "success" | "error"
    val data: PathSaveResult
)

@Serializable
data class PathSaveResult(
    val result: PathSaveDetails
)

@Serializable
data class PathSaveDetails(
    val session_id: String,
    val task_intent: String,
    val domain: String,
    val steps_saved: Int
)

// ===== 경로 검색 (GET /api/v1/paths/search) =====

@Serializable
data class PathSearchResponse(
    val type: String = "search_path_result",
    val status: String,  // "success" | "error"
    val data: PathSearchResult
)

@Serializable
data class PathSearchResult(
    val query: String,
    val total_matched: Int,
    val matched_paths: List<MatchedPathDetail>,
    val performance: PerformanceMetrics
)

@Serializable
data class MatchedPathDetail(
    val domain: String,
    val task_intent: String,  // 핵심: 작업 의도
    val relevance_score: Double,
    val weight: Int,
    val steps: List<PathStepDetail>
)

@Serializable
data class PathStepDetail(
    val order: Int,
    val url: String,
    val action: String,  // "click" | "input" | "wait"
    val selectors: List<String>,
    val description: String,
    val is_input: Boolean,
    val should_wait: Boolean,
    val text_labels: List<String> = emptyList(),
    // Input 필드
    val input_type: String? = null,
    val input_placeholder: String? = null,
    // Wait 필드
    val wait_message: String? = null
)

@Serializable
data class PerformanceMetrics(
    val search_time: Long  // milliseconds
)