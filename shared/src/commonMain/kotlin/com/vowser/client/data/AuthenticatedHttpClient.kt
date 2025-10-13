package com.vowser.client.data

import io.github.aakira.napier.Napier
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/**
 * 인증이 필요한 HTTP 요청을 처리하는 공통 HttpClient
 */
object AuthenticatedHttpClient {

    private const val backendUrl = "http://localhost:8080"
    var onTokenRefreshFailed: (() -> Unit)? = null

    val client = HttpClient {
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
     * 인증이 필요한 HTTP 요청을 실행
     */
    suspend fun <T> executeWithAuth(
        onTokenRefreshFailedCallback: (() -> Unit)? = null,
        block: suspend (HttpClient) -> T
    ): T {
        try {
            return block(client)
        } catch (e: ResponseException) {
            if (e.response.status == HttpStatusCode.Unauthorized) {
                Napier.w("Received 401 Unauthorized - attempting token refresh")

                // 토큰 갱신 시도
                val refreshResult = refreshToken()

                if (refreshResult.isSuccess) {
                    Napier.i("Token refreshed successfully - retrying original request")
                    // 원래 요청 재시도
                    return block(client)
                } else {
                    Napier.e("Token refresh failed - invoking callback")
                    onTokenRefreshFailedCallback?.invoke()
                    onTokenRefreshFailed?.invoke()
                    throw e
                }
            } else {
                throw e
            }
        }
    }

    /**
     * RefreshToken을 사용해 새로운 AccessToken 발급
     */
    private suspend fun refreshToken(): Result<Unit> {
        return try {
            Napier.d("Refreshing token from $backendUrl/api/auth/refresh")
            val response = client.post("$backendUrl/api/auth/refresh")

            when (response.status) {
                HttpStatusCode.OK -> {
                    Napier.i("Token refreshed successfully")
                    Result.success(Unit)
                }
                HttpStatusCode.Unauthorized -> {
                    Napier.w("Refresh token expired (401) - logout required")
                    Result.failure(Exception("Refresh token expired"))
                }
                else -> {
                    Napier.e("Token refresh failed with status: ${response.status}")
                    Result.failure(Exception("Token refresh failed: ${response.status}"))
                }
            }
        } catch (e: Exception) {
            Napier.e("Failed to refresh token: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * 리소스 정리
     */
    fun close() {
        client.close()
    }
}
