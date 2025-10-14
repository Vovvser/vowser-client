package com.vowser.client.auth

import com.vowser.client.data.AuthRepository
import io.github.aakira.napier.Napier
import com.vowser.client.logging.Tags
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.awt.Desktop
import java.net.URI

class AuthManagerDesktop(
    private val backendUrl: String = "http://localhost:8080",
    private val authRepository: AuthRepository
) : AuthManager {
    private var onTokenReceivedCallback: ((String, String) -> Unit)? = null
    private var callbackServer: NettyApplicationEngine? = null

    companion object {
        private const val CALLBACK_PORT = 8888
    }

    override fun login() {
        val authUrl = "$backendUrl/oauth2/authorization/naver"
        try {
            // 시스템 기본 브라우저로 OAuth 로그인 페이지 열기
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI(authUrl))
                Napier.i("Opened OAuth login page in system browser", tag = Tags.AUTH)
            } else {
                Napier.e("Desktop browsing not supported", tag = Tags.AUTH)
            }
        } catch (e: Exception) {
            Napier.e("Failed to open browser: ${e.message}", e, tag = Tags.AUTH)
        }
    }

    override fun startCallbackServer(onTokenReceived: (String, String) -> Unit) {
        this.onTokenReceivedCallback = onTokenReceived

        try {
            callbackServer = embeddedServer(Netty, port = CALLBACK_PORT) {
                routing {
                    get("/auth/callback") {
                        val code = call.request.queryParameters["code"]

                        if (code != null) {
                            Napier.i("Authorization code received: $code", tag = Tags.AUTH)

                            // 사용자에게 성공 메시지 표시
                            call.respondText("로그인 성공! 이 창을 닫으셔도 됩니다.")

                            // 코드를 토큰으로 교환
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    val result = authRepository.exchangeCodeForToken(code)
                                    result.onSuccess { tokenResponse ->
                                        Napier.i("Token exchange successful", tag = Tags.AUTH)
                                        onTokenReceivedCallback?.invoke(
                                            tokenResponse.accessToken,
                                            tokenResponse.refreshToken
                                        )

                                        // 서버 종료
                                        Thread {
                                            Thread.sleep(2000)
                                            stopCallbackServer()
                                        }.start()
                                    }.onFailure { e ->
                                        Napier.e("Token exchange failed: ${e.message}", e, tag = Tags.AUTH)
                                    }
                                } catch (e: Exception) {
                                    Napier.e("Error exchanging code for token: ${e.message}", e, tag = Tags.AUTH)
                                }
                            }
                        } else {
                            Napier.w("No authorization code in callback", tag = Tags.AUTH)
                            call.respondText("로그인 실패: 인증 코드가 없습니다.")
                        }
                    }
                }
            }.start(wait = false)

            Napier.i("Callback server started on port $CALLBACK_PORT", tag = Tags.AUTH)
        } catch (e: Exception) {
            Napier.e("Failed to start callback server: ${e.message}", e, tag = Tags.AUTH)
        }
    }

    override fun stopCallbackServer() {
        try {
            callbackServer?.stop(1000, 2000)
            callbackServer = null
            Napier.i("Callback server stopped", tag = Tags.AUTH)
        } catch (e: Exception) {
            Napier.e("Error stopping callback server: ${e.message}", e, tag = Tags.AUTH)
        }
    }
}