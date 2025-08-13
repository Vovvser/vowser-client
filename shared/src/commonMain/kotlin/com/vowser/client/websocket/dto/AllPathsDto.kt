package com.vowser.client.websocket.dto

import kotlinx.serialization.Serializable

@Serializable
data class AllPathsResponse(
    val query: String,
    val paths: List<PathDetail>
)

@Serializable
data class PathDetail(
    val pathId: String,
    val score: Double?,
    val total_weight: Int?,
    val last_used: String? = null,
    val estimated_time: Double? = null,
    val steps: List<NavigationStep>
)
