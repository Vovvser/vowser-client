package com.vowser.client.websocket

import com.vowser.client.websocket.dto.AllPathsResponse
import com.vowser.client.websocket.dto.CallToolRequest
import com.vowser.client.websocket.dto.BrowserCommand
import com.vowser.client.websocket.dto.WebSocketMessage
import com.vowser.client.websocket.dto.VoiceProcessingResult
import com.vowser.client.websocket.dto.MatchedPath
import com.vowser.client.contribution.ContributionMessage
import com.vowser.client.exception.ExceptionHandler
import com.vowser.client.exception.NetworkException
import com.vowser.client.exception.ContributionException
import io.github.aakira.napier.Napier
import com.vowser.client.logging.Tags
import com.vowser.client.logging.LogUtils
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.websocket.*
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
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job

class BrowserControlWebSocketClient(
    private val exceptionHandler: ExceptionHandler
) {

    companion object {
        private val json = Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
            useAlternativeNames = false
            serializersModule = SerializersModule {
                polymorphic(WebSocketMessage::class) {
                    subclass(WebSocketMessage.AllPathsWrapper::class)
                    subclass(WebSocketMessage.BrowserCommandWrapper::class)
                    subclass(WebSocketMessage.VoiceProcessingResultWrapper::class)
                    subclass(WebSocketMessage.ErrorWrapper::class)
                    subclass(WebSocketMessage.SearchPathResultWrapper::class)
                }
                polymorphic(BrowserCommand::class) {
                    subclass(BrowserCommand.Navigate::class)
                    subclass(BrowserCommand.GoBack::class)
                    subclass(BrowserCommand.GoForward::class)
                }
            }
        }
    }

    var onAllPathsReceived: ((AllPathsResponse) -> Unit)? = null
    var onVoiceProcessingResultReceived: ((VoiceProcessingResult) -> Unit)? = null
    var onSearchResultReceived: ((List<MatchedPath>, String) -> Unit)? = null
    var onConnectionOpened: (() -> Unit)? = null
    var onConnectionClosed: ((CloseReason?) -> Unit)? = null

    private var currentNavigationJob: Job? = null
    private var messageReceivingJob: Job? = null

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
            Napier.i("Already connected.", tag = Tags.NETWORK_WEBSOCKET)
            return
        }
        if (isConnecting) {
            Napier.i("Connection in progress.", tag = Tags.NETWORK_WEBSOCKET)
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
                Napier.i("Successfully connected to ws://localhost:8080/control", tag = Tags.NETWORK_WEBSOCKET)
                isConnecting = false
                onConnectionOpened?.invoke()
                Napier.d("Callback status - onAllPathsReceived: ${if (onAllPathsReceived != null) "SET" else "NULL"}", tag = Tags.NETWORK_WEBSOCKET)
                startReceivingMessages()
                CoroutineScope(Dispatchers.IO).launch {
                    val reason = runCatching { session?.closeReason?.await() }.getOrNull()
                    if (reason != null) {
                        onConnectionClosed?.invoke(reason)
                    }
                }
                return
            } catch (e: Exception) {
                attempts++
                val networkException = NetworkException.ConnectionFailed(e)
                Napier.e("Failed to connect (attempt $attempts/$maxRetries): ${e.message}", e, tag = Tags.NETWORK_WEBSOCKET)

                if (attempts >= maxRetries) {
                    exceptionHandler.handleException(
                        networkException,
                        context = "WebSocket connection"
                    ) {
                        connect(maxRetries, retryDelayMillis)
                    }
                    return
                }

                if (attempts < maxRetries) {
                    delay(retryDelayMillis)
                }
            }
        }
        isConnecting = false
        Napier.e("Failed to connect after $maxRetries attempts.", tag = Tags.NETWORK_WEBSOCKET)
        session = null
    }

    suspend fun reconnect() {
        Napier.i("Attempting to reconnect...", tag = Tags.NETWORK_WEBSOCKET)
        close()
        connect()
    }

    /**
     * 활성 상태인 세션에서 텍스트 프레임만을 파싱하여 메시지를 flow로 수신
     * 세션이 활성 상태가 아니라면 빈 flow 반환
     */
    fun receiveMessages(): Flow<String> {
        if (session?.isActive != true) {
            Napier.w("Not connected. Call connect() first.", tag = Tags.NETWORK_WEBSOCKET)
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
        Napier.d("=== DEBUG: startReceivingMessages() 호출됨 ===", tag = Tags.NETWORK_WEBSOCKET)
        messageReceivingJob = CoroutineScope(Dispatchers.IO).launch {
            receiveMessages().collect { messageString ->
                Napier.d("Received raw message: ${messageString.take(100)}...", tag = Tags.NETWORK_WEBSOCKET)
                try {
                    // 필수 키가 없는 단순 텍스트/상태 메시지는 무시 (백엔드 콘솔/상태 에코)
                    val looksLikeProtocol = messageString.contains("\"status\"") || messageString.contains("\"type\"") || messageString.contains("\"data\"")
                    val looksLikeTextOnly = messageString.contains("\"content\"") && messageString.contains("\"text\"") && messageString.contains("\"error\"")
                    if (!looksLikeProtocol && looksLikeTextOnly) {
                        Napier.d("Non-protocol text message (ignored): ${messageString.take(60)}...", tag = Tags.NETWORK_WEBSOCKET)
                        return@collect
                    }
                    val message = json.decodeFromString<WebSocketMessage>(messageString)

                    when (message) {
                        is WebSocketMessage.AllPathsWrapper -> {
                            Napier.i("All paths received for query: ${message.data.query}", tag = Tags.NETWORK_WEBSOCKET)
                            onAllPathsReceived?.invoke(message.data)
                        }

                        is WebSocketMessage.SearchPathResultWrapper -> {
                            Napier.i("Search result: ${message.data.total_matched} paths for '${message.data.query}'", tag = Tags.NETWORK_WEBSOCKET)
                            onSearchResultReceived?.invoke(message.data.matched_paths, message.data.query)
                        }

                        is WebSocketMessage.VoiceProcessingResultWrapper -> {
                            Napier.i("Voice result: ${message.data.transcript ?: "N/A"} (success=${message.data.success})", tag = Tags.NETWORK_WEBSOCKET)
                            onVoiceProcessingResultReceived?.invoke(message.data)
                        }

                        is WebSocketMessage.BrowserCommandWrapper -> {
                            Napier.i("Browser command: ${message.data::class.simpleName}", tag = Tags.NETWORK_WEBSOCKET)
                        }

                        is WebSocketMessage.ErrorWrapper -> {
                            Napier.e("WebSocket error: ${message.data.message}", tag = Tags.NETWORK_WEBSOCKET)
                        }
                    }
                } catch (e: Exception) {
                    if (messageString.contains("연결되었습니다") || messageString.contains("error\":false") || messageString.contains("\"content\"")) {
                        Napier.d("Received welcome or status message (ignored): ${messageString.take(50)}...", tag = Tags.NETWORK_WEBSOCKET)
                    } else {
                        Napier.e("Failed to parse or execute command from message: ${messageString.take(50)}... Error: ${e.message}", e, tag = Tags.NETWORK_WEBSOCKET)
                    }
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
            Napier.w("Not connected. Call connect() first.", tag = Tags.NETWORK_WEBSOCKET)
            return
        }
        try {
            val jsonString = json.encodeToString(request)
            session?.send(Frame.Text(jsonString))
            Napier.i("Sent tool call: ${request.toolName}", tag = Tags.NETWORK_WEBSOCKET)
        } catch (e: Exception) {
            exceptionHandler.handleException(
                NetworkException.ConnectionFailed(e),
                context = "Tool call transmission"
            ) {
                sendToolCall(request)
            }
        }
    }

    /**
     * 브라우저 명령어 기반 명령 전달이 필요할 때 사용
     */
    suspend fun sendBrowserCommand(command: BrowserCommand) {
        if (session?.isActive != true) {
            Napier.w("Not connected. Call connect() first.", tag = Tags.NETWORK_WEBSOCKET)
            return
        }
        try {
            val jsonString = json.encodeToString(command)
            session?.send(Frame.Text(jsonString))
            Napier.i("Sent browser command: $command", tag = Tags.NETWORK_WEBSOCKET)
        } catch (e: Exception) {
            exceptionHandler.handleException(
                NetworkException.ConnectionFailed(e),
                context = "Browser command transmission"
            ) {
                sendBrowserCommand(command)
            }
        }
    }

    /**
     * 기여 메시지 전송
     */
    suspend fun sendContributionMessage(message: ContributionMessage) {
        if (session?.isActive != true) {
            Napier.w("Not connected. Call connect() first.", tag = Tags.NETWORK_WEBSOCKET)
            return
        }
        try {
            val jsonString = json.encodeToString(message)
            session?.send(Frame.Text(jsonString))
            Napier.i("Sent contribution message: sessionId=${LogUtils.filterSensitive(message.sessionId)}, steps=${message.steps.size}", tag = Tags.CONTRIBUTION)
        } catch (e: Exception) {
            exceptionHandler.handleException(
                ContributionException.DataTransmissionFailed(e),
                context = "Contribution data transmission"
            ) {
                sendContributionMessage(message)
            }
        }
    }

    suspend fun close() {
        try {
            Napier.i("Starting connection cleanup...", tag = Tags.NETWORK_WEBSOCKET)

            currentNavigationJob?.cancel()
            messageReceivingJob?.cancel()
            delay(100)
            session?.let { activeSession ->
                if (activeSession.isActive) {
                    activeSession.close(CloseReason(CloseReason.Codes.GOING_AWAY, "Client disconnect"))
                    Napier.d("WebSocket session closed with GOING_AWAY reason", tag = Tags.NETWORK_WEBSOCKET)
                }
            }
            session = null
            isConnecting = false
            onAllPathsReceived = null
            onVoiceProcessingResultReceived = null
            client.close()
            Napier.i("Connection closed and all resources cleaned up successfully.", tag = Tags.NETWORK_WEBSOCKET)
            onConnectionClosed?.invoke(null)
        } catch (e: Exception) {
            Napier.e("Error during resource cleanup: ${e.message}", e, tag = Tags.NETWORK_WEBSOCKET)
            session = null
            isConnecting = false
            throw e
        }
    }
}
