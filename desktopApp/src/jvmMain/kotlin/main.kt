import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.vowser.client.browserautomation.BrowserAutomationService
import io.github.aakira.napier.Napier
import com.vowser.client.logging.Tags
import kotlinx.coroutines.runBlocking
import java.net.ServerSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * Deep Link 수신을 위한 로컬 서버
 */
object DeepLinkServer {
    private var serverSocket: ServerSocket? = null
    var onOAuthCallback: (() -> Unit)? = null

    fun start() {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                serverSocket = ServerSocket(9876)
                Napier.i("Deep Link server started on port 9876", tag = Tags.AUTH)

                while (true) {
                    val client = serverSocket?.accept() ?: break
                    GlobalScope.launch(Dispatchers.IO) {
                        try {
                            val input = client.getInputStream().bufferedReader()
                            val requestLine = input.readLine()
                            Napier.d("Deep Link request: $requestLine", tag = Tags.AUTH)

                            // OAuth 성공 콜백 확인
                            if (requestLine?.contains("/oauth/callback") == true) {
                                Napier.i("OAuth callback received", tag = Tags.AUTH)
                                onOAuthCallback?.invoke()

                                // HTTP 응답 전송
                                val response = """
                                    HTTP/1.1 200 OK
                                    Content-Type: text/html; charset=utf-8

                                    <html>
                                    <body style="font-family: system-ui; text-align: center; padding: 50px;">
                                        <h1>로그인 성공!</h1>
                                        <p>이 창을 닫고 Vowser 앱으로 돌아가세요.</p>
                                        <script>setTimeout(() => window.close(), 2000);</script>
                                    </body>
                                    </html>
                                """.trimIndent()
                                client.getOutputStream().write(response.toByteArray())
                            }
                            client.close()
                        } catch (e: Exception) {
                            Napier.e("Error handling deep link request: ${e.message}", e, tag = Tags.AUTH)
                        }
                    }
                }
            } catch (e: Exception) {
                Napier.e("Deep Link server error: ${e.message}", e, tag = Tags.AUTH)
            }
        }
    }

    fun stop() {
        serverSocket?.close()
        serverSocket = null
        Napier.i("Deep Link server stopped", tag = Tags.AUTH)
    }
}

fun main() = application {
    DeepLinkServer.start()

    Window(onCloseRequest = {
        DeepLinkServer.stop()
        exitApplication()
    }) {
        App { viewModel ->
            DeepLinkServer.onOAuthCallback = {
                viewModel.handleOAuthCallback()
            }

            // ===== 임시 테스트: 여기에 브라우저에서 복사한 쿠키 붙여넣기 =====
            // 1. http://localhost:8080/test 에서 네이버 로그인
            // 2. F12 개발자 도구 → Application → Cookies → AccessToken 복사
            // 3. 아래 주석 해제하고 쿠키 값 붙여넣기

//            viewModel.setManualCookies(
//                accessToken = "임시용입니다. 토큰 입력해주세요"
//            )
        }
    }

    Runtime.getRuntime().addShutdownHook(Thread {
        runBlocking {
            try {
                DeepLinkServer.stop()
                BrowserAutomationService.cleanup()
                Napier.i("BrowserAutomationService 정리 완료.", tag = Tags.SYSTEM_SHUTDOWN)
            } catch (e: Exception) {
                Napier.e("BrowserAutomationService 정리 중 오류 발생: ${e.message}", e, tag = Tags.SYSTEM_SHUTDOWN)
            }
        }
    })
}