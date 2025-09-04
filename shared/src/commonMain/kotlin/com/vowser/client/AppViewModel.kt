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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.benasher44.uuid.uuid4
import com.vowser.client.data.GraphDataConverter
import com.vowser.client.visualization.GraphVisualizationData
import com.vowser.client.websocket.dto.NavigationPath
import com.vowser.client.websocket.dto.AllPathsResponse
import com.vowser.client.websocket.dto.PathDetail
import com.vowserclient.shared.browserautomation.BrowserAutomationBridge
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime


data class StatusLogEntry(
    val timestamp: String,
    val message: String,
    val type: StatusLogType = StatusLogType.INFO
)

enum class StatusLogType {
    INFO, SUCCESS, WARNING, ERROR
}

class AppViewModel(private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default)) {

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.Disconnected)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _receivedMessage = MutableStateFlow("No message")
    val receivedMessage: StateFlow<String> = _receivedMessage.asStateFlow()

    private val webSocketClient = BrowserControlWebSocketClient()

    // 음성 녹음 관련
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingStatus = MutableStateFlow("Ready to record")
    val recordingStatus: StateFlow<String> = _recordingStatus.asStateFlow()

    // 그래프 상태 관리
    private val _currentGraphData = MutableStateFlow<GraphVisualizationData?>(null)
    val currentGraphData: StateFlow<GraphVisualizationData?> = _currentGraphData.asStateFlow()

    private val _lastVoiceResult = MutableStateFlow<VoiceProcessingResult?>(null)

    private val _graphLoading = MutableStateFlow(false)
    val graphLoading: StateFlow<Boolean> = _graphLoading.asStateFlow()

    // 상태 히스토리 관리 (최대 100개)
    private val _statusHistory = MutableStateFlow<List<StatusLogEntry>>(emptyList())
    val statusHistory: StateFlow<List<StatusLogEntry>> = _statusHistory.asStateFlow()

    private val speechRepository = SpeechRepository(HttpClient(CIO))
    val sessionId = uuid4().toString()

    init {
        setupWebSocketCallbacks()
        connectWebSocket()
        addStatusLog("시스템 시작", StatusLogType.INFO)
    }

    /**
     * 상태 로그 추가 (최대 100개 유지)
     */
    private fun addStatusLog(message: String, type: StatusLogType = StatusLogType.INFO) {
        val timestamp = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            .let { "${it.hour.toString().padStart(2, '0')}:${it.minute.toString().padStart(2, '0')}:${it.second.toString().padStart(2, '0')}" }
        
        val newEntry = StatusLogEntry(timestamp, message, type)
        val currentList = _statusHistory.value.toMutableList()
        
        currentList.add(newEntry)
        if (currentList.size > 100) {
            currentList.removeAt(0)
        }
        
        _statusHistory.value = currentList
    }

    /**
     * 상태 히스토리 클리어
     */
    fun clearStatusHistory() {
        _statusHistory.value = emptyList()
        addStatusLog("로그 클리어됨", StatusLogType.INFO)
    }

    private fun connectWebSocket() {
        coroutineScope.launch {
            _connectionStatus.value = ConnectionStatus.Connecting
            addStatusLog("서버 연결 중...", StatusLogType.INFO)
            try {
                webSocketClient.connect()
                _connectionStatus.value = ConnectionStatus.Connected
                addStatusLog("서버 연결 완료", StatusLogType.SUCCESS)
            } catch (e: Exception) {
                Napier.e("ViewModel: Failed to connect WebSocket: ${e.message}", e)
                _connectionStatus.value = ConnectionStatus.Error
                addStatusLog("서버 연결 실패: ${e.message}", StatusLogType.ERROR)
            }
        }
    }

    fun sendToolCall(toolName: String, args: Map<String, String>) {
        coroutineScope.launch {
            if (toolName == "mock_navigation_data") {
                handleMockNavigationData()
            } else {
                webSocketClient.sendToolCall(CallToolRequest(toolName, args))
            }
        }
    }
    
    private suspend fun handleMockNavigationData() {
        try {
            _graphLoading.value = true
            Napier.i("Processing mock navigation data", tag = "AppViewModel")
            
            // 새로운 날씨 검색 결과로 모의 AllPathsResponse 객체 생성
            val mockNavigationSteps = listOf(
                com.vowser.client.websocket.dto.NavigationStep(
                    url = "https://naver.com",
                    title = "naver.com 메인",
                    action = "navigate",
                    selector = ""
                ),
                com.vowser.client.websocket.dto.NavigationStep(
                    url = "https://www.naver.com",
                    title = "날씨",
                    action = "click",
                    selector = "a[href*='weather.naver.com']"
                ),
                com.vowser.client.websocket.dto.NavigationStep(
                    url = "https://weather.naver.com",
                    title = "지역선택",
                    action = "click",
                    selector = ".region_select .btn_region"
                ),
                com.vowser.client.websocket.dto.NavigationStep(
                    url = "https://weather.naver.com/region/list",
                    title = "부산",
                    action = "click",
                    selector = ".region_list .region_item[data-region='busan'] a"
                ),
                com.vowser.client.websocket.dto.NavigationStep(
                    url = "https://weather.naver.com/today/09440111",
                    title = "미세먼지",
                    action = "click",
                    selector = ".content_tabmenu .tab_item[data-tab='air'] a"
                ),
                com.vowser.client.websocket.dto.NavigationStep(
                    url = "https://weather.naver.com/air/09440111",
                    title = "주간",
                    action = "click",
                    selector = ".air_chart_area .btn_chart_period[data-period='week']"
                ),
                com.vowser.client.websocket.dto.NavigationStep(
                    url = "https://weather.naver.com/air/09440111?period=week",
                    title = "지역비교",
                    action = "click",
                    selector = ".compare_area .btn_compare"
                )
            )
            
            val mockPathDetail = PathDetail(
                pathId = "09e2a975413c0e18a7cd9d0f57b15dea",
                score = 0.489,
                total_weight = 73,
                steps = mockNavigationSteps
            )
            
            val mockAllPaths = AllPathsResponse(
                query = "우리 지역 날씨 알고 싶어",
                paths = listOf(mockPathDetail)
            )
            
            // 그래프 UI 업데이트
            val visualizationData = GraphDataConverter.convertFromAllPaths(mockAllPaths)
            Napier.i("Mock graph visualization data created - Nodes: ${visualizationData.nodes.size}, Edges: ${visualizationData.edges.size}", tag = "AppViewModel")
            _currentGraphData.value = visualizationData
            _graphLoading.value = false
            
            // 첫번째 경로 자동 실행 (playwright)
            Napier.i("Auto-executing mock navigation path: ${mockPathDetail.pathId}", tag = "AppViewModel")
            try {
                val navigationPath = NavigationPath(
                    pathId = mockPathDetail.pathId,
                    steps = mockPathDetail.steps,
                    description = "Mock test execution from UI"
                )
                BrowserAutomationBridge.executeNavigationPath(navigationPath)
                Napier.i("Successfully started automation for mock path: ${mockPathDetail.pathId}", tag = "AppViewModel")
            } catch (e: Exception) {
                Napier.e("Failed to execute mock navigation path: ${e.message}", e, tag = "AppViewModel")
            }
            
        } catch (e: Exception) {
            Napier.e("Failed to process mock navigation data: ${e.message}", e, tag = "AppViewModel")
            _graphLoading.value = false
        }
    }

    fun reconnect() {
        coroutineScope.launch {
            addStatusLog("서버 재연결 시도...", StatusLogType.INFO)
            webSocketClient.reconnect()
            _connectionStatus.value = ConnectionStatus.Connecting
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
        addStatusLog("음성 녹음 시작 중...", StatusLogType.INFO)
        val success = startPlatformRecording()
        if (success) {
            _isRecording.value = true
            _recordingStatus.value = "Recording..."
            addStatusLog("음성 녹음 중", StatusLogType.INFO)
            Napier.i("Recording started successfully", tag = "AppViewModel")
        } else {
            _recordingStatus.value = "Failed to start recording"
            addStatusLog("음성 녹음 시작 실패", StatusLogType.ERROR)
            Napier.e("Failed to start recording", tag = "AppViewModel")
        }
    }

    private suspend fun stopRecordingImpl() {
        _recordingStatus.value = "Stopping recording..."
        addStatusLog("음성 녹음 중지 중...", StatusLogType.INFO)
        _isRecording.value = false
        
        val audioBytes = stopPlatformRecording()
        if (audioBytes != null) {
            _recordingStatus.value = "Uploading audio..."
            addStatusLog("음성 데이터 업로드 중...", StatusLogType.INFO)
            try {
                val result = speechRepository.transcribeAudio(audioBytes, sessionId)
                result.fold(
                    onSuccess = { response ->
                        _recordingStatus.value = "Audio processed successfully"
                        addStatusLog("음성 처리 완료", StatusLogType.SUCCESS)
                        Napier.i("Audio transcription result: $response", tag = "AppViewModel")
                    },
                    onFailure = { error ->
                        _recordingStatus.value = "Failed to process audio: ${error.message}"
                        addStatusLog("음성 처리 실패: ${error.message}", StatusLogType.ERROR)
                        Napier.e("Audio transcription failed: ${error.message}", tag = "AppViewModel")
                    }
                )
            } catch (e: Exception) {
                _recordingStatus.value = "Error processing audio: ${e.message}"
                addStatusLog("음성 처리 오류: ${e.message}", StatusLogType.ERROR)
                Napier.e("Error processing audio: ${e.message}", e, tag = "AppViewModel")
            }
        } else {
            _recordingStatus.value = "No audio data recorded"
            addStatusLog("녹음된 음성 데이터 없음", StatusLogType.WARNING)
        }

        delay(3000)
        _recordingStatus.value = "Ready to record"
        addStatusLog("녹음 준비 완료", StatusLogType.INFO)
    }

    /**
     * WebSocket 콜백 설정
     */
    private fun setupWebSocketCallbacks() {
        Napier.i("Setting up WebSocket callbacks", tag = "AppViewModel")
        webSocketClient.onAllPathsReceived = { allPaths ->
            coroutineScope.launch {
                Napier.i("Received all paths for query: ${allPaths.query}", tag = "AppViewModel")
                addStatusLog("경로 분석 완료: ${allPaths.query}", StatusLogType.SUCCESS)

                // 1. 그래프 UI 업데이트
                // AllPathsResponse를 시각화 데이터로 변환 (GraphDataConverter에 새 함수 추가 필요)
                val visualizationData = GraphDataConverter.convertFromAllPaths(allPaths)
                addStatusLog("그래프 데이터 생성됨 (노드: ${visualizationData.nodes.size}개)", StatusLogType.INFO)
                Napier.i("Graph visualization data created - Nodes: ${visualizationData.nodes.size}, Edges: ${visualizationData.edges.size}", tag = "AppViewModel")
                _currentGraphData.value = visualizationData
                _graphLoading.value = false
                Napier.i("Graph data updated and loading set to false", tag = "AppViewModel")

                // 첫번째 경로 자동 실행 (가중치가 가장 높음)
                val firstPath = allPaths.paths.firstOrNull()
                if (firstPath != null) {
                    Napier.i("Auto-executing the first path: ${firstPath.pathId}", tag = "AppViewModel")
                    addStatusLog("브라우저 자동화 시작", StatusLogType.INFO)
                    try {
                        val navigationPath = NavigationPath(
                            pathId = firstPath.pathId,
                            steps = firstPath.steps,
                            description = "Auto-executed path from voice command"
                        )
                        BrowserAutomationBridge.executeNavigationPath(navigationPath)
                        addStatusLog("브라우저 제어 완료", StatusLogType.SUCCESS)
                        Napier.i("Successfully started automation for path: ${firstPath.pathId}", tag = "AppViewModel")
                    } catch (e: Exception) {
                        addStatusLog("브라우저 제어 실패: ${e.message}", StatusLogType.ERROR)
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
                    addStatusLog("음성 인식됨: ${voiceResult.transcript}", StatusLogType.SUCCESS)
                    _graphLoading.value = true
                    addStatusLog("경로 분석 중...", StatusLogType.INFO)
                } else {
                    _recordingStatus.value = "Voice processing failed: ${voiceResult.error?.message ?: "Unknown error"}"
                    addStatusLog("음성 인식 실패: ${voiceResult.error?.message ?: "Unknown error"}", StatusLogType.ERROR)
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