package com.vowser.client.data

import com.vowser.client.auth.TokenStorage
import com.vowser.client.model.ApiResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

fun createHttpClient(tokenStorage: TokenStorage, baseUrl: String = "http://localhost:8080"): HttpClient {
    return HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                isLenient = true
            })
        }
        install(Auth) {
            bearer {
                loadTokens {
                    val accessToken = tokenStorage.getAccessToken()
                    val refreshToken = tokenStorage.getRefreshToken()
                    if (accessToken != null && refreshToken != null) {
                        BearerTokens(accessToken, refreshToken)
                    } else {
                        null
                    }
                }

                refreshTokens {
                    // 401 Unauthorized 응답 시 자동으로 토큰 갱신
                    val refreshToken = tokenStorage.getRefreshToken()

                    if (refreshToken == null) {
                        println("HttpClient: No refresh token available")
                        tokenStorage.clearTokens()
                        return@refreshTokens null
                    }

                    try {
                        val response: ApiResponse<Map<String, String>> = client.post("$baseUrl/api/auth/refresh") {
                            contentType(ContentType.Application.Json)
                            header("Authorization", "Bearer $refreshToken")
                        }.body()

                        // 응답 검증
                        if (!response.success || response.data == null) {
                            println("HttpClient: Token refresh failed - API response unsuccessful")
                            tokenStorage.clearTokens()
                            return@refreshTokens null
                        }

                        val accessToken = response.data["accessToken"]
                        val newRefreshToken = response.data["refreshToken"]

                        if (accessToken == null || newRefreshToken == null) {
                            println("HttpClient: Token refresh failed - missing tokens in response")
                            tokenStorage.clearTokens()
                            return@refreshTokens null
                        }

                        // 새 토큰 저장
                        tokenStorage.saveTokens(accessToken, newRefreshToken)
                        println("HttpClient: Token refreshed successfully")

                        BearerTokens(
                            accessToken = accessToken,
                            refreshToken = newRefreshToken
                        )
                    } catch (e: Exception) {
                        println("HttpClient: Token refresh failed: ${e.message}")
                        tokenStorage.clearTokens()
                        null
                    }
                }
            }
        }
    }
}