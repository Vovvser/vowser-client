package com.vowser.client.model

import kotlinx.serialization.Serializable

/**
 * 회원 정보 응답 데이터 모델
 */
@Serializable
data class MemberResponse(
    val id: Long,
    val email: String,
    val name: String,
    val phoneNumber: String? = null,
    val birthdate: String? = null,
    val naverId: String? = null,
    val createdAt: String,
    val updatedAt: String
)

/**
 * 로그인 상태를 나타냄
 */
sealed class AuthState {
    object NotAuthenticated : AuthState()
    object Loading : AuthState()
    data class Authenticated(val user: MemberResponse) : AuthState()
    data class Error(val message: String) : AuthState()
}
