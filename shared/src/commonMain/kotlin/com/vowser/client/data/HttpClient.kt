package com.vowser.client.data

import com.vowser.client.auth.TokenStorage
import com.vowser.client.model.ApiResponse
import com.vowser.client.logging.Tags
import io.github.aakira.napier.Napier
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
                    val refreshToken = tokenStorage.getRefreshToken()

                    if (refreshToken == null) {
                        Napier.w("No refresh token available", tag = Tags.API)
                        tokenStorage.clearTokens()
                        return@refreshTokens null
                    }

                    try {
                        Napier.i("Attempting token refresh...", tag = Tags.API)

                        val response: ApiResponse<com.vowser.client.model.TokenResponse> = client.post("$baseUrl/api/auth/refresh") {
                            contentType(ContentType.Application.Json)
                            header("Authorization", "Bearer $refreshToken")
                        }.body()

                        if (!response.success || response.data == null) {
                            Napier.w("Token refresh failed - API response unsuccessful", tag = Tags.API)
                            tokenStorage.clearTokens()
                            return@refreshTokens null
                        }

                        val tokenResponse = response.data
                        val accessToken = tokenResponse.accessToken
                        val newRefreshToken = tokenResponse.refreshToken

                        if (accessToken.isBlank() || newRefreshToken.isBlank()) {
                            Napier.w("Token refresh failed - missing tokens in response", tag = Tags.API)
                            tokenStorage.clearTokens()
                            return@refreshTokens null
                        }

                        tokenStorage.saveTokens(accessToken, newRefreshToken)
                        Napier.i("Token refreshed successfully", tag = Tags.API)

                        BearerTokens(
                            accessToken = accessToken,
                            refreshToken = newRefreshToken
                        )
                    } catch (e: Exception) {
                        Napier.e("Token refresh failed: ${e.message}", e, tag = Tags.API)
                        tokenStorage.clearTokens()
                        null
                    }
                }
            }
        }
    }
}