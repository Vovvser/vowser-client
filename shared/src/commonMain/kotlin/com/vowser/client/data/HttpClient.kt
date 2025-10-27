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
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

@OptIn(ExperimentalSerializationApi::class)
fun createHttpClient(tokenStorage: TokenStorage, baseUrl: String = "http://localhost:8080"): HttpClient {
    return HttpClient {
        followRedirects = false // 리다이렉트 자동 추적 방지
        expectSuccess = false

        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                isLenient = true
                explicitNulls = false
            })
        }
        install(Auth) {
            bearer {
                sendWithoutRequest { true }
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

                        val httpResponse = client.post("$baseUrl/api/auth/refresh") {
                            contentType(ContentType.Application.Json)
                            header("Authorization", "Bearer $refreshToken")
                        }

                        if (httpResponse.status == HttpStatusCode.Unauthorized) {
                            Napier.w("Token refresh failed - unauthorized", tag = Tags.API)
                            tokenStorage.clearTokens()
                            return@refreshTokens null
                        }

                        if (httpResponse.status.value !in 200..299) {
                            Napier.w("Token refresh failed - status ${httpResponse.status}", tag = Tags.API)
                            return@refreshTokens null
                        }

                        val apiResponse = runCatching {
                            httpResponse.body<ApiResponse<com.vowser.client.model.TokenResponse>>()
                        }.getOrElse { parseError ->
                            Napier.e("Token refresh response parsing failed: ${parseError.message}", parseError, tag = Tags.API)
                            return@refreshTokens null
                        }

                        val tokenResponse = apiResponse.data
                        if (!apiResponse.success || tokenResponse == null) {
                            Napier.w("Token refresh failed - response missing token payload", tag = Tags.API)
                            tokenStorage.clearTokens()
                            return@refreshTokens null
                        }

                        val accessToken = tokenResponse.accessToken
                        val newRefreshToken = tokenResponse.refreshToken

                        if (accessToken.isBlank() || newRefreshToken.isBlank()) {
                            Napier.w("Token refresh failed - missing tokens in response body", tag = Tags.API)
                            tokenStorage.clearTokens()
                            return@refreshTokens null
                        }

                        tokenStorage.saveTokens(accessToken, newRefreshToken)
                        Napier.i("Token refreshed successfully", tag = Tags.API)

                        BearerTokens(accessToken = accessToken, refreshToken = newRefreshToken)
                    } catch (e: Exception) {
                        Napier.e("Token refresh failed: ${e.message}", e, tag = Tags.API)
                        if (e is io.ktor.client.plugins.ClientRequestException && e.response.status == HttpStatusCode.Unauthorized) {
                            tokenStorage.clearTokens()
                        }
                        null
                    }
                }
            }
        }
    }
}
