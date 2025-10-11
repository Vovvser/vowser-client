package com.vowser.client

import com.vowser.client.auth.AuthManager
import com.vowser.client.auth.TokenStorage
import com.vowser.client.data.AuthRepository
import com.vowser.client.data.GraphDataConverter
import com.vowser.client.data.SpeechRepository
import com.vowser.client.data.createHttpClient
import com.vowser.client.exception.ExceptionHandler
import com.vowser.client.logging.Tags
import com.vowser.client.model.AuthState
import com.vowser.client.visualization.GraphVisualizationData
import com.vowser.client.websocket.BrowserControlWebSocketClient
import com.vowser.client.websocket.ConnectionStatus
import com.vowser.client.websocket.dto.CallToolRequest
import com.vowser.client.websocket.dto.VoiceProcessingResult
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import com.vowser.client.navigation.NavigationProcessor
import com.vowser.client.data.WebNavigationDataGenerator
import kotlinx.coroutines.IO

class AppViewModel(
    private val coroutineScope: CoroutineScope,
    private val tokenStorage: TokenStorage,
    private val authRepository: AuthRepository,
    private val authManager: AuthManager,
    val exceptionHandler: ExceptionHandler = ExceptionHandler(coroutineScope)
) {

    val navigationProcessor: NavigationProcessor

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.Disconnected)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _receivedMessage = MutableStateFlow("No message")
    val receivedMessage: StateFlow<String> = _receivedMessage.asStateFlow()

    private val webSocketClient = BrowserControlWebSocketClient(exceptionHandler)

    val dialogState = exceptionHandler.dialogState

    // Recording states
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingStatus = MutableStateFlow("Ready to record")
    val recordingStatus: StateFlow<String> = _recordingStatus.asStateFlow()

    // Graph states
    private val _currentGraphData = MutableStateFlow<GraphVisualizationData?>(null)
    val currentGraphData: StateFlow<GraphVisualizationData?> = _currentGraphData.asStateFlow()

    private val _lastVoiceResult = MutableStateFlow<VoiceProcessingResult?>(null)

    private val _graphLoading = MutableStateFlow(false)
    val graphLoading: StateFlow<Boolean> = _graphLoading.asStateFlow()

    // Status history - must be initialized before init block
    private val _statusHistory = MutableStateFlow<List<StatusLogEntry>>(emptyList())
    val statusHistory: StateFlow<List<StatusLogEntry>> = _statusHistory.asStateFlow()

    // STT modes
    private val _selectedSttModes = MutableStateFlow(setOf("general"))
    val selectedSttModes: StateFlow<Set<String>> = _selectedSttModes.asStateFlow()

    // Services
    private val speechRepository = SpeechRepository(createHttpClient(tokenStorage))
    val sessionId = com.benasher44.uuid.uuid4().toString()

    private val contributionModeService = com.vowser.client.contribution.ContributionModeService(
        coroutineScope = coroutineScope,
        onSendMessage = { message -> sendContributionMessage(message) },
        onUILog = { stepNumber, action, elementName, url ->
            addContributionLog(stepNumber, action, elementName, url)
        }
    )
    val contributionStatus = contributionModeService.status
    val contributionStepCount = contributionModeService.currentStepCount
    val contributionTask = contributionModeService.currentTask

    init {
        val initialGraph = WebNavigationDataGenerator.createSampleData()
        navigationProcessor = NavigationProcessor(initialGraph)

        checkAuthStatus()
        setupWebSocketCallbacks()
        connectWebSocket()
        setupContributionMode()
        addStatusLog("시스템 시작", StatusLogType.INFO)
    }

    fun checkAuthStatus() {
        coroutineScope.launch {
            _authState.value = AuthState.Loading
            val accessToken = tokenStorage.getAccessToken()
            if (accessToken == null) {
                _authState.value = AuthState.NotAuthenticated
                return@launch
            }

            println("AppViewModel: Calling authRepository.getMe()...")
            val result = authRepository.getMe()
            result.onSuccess {
                _authState.value = AuthState.Authenticated(it.name, it.email)
                println("AppViewModel: getMe() success! authState = Authenticated(${it.name}, ${it.email})")
            }.onFailure {
                _authState.value = AuthState.NotAuthenticated
                tokenStorage.clearTokens()
                println("AppViewModel: getMe() failed: ${it.message}, authState = NotAuthenticated")
            }
        }
    }

    fun startAuthCallbackServer() {
        authManager.startCallbackServer { accessToken, refreshToken ->
            handleLoginSuccess(accessToken, refreshToken)
        }
    }

    fun login() {
        authManager.login()
    }

    fun handleLoginSuccess(accessToken: String, refreshToken: String) {
        tokenStorage.saveTokens(accessToken, refreshToken)
        checkAuthStatus()
    }

    fun logout() {
        coroutineScope.launch {
            authRepository.logout()
            tokenStorage.clearTokens()
            _authState.value = AuthState.NotAuthenticated
        }
    }

    fun toggleSttMode(modeId: String) {
        val currentModes = _selectedSttModes.value.toMutableSet()

        if (currentModes.contains(modeId)) {
            if (currentModes.size > 1) {
                currentModes.remove(modeId)
                addStatusLog("STT 모드 비활성화: ${getSttModeDisplayName(modeId)}", StatusLogType.INFO)
            }
        } else {
            currentModes.add(modeId)
            addStatusLog("STT 모드 활성화: ${getSttModeDisplayName(modeId)}", StatusLogType.INFO)
        }

        _selectedSttModes.value = currentModes
    }

    private fun getSttModeDisplayName(modeId: String): String {
        return when (modeId) {
            "general" -> "일반"
            "number" -> "숫자"
            "alphabet" -> "알파벳"
            "snippet" -> "스니펫"
            else -> modeId
        }
    }

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

    fun clearStatusHistory() {
        _statusHistory.value = emptyList()
    }

    fun addContributionLog(stepNumber: Int, action: String, elementName: String?, url: String?) {
        val message = when (action) {
            "click" -> {
                val element = elementName?.let { "\"$it\"" } ?: "요소"
                "[$stepNumber]스텝 $element 를 클릭했습니다."
            }
            "navigate" -> {
                val destination = url?.let {
                    when {
                        it.startsWith("about:blank") -> "빈 페이지"
                        it.startsWith("http") -> it.substringAfter("://").substringBefore("/").take(25)
                        else -> it.take(25)
                    }
                } ?: "페이지"
                "[$stepNumber]스텝 $destination 로 이동했습니다."
            }
            "type" -> {
                val input = elementName?.let { "\"$it\"" } ?: "텍스트"
                "[$stepNumber]스텝 $input 를 입력했습니다."
            }
            "new_tab" -> {
                "[$stepNumber]스텝 새 탭이 열렸습니다."
            }
            else -> {
                "[$stepNumber]스텝 $action 작업을 수행했습니다."
            }
        }

        addStatusLog(message, StatusLogType.INFO)
    }

    private fun connectWebSocket() {
        coroutineScope.launch {
            _connectionStatus.value = ConnectionStatus.Connecting
            addStatusLog("서버 연결 중...", StatusLogType.INFO)
            try {
                webSocketClient.connect()
                _connectionStatus.value = ConnectionStatus.Connected
                addStatusLog("서버 연결 완료", StatusLogType.SUCCESS)
                addStatusLog("음성으로 명령해보세요! (예: \"웹툰 보고싶어\", \"서울 날씨 알려줘\")", StatusLogType.INFO)
            } catch (e: Exception) {
                Napier.e("ViewModel: Failed to connect WebSocket: ${e.message}", e, tag = Tags.APP_VIEWMODEL)
                _connectionStatus.value = ConnectionStatus.Error
                exceptionHandler.handleException(e, "WebSocket initial connection") {
                    connectWebSocket()
                }
            }
        }
    }

    fun sendToolCall(toolName: String, args: Map<String, String>) {
        coroutineScope.launch {
            if (toolName == "mock_navigation_data") {
                // handleMockNavigationData()
            } else {
                webSocketClient.sendToolCall(CallToolRequest(toolName, args))
            }
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
            Napier.i("Recording started successfully", tag = Tags.MEDIA_RECORDING)
        } else {
            _recordingStatus.value = "Failed to start recording"
            addStatusLog("음성 녹음 시작 실패", StatusLogType.ERROR)
            Napier.e("Failed to start recording", tag = Tags.MEDIA_RECORDING)
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
                val result = speechRepository.transcribeAudio(audioBytes, sessionId, _selectedSttModes.value)
                result.fold(
                    onSuccess = { response ->
                        _recordingStatus.value = "Audio processed successfully"
                        addStatusLog("음성 처리 완료", StatusLogType.SUCCESS)
                        Napier.i("Audio transcription result: ${com.vowser.client.logging.LogUtils.filterSensitive(response.toString())}", tag = Tags.MEDIA_SPEECH)
                    },
                    onFailure = { error ->
                        _recordingStatus.value = "Failed to process audio: ${error.message}"
                        exceptionHandler.handleException(
                            error,
                            context = "Audio transcription"
                        ) {
                            val audioBytes = stopPlatformRecording()
                            audioBytes?.let {
                                val result = speechRepository.transcribeAudio(it, sessionId, _selectedSttModes.value)
                                result.getOrThrow()
                            }
                        }
                    }
                )
            } catch (e: Exception) {
                _recordingStatus.value = "Error processing audio: ${e.message}"
                exceptionHandler.handleException(e, "Audio processing") {
                    val audioBytes = stopPlatformRecording()
                    audioBytes?.let {
                        val result = speechRepository.transcribeAudio(it, sessionId, _selectedSttModes.value)
                        result.getOrThrow()
                    }
                }
            }
        } else {
            _recordingStatus.value = "No audio data recorded"
            addStatusLog("녹음된 음성 데이터 없음", StatusLogType.WARNING)
        }

        kotlinx.coroutines.delay(com.vowser.client.contribution.ContributionConstants.RECORDING_STATUS_RESET_DELAY_MS)
        _recordingStatus.value = "Ready to record"
        addStatusLog("녹음 준비 완료", StatusLogType.INFO)
    }

    private fun setupWebSocketCallbacks() {
        Napier.i("Setting up WebSocket callbacks", tag = Tags.APP_VIEWMODEL)
        webSocketClient.onAllPathsReceived = { allPaths ->
            coroutineScope.launch {
                Napier.i("Received all paths for query: ${allPaths.query}", tag = Tags.APP_VIEWMODEL)
                addStatusLog("경로 분석 완료: ${allPaths.query}", StatusLogType.SUCCESS)

                val visualizationData = GraphDataConverter.convertFromAllPaths(allPaths)
                addStatusLog("그래프 데이터 생성됨 (노드: ${visualizationData.nodes.size}개)", StatusLogType.INFO)
                Napier.i("Graph visualization data created - Nodes: ${visualizationData.nodes.size}, Edges: ${visualizationData.edges.size}", tag = Tags.UI_GRAPH)
                _currentGraphData.value = visualizationData
                _graphLoading.value = false
                Napier.d("Graph data updated and loading set to false", tag = Tags.UI_GRAPH)

                val firstPath = allPaths.paths.firstOrNull()
                if (firstPath != null) {
                    Napier.i("Auto-executing the first path: ${firstPath.pathId}", tag = Tags.BROWSER_AUTOMATION)
                    addStatusLog("브라우저 자동화 시작", StatusLogType.INFO)
                    try {
                        val navigationPath = com.vowser.client.websocket.dto.NavigationPath(
                            pathId = firstPath.pathId,
                            steps = firstPath.steps,
                            description = "Auto-executed path from voice command"
                        )
                        com.vowser.client.browserautomation.BrowserAutomationBridge.executeNavigationPath(navigationPath)
                        addStatusLog("브라우저 제어 완료", StatusLogType.SUCCESS)
                        Napier.i("Successfully started automation for path: ${firstPath.pathId}", tag = Tags.BROWSER_AUTOMATION)
                    } catch (e: Exception) {
                        exceptionHandler.handleException(e, "Browser automation execution") {
                            val navigationPath = com.vowser.client.websocket.dto.NavigationPath(
                                pathId = firstPath.pathId,
                                steps = firstPath.steps,
                                description = "Auto-executed path from voice command"
                            )
                            com.vowser.client.browserautomation.BrowserAutomationBridge.executeNavigationPath(navigationPath)
                        }
                    }
                }
            }
        }

        webSocketClient.onVoiceProcessingResultReceived = { voiceResult ->
            coroutineScope.launch {
                Napier.i("Received voice processing result: ${voiceResult.transcript ?: ""}", tag = Tags.MEDIA_SPEECH)
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
        Napier.i("WebSocket callbacks setup completed", tag = Tags.APP_VIEWMODEL)
    }

    fun refreshGraph() {
        coroutineScope.launch {
            _graphLoading.value = true
            try {
                webSocketClient.sendToolCall(CallToolRequest("refresh_graph", mapOf(
                    "sessionId" to sessionId
                )))
                Napier.i("Graph refresh requested", tag = Tags.UI_GRAPH)
            } catch (e: Exception) {
                Napier.e("Failed to request graph refresh: ${e.message}", e, tag = Tags.UI_GRAPH)
                _graphLoading.value = false
            }
        }
    }

    private fun setupContributionMode() {
        com.vowser.client.browserautomation.BrowserAutomationBridge.setContributionRecordingCallback { step ->
            contributionModeService.recordStep(step)
        }
    }

    fun startContribution(task: String) {
        coroutineScope.launch {
            try {
                addStatusLog("기여 모드 초기화 중...", StatusLogType.INFO)

                com.vowser.client.browserautomation.BrowserAutomationBridge.startContributionRecording()

                contributionModeService.startSession(task)

                kotlinx.coroutines.delay(com.vowser.client.contribution.ContributionConstants.BROWSER_INIT_WAIT_MS) // 브라우저 초기화 대기
                com.vowser.client.browserautomation.BrowserAutomationBridge.navigate("about:blank")

                addStatusLog("🚀 기여 모드 시작됨 - 작업: \"$task\"", StatusLogType.SUCCESS)

            } catch (e: Exception) {
                exceptionHandler.handleException(e, "Contribution mode initialization") {
                    startContribution(task)
                }

                try {
                    com.vowser.client.browserautomation.BrowserAutomationBridge.stopContributionRecording()
                    contributionModeService.resetSession()
                } catch (cleanupError: Exception) {
                    Napier.w("Cleanup error: ${cleanupError.message}", tag = Tags.CONTRIBUTION_MODE)
                }
            }
        }
    }

    fun stopContribution() {
        val stepCount = contributionModeService.currentStepCount.value
        com.vowser.client.browserautomation.BrowserAutomationBridge.stopContributionRecording()
        contributionModeService.endSession()
        addStatusLog("🏁 기여 모드 완료 - 총 ${stepCount}개 스텝 기록됨", StatusLogType.SUCCESS)
    }

    private suspend fun sendContributionMessage(message: com.vowser.client.contribution.ContributionMessage) {
        try {
            webSocketClient.sendContributionMessage(message)
            addStatusLog("기여 데이터 전송 완료 (${message.steps.size}개 단계)", StatusLogType.SUCCESS)
        } catch (e: Exception) {
            exceptionHandler.handleException(e, "Contribution data transmission") {
                sendContributionMessage(message)
            }
        }
    }
}

expect suspend fun startPlatformRecording(): Boolean
expect suspend fun stopPlatformRecording(): ByteArray?