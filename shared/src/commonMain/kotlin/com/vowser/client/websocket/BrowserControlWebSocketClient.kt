package com.vowser.client.websocket

import com.vowser.client.websocket.dto.AllPathsResponse
import com.vowser.client.websocket.dto.CallToolRequest
import com.vowser.client.websocket.dto.BrowserCommand
import com.vowser.client.websocket.dto.WebSocketMessage
import com.vowser.client.websocket.dto.GraphUpdateData
import com.vowser.client.websocket.dto.VoiceProcessingResult
import com.vowser.client.websocket.dto.NavigationStep
import com.vowser.client.websocket.dto.PathDetail
import com.vowser.client.contribution.ContributionMessage
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
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job

class BrowserControlWebSocketClient {

    var onAllPathsReceived: ((AllPathsResponse) -> Unit)? = null
    var onGraphUpdateReceived: ((GraphUpdateData) -> Unit)? = null
    var onVoiceProcessingResultReceived: ((VoiceProcessingResult) -> Unit)? = null

    private var currentNavigationJob: Job? = null

    private val json = Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
        useAlternativeNames = false
        serializersModule = SerializersModule {
            polymorphic(WebSocketMessage::class) {
                subclass(WebSocketMessage.AllPathsWrapper::class)
                subclass(WebSocketMessage.BrowserCommandWrapper::class)
                subclass(WebSocketMessage.GraphUpdateWrapper::class)
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
                Napier.i("Callback status - onAllPathsReceived: ${if (onAllPathsReceived != null) "SET" else "NULL"}", tag = "VowserSocketClient")
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
        Napier.i("=== DEBUG: startReceivingMessages() 호출됨 ===", tag = "VowserSocketClient")
        CoroutineScope(Dispatchers.IO).launch {
            receiveMessages().collect { messageString ->
                Napier.i("Received raw message: $messageString", tag = "VowserSocketClient")
                try {
                    val message = json.decodeFromString<WebSocketMessage>(messageString)

                    when (message) {
                        is WebSocketMessage.AllPathsWrapper -> {
                            val allPathsData = message.data
                            Napier.i("Decoded all paths data for query: ${allPathsData.query}", tag = "VowserSocketClient")
                            Napier.i("onAllPathsReceived callback is ${if (onAllPathsReceived != null) "SET" else "NULL"}", tag = "VowserSocketClient")
                            onAllPathsReceived?.invoke(allPathsData)
                            Napier.i("onAllPathsReceived callback invoked", tag = "VowserSocketClient")
                        }

                        is WebSocketMessage.SearchPathResultWrapper -> {
                            val searchResult = message.data
                            Napier.i("Search path result received. Query: ${searchResult.query}", tag = "VowserSocketClient")

                            val pathDetails = searchResult.matched_paths.map { matchedPath ->
                                PathDetail(
                                    pathId = matchedPath.pathId,
                                    score = matchedPath.score,
                                    total_weight = matchedPath.total_weight,
                                    last_used = null,
                                    estimated_time = null,
                                    steps = matchedPath.steps.map { step ->
                                        NavigationStep(
                                            url = step.url,
                                            title = step.title,
                                            action = step.action,
                                            selector = step.selector
                                        )
                                    }
                                )
                            }

                            val allPathsResponse = AllPathsResponse(
                                query = searchResult.query,
                                paths = pathDetails
                            )

                            onAllPathsReceived?.invoke(allPathsResponse)
                        }

                        else -> {
                            Napier.w("Unhandled WebSocketMessage type received.", tag = "VowserSocketClient")
                        }
                    }
                } catch (e: Exception) {
                    if (messageString.contains("연결되었습니다") || messageString.contains("error\":false")) {
                        Napier.i("Received welcome or status message (ignored): $messageString", tag = "VowserSocketClient")
                    } else {
                        Napier.e("Failed to parse or execute command from message: $messageString. Error: ${e.message}", e, tag = "VowserSocketClient")
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

    /**
     * 기여 메시지 전송
     */
    suspend fun sendContributionMessage(message: ContributionMessage) {
        if (session?.isActive != true) {
            Napier.w("Not connected. Call connect() first.", tag = "VowserSocketClient")
            return
        }
        try {
            val jsonString = json.encodeToString(message)
            session?.send(Frame.Text(jsonString))
            Napier.i("Sent contribution message: sessionId=${message.sessionId}, steps=${message.steps.size}", tag = "VowserSocketClient")
        } catch (e: Exception) {
            Napier.e("Failed to send contribution message: ${e.message}", e, tag = "VowserSocketClient")
            throw e
        }
    }

    suspend fun close() {
        currentNavigationJob?.cancel()
        session?.close(CloseReason(CloseReason.Codes.NORMAL, "Client initiated disconnect"))
        session = null
        client.close()
        Napier.i("Connection closed.", tag = "VowserSocketClient")
    }
}