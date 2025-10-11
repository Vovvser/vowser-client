package com.vowser.client.auth

import com.sun.net.httpserver.HttpServer
import io.github.aakira.napier.Napier
import com.vowser.client.logging.Tags
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
        val authUrl = "$backendUrl/oauth2/authorization/naver"
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI(authUrl))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun startCallbackServer(onTokenReceived: (String, String) -> Unit) {
        this.onTokenReceivedCallback = onTokenReceived

        try {
            callbackServer = HttpServer.create(InetSocketAddress(CALLBACK_PORT), 0)
            callbackServer?.createContext(CALLBACK_PATH) { exchange ->
                try {
                    val cookieString = exchange.requestHeaders.getFirst("Cookie") ?: ""
                    val cookies = parseCookies(cookieString)

                    val accessToken = cookies["AccessToken"]
                    val refreshToken = cookies["RefreshToken"]

                    if (accessToken != null && refreshToken != null) {
                        val response = """
                            <!DOCTYPE html><html><head><title>Login Success</title></head>
                            <body>
                                <h1>로그인 성공!</h1><p>이제 이 창을 닫고 Vowser 앱으로 돌아가세요.</p>
                                <script>setTimeout(() => window.close(), 1000);</script>
                            </body></html>
                        """.trimIndent()

                        exchange.sendResponseHeaders(200, response.toByteArray().size.toLong())
                        exchange.responseBody.use { it.write(response.toByteArray()) }

                        onTokenReceivedCallback?.invoke(accessToken, refreshToken)

                        Thread {
                            Thread.sleep(500)
                            stopCallbackServer()
                        }.start()
                    } else {
                        val response = """
                            <!DOCTYPE html><html><head><title>Login Failed</title></head>
                            <body><h1>로그인 실패</h1><p>토큰을 받지 못했습니다. 다시 시도해주세요.</p></body></html>
                        """.trimIndent()

                        exchange.sendResponseHeaders(400, response.toByteArray().size.toLong())
                        exchange.responseBody.use { it.write(response.toByteArray()) }

                        Napier.w("Login failed - tokens not found in cookies", tag = Tags.AUTH)
                    }
                } catch (e: Exception) {
                    Napier.e("Error handling callback: ${e.message}", e, tag = Tags.AUTH)
                }
            }

            callbackServer?.executor = null
            callbackServer?.start()

            Napier.i("Callback server started on http://localhost:$CALLBACK_PORT$CALLBACK_PATH", tag = Tags.AUTH)
        } catch (e: Exception) {
            Napier.e("Failed to start callback server: ${e.message}", e, tag = Tags.AUTH)
        }
    }

    override fun stopCallbackServer() {
        callbackServer?.stop(0)
        callbackServer = null
        Napier.i("Callback server stopped", tag = Tags.AUTH)
    }

    private fun parseCookies(cookieString: String): Map<String, String> {
        return cookieString.split(";")
            .map { it.trim() }
            .filter { it.contains("=") }
            .associate {
                val parts = it.split("=", limit = 2)
                val name = URLDecoder.decode(parts[0], "UTF-8")
                val value = URLDecoder.decode(parts[1], "UTF-8")
                name to value
            }
    }
}