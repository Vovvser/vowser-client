package com.vowser.client

import com.vowser.client.auth.AuthManager
import com.vowser.client.auth.TokenStorage
import com.vowser.client.data.AuthRepository
import com.vowser.client.data.SpeechRepository
import com.vowser.client.data.createHttpClient
import com.vowser.client.exception.ExceptionHandler
import com.vowser.client.api.PathApiClient
import com.vowser.client.api.PathExecutor
import com.vowser.client.api.dto.MatchedPathDetail
import com.vowser.client.logging.LogUtils
import com.vowser.client.logging.Tags
import com.vowser.client.model.AuthState
import com.vowser.client.model.MemberResponse
import com.vowser.client.visualization.GraphVisualizationData
import com.vowser.client.websocket.BrowserControlWebSocketClient
import com.vowser.client.websocket.ConnectionStatus
import com.vowser.client.websocket.dto.CallToolRequest
import com.vowser.client.websocket.dto.VoiceProcessingResult
import com.vowser.client.websocket.dto.toMatchedPathDetail
import com.vowser.client.browserautomation.BrowserAutomationBridge
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
import kotlinx.coroutines.IO

class AppViewModel(
    private val coroutineScope: CoroutineScope,
    private val tokenStorage: TokenStorage,
    private val authRepository: AuthRepository,
    private val authManager: AuthManager,
    val exceptionHandler: ExceptionHandler = ExceptionHandler(coroutineScope)
) {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    // 유저 상세 정보
    private val _userInfo = MutableStateFlow<MemberResponse?>(null)
    val userInfo: StateFlow<MemberResponse?> = _userInfo.asStateFlow()

    private val _userInfoLoading = MutableStateFlow(false)
    val userInfoLoading: StateFlow<Boolean> = _userInfoLoading.asStateFlow()

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.Disconnected)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _receivedMessage = MutableStateFlow("No message")
    val receivedMessage: StateFlow<String> = _receivedMessage.asStateFlow()

    private val webSocketClient = BrowserControlWebSocketClient(exceptionHandler)

    val dialogState = exceptionHandler.dialogState

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

    // 상태 히스토리 관리
    private val _statusHistory = MutableStateFlow<List<StatusLogEntry>>(emptyList())
    val statusHistory: StateFlow<List<StatusLogEntry>> = _statusHistory.asStateFlow()

    // REST API 클라이언트 (경로 저장/검색)
    private val backendUrl = "http://localhost:8080"
    private val pathApiClient = PathApiClient(createHttpClient(tokenStorage), backendUrl)
    private val pathExecutor = PathExecutor()

    // 경로 검색 및 실행 상태
    private val _searchedPaths = MutableStateFlow<List<MatchedPathDetail>>(emptyList())
    val searchedPaths: StateFlow<List<MatchedPathDetail>> = _searchedPaths.asStateFlow()

    private val _isExecutingPath = MutableStateFlow(false)
    val isExecutingPath: StateFlow<Boolean> = _isExecutingPath.asStateFlow()

    private val _executionProgress = MutableStateFlow("")
    val executionProgress: StateFlow<String> = _executionProgress.asStateFlow()

    // STT modes
    private val _selectedSttModes = MutableStateFlow(setOf("general"))
    val selectedSttModes: StateFlow<Set<String>> = _selectedSttModes.asStateFlow()

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

            val result = authRepository.getMe()
            result.onSuccess { memberResponse ->
                _authState.value = AuthState.Authenticated(memberResponse.name, memberResponse.email)
                _userInfo.value = memberResponse
            }.onFailure {
                _authState.value = AuthState.NotAuthenticated
                _userInfo.value = null
                tokenStorage.clearTokens()
            }
        }
    }

    fun startAuthCallbackServer() {
        authManager.startCallbackServer { accessToken, refreshToken ->
            handleLoginSuccess(accessToken, refreshToken)
        }
    }

    fun login() {
        startAuthCallbackServer()
        authManager.login()
    }

    fun handleLoginSuccess(accessToken: String, refreshToken: String) {
        coroutineScope.launch {
            Napier.i("Saving tokens after login success", tag = Tags.AUTH)
            tokenStorage.saveTokens(accessToken, refreshToken)

            // 토큰 저장 확인
            val savedToken = tokenStorage.getAccessToken()
            Napier.i("Token saved successfully: ${savedToken != null}", tag = Tags.AUTH)
            kotlinx.coroutines.delay(150)

            checkAuthStatus()
        }
    }

    fun logout() {
        coroutineScope.launch {
            authRepository.logout()
            tokenStorage.clearTokens()
            _authState.value = AuthState.NotAuthenticated
            _userInfo.value = null
        }
    }

    fun refreshUserInfo() {
        coroutineScope.launch {
            _userInfoLoading.value = true
            try {
                val result = authRepository.getMe()
                result.onSuccess { memberResponse ->
                    _userInfo.value = memberResponse
                    addStatusLog("유저 정보 조회 완료", StatusLogType.SUCCESS)
                    Napier.i("User info refreshed: ${memberResponse.email}", tag = Tags.AUTH)
                }.onFailure { error ->
                    addStatusLog("유저 정보 조회 실패: ${error.message}", StatusLogType.ERROR)
                    Napier.e("Failed to refresh user info: ${error.message}", error, tag = Tags.AUTH)
                    exceptionHandler.handleException(error, "User info refresh") {
                        refreshUserInfo()
                    }
                }
            } finally {
                _userInfoLoading.value = false
            }
        }
    }

    fun executeQuery(query: String) {
        coroutineScope.launch {
            handleVoiceCommand(query)
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
            .let {
                "${it.hour.toString().padStart(2, '0')}:${
                    it.minute.toString().padStart(2, '0')
                }:${it.second.toString().padStart(2, '0')}"
            }

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
            /**
             * TODO - 새로 바뀐 구조로 추가 예정
             */
            webSocketClient.sendToolCall(CallToolRequest(toolName, args))
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
                        Napier.i(
                            "Audio transcription result: ${LogUtils.filterSensitive(response)}",
                            tag = Tags.MEDIA_SPEECH
                        )
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
        // 검색 결과 콜백
        webSocketClient.onSearchResultReceived = { matchedPaths, query ->
            coroutineScope.launch {
                addStatusLog("✅ ${matchedPaths.size}개 경로 검색됨: $query", StatusLogType.SUCCESS)
                Napier.i("Received ${matchedPaths.size} matched paths for query: $query", tag = Tags.APP_VIEWMODEL)

                // 그래프 시각화
                val pathDetails = matchedPaths.map { it.toMatchedPathDetail() }
                val visualizationData = convertToGraph(pathDetails)
                _currentGraphData.value = visualizationData
                _graphLoading.value = false

                // 첫 번째 경로의 전체 스텝 실행
                val firstPath = matchedPaths.firstOrNull()
                if (firstPath != null) {
                    addStatusLog("🚀 경로 실행: ${firstPath.taskIntent} (${firstPath.steps.size} 스텝)", StatusLogType.INFO)
                    executeFullPath(firstPath)
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

                    // 새로운 REST API로 경로 검색 및 실행
                    val transcript = voiceResult.transcript
                    if (!transcript.isNullOrBlank()) {
                        handleVoiceCommand(transcript)
                    }
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
                webSocketClient.sendToolCall(
                    CallToolRequest(
                        "refresh_graph", mapOf(
                            "sessionId" to sessionId
                        )
                    )
                )
                Napier.i("Graph refresh requested", tag = Tags.UI_GRAPH)
            } catch (e: Exception) {
                Napier.e("Failed to request graph refresh: ${e.message}", e, tag = Tags.UI_GRAPH)
                _graphLoading.value = false
            }
        }
    }

    private fun setupContributionMode() {
        BrowserAutomationBridge.setContributionRecordingCallback { step ->
            contributionModeService.recordStep(step)
        }
    }

    fun startContribution(task: String) {
        coroutineScope.launch {
            try {
                addStatusLog("기여 모드 초기화 중...", StatusLogType.INFO)

                BrowserAutomationBridge.startContributionRecording()

                contributionModeService.startSession(task)

                kotlinx.coroutines.delay(com.vowser.client.contribution.ContributionConstants.BROWSER_INIT_WAIT_MS) // 브라우저 초기화 대기
                BrowserAutomationBridge.navigate("about:blank")

                addStatusLog("🚀 기여 모드 시작됨 - 작업: \"$task\"", StatusLogType.SUCCESS)

            } catch (e: Exception) {
                exceptionHandler.handleException(e, "Contribution mode initialization") {
                    startContribution(task)
                }

                try {
                    BrowserAutomationBridge.stopContributionRecording()
                    contributionModeService.resetSession()
                } catch (cleanupError: Exception) {
                    Napier.w("Cleanup error: ${cleanupError.message}", tag = Tags.CONTRIBUTION_MODE)
                }
            }
        }
    }

    fun stopContribution() {
        coroutineScope.launch {
            try {
                val stepCount = contributionModeService.currentStepCount.value
                val task = contributionModeService.currentTask.value
                val sessionId = contributionModeService.getCurrentSessionId()

                // 브라우저 녹화 중지
                BrowserAutomationBridge.stopContributionRecording()

                // 세션 종료 (WebSocket으로 전송)
                contributionModeService.endSession()

                addStatusLog("🏁 기여 모드 완료 - 총 ${stepCount}개 스텝 기록됨", StatusLogType.SUCCESS)

                // 추가로 REST API를 통해 저장 (새로운 방식)
                if (sessionId != null && task.isNotBlank() && stepCount > 0) {
                    addStatusLog("경로 데이터 저장 중...", StatusLogType.INFO)
                    saveContributionPath(sessionId, task)
                }
            } catch (e: Exception) {
                Napier.e("Failed to stop contribution: ${e.message}", e, tag = Tags.CONTRIBUTION_MODE)
                addStatusLog("기여 모드 종료 실패: ${e.message}", StatusLogType.ERROR)
            }
        }
    }

    /**
     * 기여 경로를 REST API를 통해 저장
     */
    private suspend fun saveContributionPath(sessionId: String, taskIntent: String) {
        try {
            // 현재 세션의 스텝들을 가져옴
            val steps = contributionModeService.getCurrentSession()?.steps ?: emptyList()

            if (steps.isEmpty()) {
                Napier.w("No steps to save for session: $sessionId", tag = Tags.CONTRIBUTION_MODE)
                return
            }

            // 도메인 추출 (첫 번째 스텝의 URL에서)
            val domain = steps.firstOrNull()?.url?.let { url ->
                try {
                    val host = url.substringAfter("://").substringBefore("/")
                    host.replace("www.", "")
                } catch (e: Exception) {
                    "unknown.com"
                }
            } ?: "unknown.com"

            // REST API를 통해 저장
            val result = pathApiClient.savePath(
                sessionId = sessionId,
                taskIntent = taskIntent,
                domain = domain,
                steps = steps
            )

            result.fold(
                onSuccess = { response ->
                    val savedSteps = response.data.result.steps_saved
                    addStatusLog("경로 저장 완료: $taskIntent ($savedSteps 단계)", StatusLogType.SUCCESS)
                    Napier.i(
                        "Path saved via REST API: $savedSteps steps for task '$taskIntent'",
                        tag = Tags.CONTRIBUTION_MODE
                    )
                },
                onFailure = { error ->
                    addStatusLog("경로 저장 실패: ${error.message}", StatusLogType.WARNING)
                    Napier.e("Failed to save path via REST API: ${error.message}", error, tag = Tags.CONTRIBUTION_MODE)
                }
            )
        } catch (e: Exception) {
            Napier.e("Error in saveContributionPath: ${e.message}", e, tag = Tags.CONTRIBUTION_MODE)
            addStatusLog("경로 저장 오류: ${e.message}", StatusLogType.WARNING)
        }
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

// ===== 경로 검색 및 실행 기능 =====

    /**
     * 음성 명령 처리 (REST API 기반)
     */
    private suspend fun handleVoiceCommand(transcript: String) {
        try {
            _graphLoading.value = true
            addStatusLog("경로 검색 중: $transcript", StatusLogType.INFO)

            val result = kotlinx.coroutines.withContext(Dispatchers.IO) {
                pathApiClient.searchPaths(transcript, limit = 5)
            }

            result.fold(
                onSuccess = { response ->
                    val paths = response.data.matched_paths
                    _searchedPaths.value = paths

                    if (paths.isEmpty()) {
                        addStatusLog("검색 결과 없음: $transcript", StatusLogType.WARNING)
                        _graphLoading.value = false
                        return
                    }

                    addStatusLog(
                        "${paths.size}개 경로 검색됨 (${response.data.performance.search_time}ms)",
                        StatusLogType.SUCCESS
                    )
                    Napier.i("Found ${paths.size} paths for query: $transcript", tag = Tags.APP_VIEWMODEL)

                    val visualizationData = convertToGraph(paths)
                    _currentGraphData.value = visualizationData
                    _graphLoading.value = false

                    val firstPath = paths.firstOrNull()
                    if (firstPath != null) {
                        addStatusLog(
                            "최적 경로 실행 중: ${firstPath.task_intent} (관련도: ${(firstPath.relevance_score * 100).toInt()}%)",
                            StatusLogType.INFO
                        )

                        coroutineScope.launch {
                            executePathFromVoice(firstPath)
                        }
                    }
                },
                onFailure = { error ->
                    _searchedPaths.value = emptyList()
                    _graphLoading.value = false
                    addStatusLog("경로 검색 실패: ${error.message}", StatusLogType.ERROR)
                    Napier.e("Failed to search paths: ${error.message}", error, tag = Tags.APP_VIEWMODEL)
                }
            )
        } catch (e: Exception) {
            _searchedPaths.value = emptyList()
            _graphLoading.value = false
            addStatusLog("경로 검색 오류: ${e.message}", StatusLogType.ERROR)
            Napier.e("Error in handleVoiceCommand: ${e.message}", e, tag = Tags.APP_VIEWMODEL)
        }
    }

    /**
     * 음성 명령으로부터 경로 실행
     */
    private suspend fun executePathFromVoice(path: MatchedPathDetail) {
        try {
            if (_isExecutingPath.value) {
                addStatusLog("다른 경로가 실행 중입니다", StatusLogType.WARNING)
                return
            }

            _isExecutingPath.value = true
            _executionProgress.value = "0/${path.steps.size}"

            val result = pathExecutor.executePath(
                path = path,
                onStepComplete = { current, total, description ->
                    _executionProgress.value = "$current/$total"
                    addStatusLog("[$current/$total] $description", StatusLogType.INFO)
                },
                getUserInput = null
            )

            if (result.success) {
                addStatusLog("경로 실행 완료: ${path.task_intent}", StatusLogType.SUCCESS)
                Napier.i("Voice command path execution completed: ${path.task_intent}", tag = Tags.BROWSER_AUTOMATION)
            } else {
                val failedStep = result.failedAt?.let { "${it + 1}/${result.totalSteps}" } ?: "Unknown"
                addStatusLog("경로 실행 실패 (단계 $failedStep): ${result.error}", StatusLogType.ERROR)
                Napier.e(
                    "Voice command path execution failed at step $failedStep: ${result.error}",
                    tag = Tags.BROWSER_AUTOMATION
                )
            }
        } catch (e: Exception) {
            addStatusLog("경로 실행 오류: ${e.message}", StatusLogType.ERROR)
            Napier.e("Error executing voice command path: ${e.message}", e, tag = Tags.BROWSER_AUTOMATION)
        } finally {
            _isExecutingPath.value = false
            _executionProgress.value = ""
        }
    }

    /**
     * 전체 경로 실행 (MatchedPath를 받아서 모든 스텝 순차 실행)
     */
    private suspend fun executeFullPath(path: com.vowser.client.websocket.dto.MatchedPath) {
        try {
            _isExecutingPath.value = true
            _executionProgress.value = "0/${path.steps.size}"

            // MatchedPath → MatchedPathDetail 변환
            val pathDetail = path.toMatchedPathDetail()

            val result = pathExecutor.executePath(
                path = pathDetail,
                onStepComplete = { current, total, description ->
                    _executionProgress.value = "$current/$total"
                    addStatusLog("[$current/$total] $description", StatusLogType.INFO)
                },
                getUserInput = null  // 자동 실행 (input 스킵)
            )

            if (result.success) {
                addStatusLog(
                    "전체 경로 완료: ${path.taskIntent} (${result.stepsCompleted}/${result.totalSteps})",
                    StatusLogType.SUCCESS
                )
            } else {
                addStatusLog("실패 (${result.failedAt}/${result.totalSteps}): ${result.error}", StatusLogType.ERROR)
            }
        } catch (e: Exception) {
            addStatusLog("경로 실행 오류: ${e.message}", StatusLogType.ERROR)
        } finally {
            _isExecutingPath.value = false
            _executionProgress.value = ""
        }
    }

    /**
     * 그래프 변환
     */
    private fun convertToGraph(paths: List<MatchedPathDetail>): GraphVisualizationData {
        val nodes = mutableListOf<com.vowser.client.ui.graph.GraphNode>()
        val edges = mutableListOf<com.vowser.client.ui.graph.GraphEdge>()

        paths.forEachIndexed { pathIndex, path ->
            path.steps.forEachIndexed { stepIndex, step ->
                val nodeId = "path${pathIndex}_step${stepIndex}"
                nodes.add(
                    com.vowser.client.ui.graph.GraphNode(
                        id = nodeId,
                        label = step.description
                    )
                )

                if (stepIndex > 0) {
                    edges.add(
                        com.vowser.client.ui.graph.GraphEdge(
                            from = "path${pathIndex}_step${stepIndex - 1}",
                            to = nodeId
                        )
                    )
                }
            }
        }

        return GraphVisualizationData(nodes, edges)
    }
}

expect suspend fun startPlatformRecording(): Boolean
expect suspend fun stopPlatformRecording(): ByteArray?
expect fun openUrlInBrowser(url: String)