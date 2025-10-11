package com.vowser.client.auth

import com.sun.net.httpserver.HttpServer
import java.awt.Desktop
import java.net.InetSocketAddress
import java.net.URI
import java.net.URLDecoder

class AuthManagerDesktop(private val backendUrl: String = "http://localhost:8080") : AuthManager {
    private var callbackServer: HttpServer? = null
    private var onTokenReceivedCallback: ((String, String) -> Unit)? = null

    companion object {
        private const val CALLBACK_PORT = 8888
        private const val CALLBACK_PATH = "/auth/callback"
    }

    override fun login() {
        // 백엔드의 OAuth2 인증 URL로 브라우저를 열기
        val authUrl = "$backendUrl/oauth2/authorization/naver"

        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI(authUrl))
                println("AuthManagerDesktop: Opening browser for OAuth2 login: $authUrl")
            } else {
                println("AuthManagerDesktop: Desktop browsing not supported. Please navigate to: $authUrl")
            }
        } catch (e: Exception) {
            println("AuthManagerDesktop: Failed to open browser: ${e.message}")
            e.printStackTrace()
        }
    }

    override fun startCallbackServer(onTokenReceived: (String, String) -> Unit) {
        this.onTokenReceivedCallback = onTokenReceived

        try {
            // HTTP 서버 생성 및 시작
            callbackServer = HttpServer.create(InetSocketAddress(CALLBACK_PORT), 0)

            callbackServer?.createContext(CALLBACK_PATH) { exchange ->
                try {
                    val query = exchange.requestURI.query ?: ""
                    val params = parseQueryString(query)

                    val accessToken = params["access_token"]
                    val refreshToken = params["refresh_token"]

                    if (accessToken != null && refreshToken != null) {
                        // 성공 페이지 응답
                        val response = """
                            <!DOCTYPE html>
                            <html>
                            <head><title>Login Success</title></head>
                            <body>
                                <h1>로그인 성공!</h1>
                                <p>이제 이 창을 닫고 Vowser 앱으로 돌아가세요.</p>
                                <script>setTimeout(() => window.close(), 2000);</script>
                            </body>
                            </html>
                        """.trimIndent()

                        exchange.sendResponseHeaders(200, response.toByteArray().size.toLong())
                        exchange.responseBody.write(response.toByteArray())
                        exchange.responseBody.close()

                        // 토큰 콜백 실행
                        onTokenReceivedCallback?.invoke(accessToken, refreshToken)
                        println("AuthManagerDesktop: Tokens received successfully")

                        // 서버 종료 (약간의 딜레이 후)
                        Thread {
                            Thread.sleep(1000)
                            stopCallbackServer()
                        }.start()
                    } else {
                        // 에러 페이지 응답
                        val response = """
                            <!DOCTYPE html>
                            <html>
                            <head><title>Login Failed</title></head>
                            <body>
                                <h1>로그인 실패</h1>
                                <p>토큰을 받지 못했습니다. 다시 시도해주세요.</p>
                            </body>
                            </html>
                        """.trimIndent()

                        exchange.sendResponseHeaders(400, response.toByteArray().size.toLong())
                        exchange.responseBody.write(response.toByteArray())
                        exchange.responseBody.close()

                        println("AuthManagerDesktop: Login failed - tokens not found in callback")
                    }
                } catch (e: Exception) {
                    println("AuthManagerDesktop: Error handling callback: ${e.message}")
                    e.printStackTrace()
                }
            }

            callbackServer?.executor = null // 기본 executor 사용
            callbackServer?.start()

            println("AuthManagerDesktop: Callback server started on http://localhost:$CALLBACK_PORT$CALLBACK_PATH")
        } catch (e: Exception) {
            println("AuthManagerDesktop: Failed to start callback server: ${e.message}")
            e.printStackTrace()
        }
    }

    override fun stopCallbackServer() {
        callbackServer?.stop(0)
        callbackServer = null
        println("AuthManagerDesktop: Callback server stopped")
    }

    private fun parseQueryString(query: String): Map<String, String> {
        return query.split("&")
            .mapNotNull { param ->
                val parts = param.split("=", limit = 2)
                if (parts.size == 2) {
                    URLDecoder.decode(parts[0], "UTF-8") to URLDecoder.decode(parts[1], "UTF-8")
                } else {
                    null
                }
            }
            .toMap()
    }
}