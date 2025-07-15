package com.vowser.client

import com.vowser.client.websocket.BrowserControlWebSocketClient
import com.vowser.client.websocket.ConnectionStatus
import com.vowser.client.websocket.dto.CallToolRequest
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AppViewModel(private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default)) {

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.Disconnected)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _receivedMessage = MutableStateFlow("No message")
    val receivedMessage: StateFlow<String> = _receivedMessage.asStateFlow()

    private val webSocketClient = BrowserControlWebSocketClient()
    private var messageCollectionJob: Job? = null

    init {
        connectWebSocket()
    }

    private fun connectWebSocket() {
        coroutineScope.launch {
            _connectionStatus.value = ConnectionStatus.Connecting
            try {
                webSocketClient.connect()
                _connectionStatus.value = ConnectionStatus.Connected
                startMessageCollection()
            } catch (e: Exception) {
                Napier.e("ViewModel: Failed to connect WebSocket: ${e.message}", e)
                _connectionStatus.value = ConnectionStatus.Error
            }
        }
    }

    private fun startMessageCollection() {
        messageCollectionJob?.cancel()
        messageCollectionJob = coroutineScope.launch {
            webSocketClient.receiveMessages().collect {
                _receivedMessage.value = it
                Napier.i("ViewModel: Received message: $it")
            }
        }
    }

    fun sendToolCall(toolName: String, args: Map<String, String>) {
        coroutineScope.launch {
            webSocketClient.sendToolCall(CallToolRequest(toolName, args))
        }
    }

    fun reconnect() {
        coroutineScope.launch {
            webSocketClient.reconnect()
            _connectionStatus.value = ConnectionStatus.Connecting
            // 재연결을 성공하면 message collection 재시작
            startMessageCollection()
        }
    }

    fun closeConnection() {
        coroutineScope.launch {
            webSocketClient.close()
            _connectionStatus.value = ConnectionStatus.Disconnected
            messageCollectionJob?.cancel()
        }
    }
}
