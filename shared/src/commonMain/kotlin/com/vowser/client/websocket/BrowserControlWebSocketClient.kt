package com.vowser.client.websocket

import com.vowser.client.websocket.dto.CallToolRequest
import com.vowser.client.websocket.dto.BrowserCommand
import com.vowser.client.websocket.dto.WebSocketMessage
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.websocket.*
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import com.vowserclient.shared.browserautomation.BrowserAutomationBridge
import kotlinx.coroutines.IO

class BrowserControlWebSocketClient {

    private val json = Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
        useAlternativeNames = false
        serializersModule = SerializersModule {
            polymorphic(WebSocketMessage::class) {
                subclass(WebSocketMessage.BrowserCommandWrapper::class)
                subclass(WebSocketMessage.NavigationPathWrapper::class)
            }
        }
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
                startReceivingMessages()
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
        return session!!.incoming.receiveAsFlow().mapNotNull {
            (it as? Frame.Text)?.readText()
        }
    }

    /**
     * 메시지를 수신하고 tool에 따라 다른 동작 수행
     */
    private fun startReceivingMessages() {
        CoroutineScope(Dispatchers.IO).launch {
            receiveMessages().collect { messageString ->
                Napier.i("Received raw message: $messageString", tag = "VowserSocketClient")
                try {
                    val message = json.decodeFromString<WebSocketMessage>(messageString)

                    when (message) {
                        is WebSocketMessage.BrowserCommandWrapper -> {
                            val command = message.data
                            Napier.i("Decoded command: $command", tag = "VowserSocketClient")
                            when (command) {
                                BrowserCommand.GoBack -> {
                                    Napier.i("Executing 'goBack' command.", tag = "VowserSocketClient")
                                    BrowserAutomationBridge.goBackInBrowser()
                                }
                                BrowserCommand.GoForward -> {
                                    Napier.i("Executing 'goForward' command.", tag = "VowserSocketClient")
                                    BrowserAutomationBridge.goForwardInBrowser()
                                }
                                is BrowserCommand.Navigate -> {
                                    Napier.i("Executing 'navigate' command to URL: ${command.url}", tag = "VowserSocketClient")
                                    BrowserAutomationBridge.navigateInBrowser(command.url)
                                }
                            }
                        }
                        is WebSocketMessage.NavigationPathWrapper -> {
                            val navigationPath = message.data
                            Napier.i("Decoded navigation path: $navigationPath", tag = "VowserSocketClient")
                            BrowserAutomationBridge.executeNavigationPath(navigationPath)
                        }
                    }
                } catch (e: Exception) {
                    Napier.e("Failed to parse or execute command from message: $messageString. Error: ${e.message}", e, tag = "VowserSocketClient")
                }
            }
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

    /**
     * 브라우저 명령어 기반 명령 전달이 필요할 때 사용
     */
    suspend fun sendBrowserCommand(command: BrowserCommand) {
        if (session?.isActive != true) {
            Napier.w("Not connected. Call connect() first.", tag = "VowserSocketClient")
            return
        }
        try {
            val jsonString = json.encodeToString(command)
            session?.send(Frame.Text(jsonString))
            Napier.i("Sent browser command: $command", tag = "VowserSocketClient")
        } catch (e: Exception) {
            Napier.e("Failed to send browser command: ${e.message}", e, tag = "VowserSocketClient")
        }
    }

    suspend fun close() {
        session?.close(CloseReason(CloseReason.Codes.NORMAL, "Client initiated disconnect"))
        session = null
        client.close()
        Napier.i("Connection closed.", tag = "VowserSocketClient")
    }
}