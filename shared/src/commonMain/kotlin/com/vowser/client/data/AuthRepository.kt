package com.vowser.client.data

import com.vowser.client.model.MemberResponse
import io.github.aakira.napier.Napier
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * 로그인 API 응답
 */
@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null
)

/**
 * 로그인 API 호출 Repository
 */
class AuthRepository(private val backendUrl: String = "http://localhost:8080") {

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        install(HttpCookies) {
            // 쿠키 자동 관리
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30000
        }
        defaultRequest {
            contentType(ContentType.Application.Json)
        }
    }

    /**
     * 현재 로그인한 사용자 정보 조회
     * 쿠키에 저장된 JWT 토큰으로 인증
     */
    suspend fun getCurrentUser(): Result<MemberResponse> {
        return try {
            Napier.d("Fetching current user info from $backendUrl/api/auth/me")
            val response = client.get("$backendUrl/api/auth/me")

            if (response.status == HttpStatusCode.OK) {
                val apiResponse = response.body<ApiResponse<MemberResponse>>()
                if (apiResponse.success && apiResponse.data != null) {
                    Napier.i("Successfully fetched user info: ${apiResponse.data.name}")
                    Result.success(apiResponse.data)
                } else {
                    Napier.w("API returned success=false: ${apiResponse.message}")
                    Result.failure(Exception(apiResponse.message ?: "Unknown error"))
                }
            } else if (response.status == HttpStatusCode.Unauthorized) {
                Napier.w("User not authenticated (401)")
                Result.failure(Exception("Not authenticated"))
            } else {
                Napier.e("Unexpected status: ${response.status}")
                Result.failure(Exception("HTTP ${response.status.value}"))
            }
        } catch (e: Exception) {
            Napier.e("Failed to get current user: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * 로그아웃
     */
    suspend fun logout(): Result<String> {
        return try {
            Napier.d("Logging out from $backendUrl/api/auth/logout")
            val response = client.post("$backendUrl/api/auth/logout")

            if (response.status == HttpStatusCode.OK) {
                val message = response.body<String>()
                Napier.i("Logout successful: $message")
                Result.success(message)
            } else {
                Napier.e("Logout failed with status: ${response.status}")
                Result.failure(Exception("Logout failed"))
            }
        } catch (e: Exception) {
            Napier.e("Failed to logout: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * OAuth 로그인 URL 생성
     */
    fun getOAuthLoginUrl(): String {
        return "$backendUrl/oauth2/authorization/naver"
    }

    /**
     * 리소스 정리
     */
    fun close() {
        client.close()
    }
}
