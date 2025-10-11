package com.vowser.client.model

sealed class AuthState {
    object Loading : AuthState()
    object NotAuthenticated : AuthState()
    data class Authenticated(val name: String, val email: String) : AuthState()
    data class Error(val message: String) : AuthState()
}
