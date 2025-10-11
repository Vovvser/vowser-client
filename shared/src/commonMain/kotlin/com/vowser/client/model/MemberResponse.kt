package com.vowser.client.model

import kotlinx.serialization.Serializable

@Serializable
data class MemberResponse(
    val id: Long,
    val email: String,
    val name: String,
    val naverId: String? = null,
    val phoneNumber: String? = null,
    val birthdate: String? = null,  // LocalDate는 String으로 직렬화됨
    val createdAt: String? = null,  // LocalDateTime은 String으로 직렬화됨
    val updatedAt: String? = null
)