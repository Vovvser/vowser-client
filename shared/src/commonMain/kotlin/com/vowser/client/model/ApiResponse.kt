package com.vowser.client.model

import kotlinx.serialization.Serializable

/**
 * Backend의 공통 API 응답 포맷
 * Backend의 ApiResponse.java 구조와 일치
 */
@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ErrorInfo? = null,
    val timestamp: String
)

@Serializable
data class ErrorInfo(
    val code: String,
    val message: String,
    val detail: String? = null,
    val status: Int
)
