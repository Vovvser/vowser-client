package com.vowser.client.model

import kotlinx.serialization.Serializable

@Serializable
data class MemberResponse(
    val id: Long,
    val email: String,
    val name: String,
    val phoneNumber: String? = null,
    val birthdate: String? = null,
    val naverId: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)