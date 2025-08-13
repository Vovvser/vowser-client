package com.vowser.client

import com.vowser.client.websocket.BrowserControlWebSocketClient
import com.vowser.client.websocket.ConnectionStatus
import com.vowser.client.websocket.dto.CallToolRequest
import com.vowser.client.websocket.dto.VoiceProcessingResult
import com.vowser.client.data.SpeechRepository
import io.github.aakira.napier.Napier
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.benasher44.uuid.uuid4
import com.vowser.client.data.GraphDataConverter
import com.vowser.client.visualization.GraphVisualizationData
import com.vowser.client.websocket.dto.NavigationPath
import com.vowserclient.shared.browserautomation.BrowserAutomationBridge
import kotlinx.coroutines.delay

class AppViewModel(private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default)) {

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.Disconnected)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _receivedMessage = MutableStateFlow("No message")
    val receivedMessage: StateFlow<String> = _receivedMessage.asStateFlow()

    private val webSocketClient = BrowserControlWebSocketClient()
    private var messageCollectionJob: Job? = null

    // 음성 녹음 관련
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingStatus = MutableStateFlow("Ready to record")
    val recordingStatus: StateFlow<String> = _recordingStatus.asStateFlow()

    // 그래프 상태 관리
    private val _currentGraphData = MutableStateFlow<GraphVisualizationData?>(null)
    val currentGraphData: StateFlow<GraphVisualizationData?> = _currentGraphData.asStateFlow()

    private val _lastVoiceResult = MutableStateFlow<VoiceProcessingResult?>(null)
    val lastVoiceResult: StateFlow<VoiceProcessingResult?> = _lastVoiceResult.asStateFlow()

    private val _graphLoading = MutableStateFlow(false)
    val graphLoading: StateFlow<Boolean> = _graphLoading.asStateFlow()

    private val speechRepository = SpeechRepository(HttpClient(CIO))
    val sessionId = uuid4().toString()

    init {
        setupWebSocketCallbacks()
        connectWebSocket()
    }

    private fun connectWebSocket() {
        coroutineScope.launch {
            _connectionStatus.value = ConnectionStatus.Connecting
            try {
                webSocketClient.connect()
                _connectionStatus.value = ConnectionStatus.Connected
                // startMessageCollection() 제거 - BrowserControlWebSocketClient에서 자체적으로 메시지 처리
            } catch (e: Exception) {
                Napier.e("ViewModel: Failed to connect WebSocket: ${e.message}", e)
                _connectionStatus.value = ConnectionStatus.Error
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
            // startMessageCollection() 제거 - BrowserControlWebSocketClient에서 자체적으로 메시지 처리
        }
    }

    fun closeConnection() {
        coroutineScope.launch {
            webSocketClient.close()
            _connectionStatus.value = ConnectionStatus.Disconnected
            messageCollectionJob?.cancel()
            speechRepository.close()
        }
    }

    fun toggleRecording() {
        coroutineScope.launch(Dispatchers.IO) {
            if (_isRecording.value) {
                stopRecordingImpl()
            } else {
                startRecordingImpl()
            }
        }
    }

    private suspend fun startRecordingImpl() {
        _recordingStatus.value = "Starting recording..."
        val success = startPlatformRecording()
        if (success) {
            _isRecording.value = true
            _recordingStatus.value = "Recording..."
            Napier.i("Recording started successfully", tag = "AppViewModel")
        } else {
            _recordingStatus.value = "Failed to start recording"
            Napier.e("Failed to start recording", tag = "AppViewModel")
        }
    }

    private suspend fun stopRecordingImpl() {
        _recordingStatus.value = "Stopping recording..."
        _isRecording.value = false
        
        val audioBytes = stopPlatformRecording()
        if (audioBytes != null) {
            _recordingStatus.value = "Uploading audio..."
            try {
                val result = speechRepository.transcribeAudio(audioBytes, sessionId)
                result.fold(
                    onSuccess = { response ->
                        _recordingStatus.value = "Audio processed successfully"
                        Napier.i("Audio transcription result: $response", tag = "AppViewModel")
                    },
                    onFailure = { error ->
                        _recordingStatus.value = "Failed to process audio: ${error.message}"
                        Napier.e("Audio transcription failed: ${error.message}", tag = "AppViewModel")
                    }
                )
            } catch (e: Exception) {
                _recordingStatus.value = "Error processing audio: ${e.message}"
                Napier.e("Error processing audio: ${e.message}", e, tag = "AppViewModel")
            }
        } else {
            _recordingStatus.value = "No audio data recorded"
        }

        delay(3000)
        _recordingStatus.value = "Ready to record"
    }

    /**
     * WebSocket 콜백 설정
     */
    private fun setupWebSocketCallbacks() {
        // [신규] 모든 경로 정보를 받았을 때의 콜백 설정
        // (BrowserControlWebSocketClient 내부에 onAllPathsReceived 콜백 속성 추가 필요)
        Napier.i("Setting up WebSocket callbacks", tag = "AppViewModel")
        webSocketClient.onAllPathsReceived = { allPaths ->
            coroutineScope.launch {
                Napier.i("Received all paths for query: ${allPaths.query}", tag = "AppViewModel")

                // 1. 그래프 UI 업데이트
                // AllPathsResponse를 시각화 데이터로 변환 (GraphDataConverter에 새 함수 추가 필요)
                val visualizationData = GraphDataConverter.convertFromAllPaths(allPaths)
                Napier.i("Graph visualization data created - Nodes: ${visualizationData.nodes.size}, Edges: ${visualizationData.edges.size}", tag = "AppViewModel")
                _currentGraphData.value = visualizationData
                _graphLoading.value = false
                Napier.i("Graph data updated and loading set to false", tag = "AppViewModel")

                // 첫번째 경로 자동 실행 (가중치가 가장 높음)
                val firstPath = allPaths.paths.firstOrNull()
                if (firstPath != null) {
                    Napier.i("Auto-executing the first path: ${firstPath.pathId}", tag = "AppViewModel")
                    try {
                        val navigationPath = NavigationPath(
                            pathId = firstPath.pathId,
                            steps = firstPath.steps,
                            description = "Auto-executed path from voice command"
                        )
                        BrowserAutomationBridge.executeNavigationPath(navigationPath)
                        Napier.i("Successfully started automation for path: ${firstPath.pathId}", tag = "AppViewModel")
                    } catch (e: Exception) {
                        Napier.e("Failed to execute navigation path: ${e.message}", e, tag = "AppViewModel")
                    }
                }
            }
        }

        webSocketClient.onVoiceProcessingResultReceived = { voiceResult ->
            coroutineScope.launch {
                Napier.i("Received voice processing result: ${voiceResult.transcript}", tag = "AppViewModel")
                _lastVoiceResult.value = voiceResult

                if (voiceResult.success) {
                    _recordingStatus.value = "Voice processed: ${voiceResult.transcript}"
                    _graphLoading.value = true
                } else {
                    _recordingStatus.value = "Voice processing failed: ${voiceResult.error?.message ?: "Unknown error"}"
                }
            }
        }
        Napier.i("WebSocket callbacks setup completed", tag = "AppViewModel")
    }

    /**
     * 그래프 새로고침 요청
     */
    fun refreshGraph() {
        coroutineScope.launch {
            _graphLoading.value = true
            try {
                webSocketClient.sendToolCall(CallToolRequest("refresh_graph", mapOf(
                    "sessionId" to sessionId
                )))
                Napier.i("Graph refresh requested", tag = "AppViewModel")
            } catch (e: Exception) {
                Napier.e("Failed to request graph refresh: ${e.message}", e, tag = "AppViewModel")
                _graphLoading.value = false
            }
        }
    }

    /**
     * 특정 노드로 탐색 요청
     */
    fun navigateToNode(nodeId: String) {
        coroutineScope.launch {
            try {
                webSocketClient.sendToolCall(CallToolRequest("navigate_to_node", mapOf(
                    "sessionId" to sessionId,
                    "nodeId" to nodeId
                )))
                Napier.i("Navigation to node $nodeId requested", tag = "AppViewModel")
            } catch (e: Exception) {
                Napier.e("Failed to navigate to node: ${e.message}", e, tag = "AppViewModel")
            }
        }
    }
}

expect suspend fun startPlatformRecording(): Boolean
expect suspend fun stopPlatformRecording(): ByteArray?