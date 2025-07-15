package com.vowser.client.websocket

import com.vowser.client.websocket.dto.CallToolRequest
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.websocket.*
import io.github.aakira.napier.Napier
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class BrowserControlWebSocketClient {

    private val json = Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
        useAlternativeNames = false
    }

    private val client = HttpClient {
        install(WebSockets)
        install(ContentNegotiation) {
            json(json)
        }
    }

    private var session: DefaultWebSocketSession? = null
    private var isConnecting = false

    /**
     * 웹 소켓 연결 실행
     *
     * @param maxRetries 최대 재시도 횟수, 기본값 5
     * @param retryDelayMillis 재시도 간격(밀리초), 기본값 2000ms
     */
    suspend fun connect(maxRetries: Int = 5, retryDelayMillis: Long = 2000L) {
        if (session != null && session?.isActive == true) {
            Napier.i("Already connected.", tag = "VowserSocketClient")
            return
        }
        if (isConnecting) {
            Napier.i("Connection in progress.", tag = "VowserSocketClient")
            return
        }

        isConnecting = true
        var attempts = 0
        while (attempts < maxRetries) {
            try {
                session = client.webSocketSession(
                    host = "localhost",
                    port = 8080,
                    path = "/control"
                )
                Napier.i("Successfully connected to ws://localhost:8080/control", tag = "VowserSocketClient")
                isConnecting = false
                return
            } catch (e: Exception) {
                attempts++
                Napier.e("Failed to connect (attempt $attempts/$maxRetries): ${e.message}", e, tag = "VowserSocketClient")
                if (attempts < maxRetries) {
                    delay(retryDelayMillis)
                }
            }
        }
        isConnecting = false
        Napier.e("Failed to connect after $maxRetries attempts.", tag = "VowserSocketClient")
        session = null
    }

    suspend fun reconnect() {
        Napier.i("Attempting to reconnect...", tag = "VowserSocketClient")
        close()
        connect()
    }

    /**
     * 활성 상태인 세션에서 텍스트 프레임만을 파싱하여 메시지를 flow로 수신
     * 세션이 활성 상태가 아니라면 빈 flow 반환
     */
    fun receiveMessages(): Flow<String> {
        if (session?.isActive != true) {
            Napier.w("Not connected. Call connect() first.", tag = "VowserSocketClient")
            return emptyFlow()
        }
        return session!!.incoming.receiveAsFlow().map {
            (it as? Frame.Text)?.readText() ?: ""
        }
    }

    /**
     * 활성 상태인 세션으로 도구 호출 요청을 전송
     * JSON 형식으로 인코딩 하여 텍스트 프레임으로 전송
     */
    suspend fun sendToolCall(request: CallToolRequest) {
        if (session?.isActive != true) {
            Napier.w("Not connected. Call connect() first.", tag = "VowserSocketClient")
            return
        }
        try {
            val jsonString = json.encodeToString(request)
            session?.send(Frame.Text(jsonString))
            Napier.i("Sent tool call: ${request.toolName}", tag = "VowserSocketClient")
        } catch (e: Exception) {
            Napier.e("Failed to send message: ${e.message}", e, tag = "VowserSocketClient")
        }
    }

    suspend fun close() {
        session?.close(CloseReason(CloseReason.Codes.NORMAL, "Client initiated disconnect"))
        session = null
        client.close()
        Napier.i("Connection closed.", tag = "VowserSocketClient")
    }
}