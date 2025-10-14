package com.vowser.client.data

import com.vowser.client.model.ApiResponse
import com.vowser.client.model.MemberResponse
import com.vowser.client.model.TokenResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*

class AuthRepository(
    private val httpClient: HttpClient,
    private val baseUrl: String = "http://localhost:8080"
) {

    suspend fun getMe(): Result<MemberResponse> {
        return try {
            val response: ApiResponse<MemberResponse> = httpClient.get("$baseUrl/api/auth/me").body()

            if (response.success && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.error?.message ?: "Failed to get user info"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout(): Result<Unit> {
        return try {
            httpClient.post("$baseUrl/api/auth/logout")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun exchangeCodeForToken(code: String): Result<TokenResponse> {
        return try {
            val response: ApiResponse<TokenResponse> = httpClient.post("$baseUrl/api/auth/token/exchange?code=$code").body()

            if (response.success && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.error?.message ?: "Failed to exchange code for token"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}