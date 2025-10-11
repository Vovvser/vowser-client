package com.vowser.client.model

import kotlinx.serialization.Serializable

@Serializable
data class MemberResponse(
    val id: Long,
    val email: String,
    val name: String,
    val profileImageUrl: String
)