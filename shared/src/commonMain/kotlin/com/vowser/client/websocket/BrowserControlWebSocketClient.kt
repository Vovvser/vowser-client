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
import com.vowser.client.exception.ExceptionHandler
import com.vowser.client.exception.NetworkException
import com.vowser.client.exception.ContributionException
import com.vowser.client.logging.VowserLogger
import com.vowser.client.logging.Tags
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
            VowserLogger.info("Already connected.", Tags.NETWORK_WEBSOCKET)
            return
        }
        if (isConnecting) {
            VowserLogger.info("Connection in progress.", Tags.NETWORK_WEBSOCKET)
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
                VowserLogger.info("Successfully connected to ${VowserLogger.logUrl("ws://localhost:8080/control")}", Tags.NETWORK_WEBSOCKET)
                isConnecting = false
                VowserLogger.debug("Callback status - onAllPathsReceived: ${if (onAllPathsReceived != null) "SET" else "NULL"}", Tags.NETWORK_WEBSOCKET)
                startReceivingMessages()
                return
            } catch (e: Exception) {
                attempts++
                val networkException = NetworkException.ConnectionFailed(e)
                VowserLogger.error("Failed to connect (attempt $attempts/$maxRetries): ${e.message}", Tags.NETWORK_WEBSOCKET, e)

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
        VowserLogger.error("Failed to connect after $maxRetries attempts.", Tags.NETWORK_WEBSOCKET)
        session = null
    }

    suspend fun reconnect() {
        VowserLogger.info("Attempting to reconnect...", Tags.NETWORK_WEBSOCKET)
        close()
        connect()
    }

    /**
     * 활성 상태인 세션에서 텍스트 프레임만을 파싱하여 메시지를 flow로 수신
     * 세션이 활성 상태가 아니라면 빈 flow 반환
     */
    fun receiveMessages(): Flow<String> {
        if (session?.isActive != true) {
            VowserLogger.warn("Not connected. Call connect() first.", Tags.NETWORK_WEBSOCKET)
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
        VowserLogger.debug("=== DEBUG: startReceivingMessages() 호출됨 ===", Tags.NETWORK_WEBSOCKET)
        CoroutineScope(Dispatchers.IO).launch {
            receiveMessages().collect { messageString ->
                VowserLogger.debug("Received raw message: ${messageString.take(100)}...", Tags.NETWORK_WEBSOCKET)
                try {
                    val message = json.decodeFromString<WebSocketMessage>(messageString)

                    when (message) {
                        is WebSocketMessage.AllPathsWrapper -> {
                            val allPathsData = message.data
                            VowserLogger.info("Decoded all paths data for query: ${VowserLogger.logUserInput(allPathsData.query)}", Tags.NETWORK_WEBSOCKET)
                            VowserLogger.debug("onAllPathsReceived callback is ${if (onAllPathsReceived != null) "SET" else "NULL"}", Tags.NETWORK_WEBSOCKET)
                            onAllPathsReceived?.invoke(allPathsData)
                            VowserLogger.debug("onAllPathsReceived callback invoked", Tags.NETWORK_WEBSOCKET)
                        }

                        is WebSocketMessage.SearchPathResultWrapper -> {
                            val searchResult = message.data
                            VowserLogger.info("Search path result received. Query: ${VowserLogger.logUserInput(searchResult.query)}", Tags.NETWORK_WEBSOCKET)

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
                            VowserLogger.warn("Unhandled WebSocketMessage type received.", Tags.NETWORK_WEBSOCKET)
                        }
                    }
                } catch (e: Exception) {
                    if (messageString.contains("연결되었습니다") || messageString.contains("error\":false")) {
                        VowserLogger.debug("Received welcome or status message (ignored): ${messageString.take(50)}...", Tags.NETWORK_WEBSOCKET)
                    } else {
                        VowserLogger.error("Failed to parse or execute command from message: ${messageString.take(50)}... Error: ${e.message}", Tags.NETWORK_WEBSOCKET, e)
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
            VowserLogger.warn("Not connected. Call connect() first.", Tags.NETWORK_WEBSOCKET)
            return
        }
        try {
            val jsonString = json.encodeToString(request)
            session?.send(Frame.Text(jsonString))
            VowserLogger.info("Sent tool call: ${request.toolName}", Tags.NETWORK_WEBSOCKET)
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
            VowserLogger.warn("Not connected. Call connect() first.", Tags.NETWORK_WEBSOCKET)
            return
        }
        try {
            val jsonString = json.encodeToString(command)
            session?.send(Frame.Text(jsonString))
            VowserLogger.info("Sent browser command: $command", Tags.NETWORK_WEBSOCKET)
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
            VowserLogger.warn("Not connected. Call connect() first.", Tags.NETWORK_WEBSOCKET)
            return
        }
        try {
            val jsonString = json.encodeToString(message)
            session?.send(Frame.Text(jsonString))
            VowserLogger.info("Sent contribution message: sessionId=${VowserLogger.filterSensitiveData("sessionId=${message.sessionId}")}, steps=${message.steps.size}", Tags.CONTRIBUTION)
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
        currentNavigationJob?.cancel()
        session?.close(CloseReason(CloseReason.Codes.NORMAL, "Client initiated disconnect"))
        session = null
        client.close()
        VowserLogger.info("Connection closed.", Tags.NETWORK_WEBSOCKET)
    }
}