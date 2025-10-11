package com.vowser.client.auth

interface TokenStorage {
    fun saveTokens(accessToken: String, refreshToken: String)
    fun getAccessToken(): String?
    fun getRefreshToken(): String?
    fun clearTokens()
}