package com.vowser.client

import kotlinx.coroutines.flow.update
import com.vowser.client.auth.AuthManager
import com.vowser.client.auth.TokenStorage
import com.vowser.client.data.AuthRepository
import com.vowser.client.data.SpeechRepository
import com.vowser.client.exception.ExceptionHandler
import com.vowser.client.api.PathApiClient
import com.vowser.client.api.PathExecutor
import com.vowser.client.api.dto.MatchedPathDetail
import com.vowser.client.api.dto.PathSearchResponse
import com.vowser.client.logging.LogUtils
import com.vowser.client.logging.Tags
import com.vowser.client.browserautomation.SelectOption
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
import io.ktor.websocket.CloseReason
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.vowser.client.contribution.ContributionStep
import com.vowser.client.ui.graph.GraphEdge
import com.vowser.client.ui.graph.GraphNode
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

import kotlinx.serialization.json.Json

class AppViewModel(
    private val coroutineScope: CoroutineScope,
    private val tokenStorage: TokenStorage,
    private val authRepository: AuthRepository,
    private val authManager: AuthManager,
    private val pathApiClient: PathApiClient,
    private val speechRepository: SpeechRepository,
    private val webSocketClient: BrowserControlWebSocketClient,
    val exceptionHandler: ExceptionHandler
) {

    private val json = Json { prettyPrint = true; isLenient = true; ignoreUnknownKeys = true }

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

    val dialogState = exceptionHandler.dialogState

    // 음성 녹음 관련
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingStatus = MutableStateFlow("Ready to record")

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
    private val pathExecutor = PathExecutor()

    // 경로 검색 및 실행 상태
    private val _searchedPaths = MutableStateFlow<List<MatchedPathDetail>>(emptyList())
    val searchedPaths: StateFlow<List<MatchedPathDetail>> = _searchedPaths.asStateFlow()

    private val _isExecutingPath = MutableStateFlow(false)
    val isExecutingPath: StateFlow<Boolean> = _isExecutingPath.asStateFlow()

    private val _executionProgress = MutableStateFlow("")
    val executionProgress: StateFlow<String> = _executionProgress.asStateFlow()

    // 현재 실행 중인 경로
    private val _currentExecutingPath = MutableStateFlow<MatchedPathDetail?>(null)
    val currentExecutingPath: StateFlow<MatchedPathDetail?> = _currentExecutingPath.asStateFlow()

    // 현재 실행 중인 스텝 인덱스
    private val _currentStepIndex = MutableStateFlow(-1)
    val currentStepIndex: StateFlow<Int> = _currentStepIndex.asStateFlow()

    // 사용자 대기 상태
    private val _isWaitingForUser = MutableStateFlow(false)
    val isWaitingForUser: StateFlow<Boolean> = _isWaitingForUser.asStateFlow()

    private val _waitMessage = MutableStateFlow("")
    val waitMessage: StateFlow<String> = _waitMessage.asStateFlow()

    // 사용자 확인 대기를 위한 continuation 저장
    private var waitContinuation: kotlin.coroutines.Continuation<Unit>? = null

    // 사용자 입력 대기 상태
    private val _isWaitingForUserInput = MutableStateFlow(false)
    val isWaitingForUserInput: StateFlow<Boolean> = _isWaitingForUserInput.asStateFlow()

    private val _inputRequest = MutableStateFlow<com.vowser.client.api.dto.PathStepDetail?>(null)
    val inputRequest: StateFlow<com.vowser.client.api.dto.PathStepDetail?> = _inputRequest.asStateFlow()

    private var userInputContinuation: kotlin.coroutines.Continuation<String>? = null

    private val _isWaitingForSelect = MutableStateFlow(false)
    val isWaitingForSelect: StateFlow<Boolean> = _isWaitingForSelect.asStateFlow()

    private val _selectOptions = MutableStateFlow<List<SelectOption>>(emptyList())
    val selectOptions: StateFlow<List<SelectOption>> = _selectOptions.asStateFlow()

    private var userSelectContinuation: kotlin.coroutines.Continuation<String>? = null

    // STT modes
    private val _selectedSttModes = MutableStateFlow(setOf("general"))
    val selectedSttModes: StateFlow<Set<String>> = _selectedSttModes.asStateFlow()

    val sessionId = com.benasher44.uuid.uuid4().toString()

    private val _pendingCommand = MutableStateFlow<String?>(null)
    val pendingCommand: StateFlow<String?> = _pendingCommand.asStateFlow()

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
    private val _isContributionInitializing = MutableStateFlow(false)
    val isContributionInitializing: StateFlow<Boolean> = _isContributionInitializing.asStateFlow()
    private var contributionSetupJob: Job? = null

    private val _awaitingContributionTask = MutableStateFlow(false)
    val awaitingContributionTask: StateFlow<Boolean> = _awaitingContributionTask.asStateFlow()

    private val _pendingContributionTask = MutableStateFlow<String?>(null)
    val pendingContributionTask: StateFlow<String?> = _pendingContributionTask.asStateFlow()

    init {
        webSocketClient.onConnectionOpened = {
            coroutineScope.launch {
                _connectionStatus.value = ConnectionStatus.Connected
                addStatusLog("서버 연결 완료", StatusLogType.SUCCESS)
            }
        }
        webSocketClient.onConnectionClosed = { reason ->
            coroutineScope.launch {
                val newStatus = when (reason?.code) {
                    CloseReason.Codes.NORMAL.code,
                    CloseReason.Codes.GOING_AWAY.code,
                    null -> ConnectionStatus.Disconnected
                    else -> ConnectionStatus.Error
                }

                if (_connectionStatus.value != newStatus) {
                    _connectionStatus.value = newStatus
                    val message = if (newStatus == ConnectionStatus.Disconnected) {
                        "서버 연결이 종료되었습니다."
                    } else {
                        "서버 연결이 비정상적으로 종료되었습니다."
                    }
                    addStatusLog(message, if (newStatus == ConnectionStatus.Disconnected) StatusLogType.WARNING else StatusLogType.ERROR)
                }
            }
        }

        checkAuthStatus()
        setupWebSocketCallbacks()
        connectWebSocket()
        setupContributionMode()
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
            }.onFailure { error ->
                val shouldInvalidateTokens = when (error) {
                    is ClientRequestException -> error.response.status == HttpStatusCode.Unauthorized
                    is ServerResponseException -> error.response.status == HttpStatusCode.Unauthorized
                    is ResponseException -> error.response.status == HttpStatusCode.Unauthorized
                    else -> false
                }

                if (shouldInvalidateTokens) {
                    tokenStorage.clearTokens()
                    _userInfo.value = null
                    _authState.value = AuthState.NotAuthenticated
                } else {
                    Napier.w("Failed to verify auth status: ${error.message}", error, tag = Tags.AUTH)
                    _authState.value = AuthState.Error(error.message ?: "인증 상태 확인 실패")
                }
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
        val currentModes = _selectedSttModes.value

        if (currentModes.size == 1 && currentModes.contains(modeId)) {
            return
        }

        addStatusLog("현재 STT 모드 : ${getSttModeDisplayName(modeId)}", StatusLogType.INFO)
        _selectedSttModes.value = setOf(modeId)
    }

    private fun getSttModeDisplayName(modeId: String): String {
        return when (modeId) {
            "general" -> "일반"
            "number" -> "숫자"
            "alphabet" -> "알파벳"
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

    fun requestContributionTaskInput() {
        _awaitingContributionTask.value = true
        _pendingContributionTask.value = null
    }

    fun clearPendingContributionTask() {
        _pendingContributionTask.value = null
    }

    fun setPendingCommand(command: String) {
        _pendingCommand.value = command
        Napier.i("Pending command set: $command", tag = Tags.APP_VIEWMODEL)
    }

    fun clearPendingCommand() {
        _pendingCommand.value = null
        Napier.d("Pending command cleared", tag = Tags.APP_VIEWMODEL)
    }

    fun cancelActiveAutomation() {
        coroutineScope.launch {
            val wasExecuting = _isExecutingPath.value

            pathExecutor.cancelExecution()
            _isExecutingPath.value = false
            _executionProgress.value = ""
            _currentExecutingPath.value = null
            _currentStepIndex.value = -1
            _currentGraphData.update { it?.copy(activeNodeId = null) }
            userSelectContinuation?.resume("")
            userSelectContinuation = null
            _isWaitingForSelect.value = false
            _selectOptions.value = emptyList()
            _inputRequest.value = null

            if (wasExecuting) {
                addStatusLog("경로 실행이 중단되었습니다.", StatusLogType.WARNING)
            }
        }
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
            _connectionStatus.value = ConnectionStatus.Connecting
            webSocketClient.reconnect()
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

                        if (response.substringAfter("\"transcript\":\"")
                                .substringBefore("\"")
                            .isNotEmpty()) {
                            _receivedMessage.value = response.substringAfter("\"transcript\":\"")
                                .substringBefore("\"")
                            setPendingCommand(response.substringAfter("\"transcript\":\"")
                                .substringBefore("\""))
                        }
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

                    val transcript = voiceResult.transcript?.trim()
                    if (!transcript.isNullOrBlank()) {
                        if (_awaitingContributionTask.value) {
                            _pendingContributionTask.value = transcript
                        } else {
                            _receivedMessage.value = transcript
                            setPendingCommand(transcript)
                        }
                    }
                } else {
                    _recordingStatus.value = "Voice processing failed: ${voiceResult.error?.message ?: "Unknown error"}"
                    addStatusLog("음성 인식 실패: ${voiceResult.error?.message ?: "Unknown error"}", StatusLogType.ERROR)
                }
            }
        }
        Napier.i("WebSocket callbacks setup completed", tag = Tags.APP_VIEWMODEL)
    }

    private fun setupContributionMode() {
        BrowserAutomationBridge.setContributionRecordingCallback { step ->
            contributionModeService.recordStep(step)
        }
        BrowserAutomationBridge.setContributionBrowserClosedCallback {
            handleContributionBrowserClosed()
        }
    }

    fun startContribution(task: String) {
        if (contributionSetupJob?.isActive == true) {
            Napier.w("Contribution initialization already in progress", tag = Tags.CONTRIBUTION_MODE)
            return
        }

        contributionSetupJob = coroutineScope.launch {
            _isContributionInitializing.value = true
            try {
                _awaitingContributionTask.value = false
                _pendingContributionTask.value = null

                BrowserAutomationBridge.setContributionRecordingCallback { step ->
                    contributionModeService.recordStep(step)
                }
                BrowserAutomationBridge.setContributionBrowserClosedCallback {
                    handleContributionBrowserClosed()
                }
                BrowserAutomationBridge.startContributionRecording()

                contributionModeService.startSession(task)

                kotlinx.coroutines.delay(com.vowser.client.contribution.ContributionConstants.BROWSER_INIT_WAIT_MS) // 브라우저 초기화 대기
                BrowserAutomationBridge.navigate("about:blank")

                addStatusLog("$task 기여가 시작되었습니다.", StatusLogType.SUCCESS)
            } catch (e: CancellationException) {
                Napier.i("Contribution initialization cancelled: ${e.message}", tag = Tags.CONTRIBUTION_MODE)
                throw e
            } catch (e: Exception) {
                _awaitingContributionTask.value = true
                exceptionHandler.handleException(e, "Contribution mode initialization") {
                    startContribution(task)
                }
                runCatching { BrowserAutomationBridge.cleanupContribution() }
                    .onFailure {
                        Napier.w("Cleanup after failed contribution initialization: ${it.message}", it, tag = Tags.CONTRIBUTION_MODE)
                    }
                if (contributionModeService.isSessionActive()) {
                    contributionModeService.resetSession()
                }
            } finally {
                _isContributionInitializing.value = false
                contributionSetupJob = null
            }
        }
    }

    fun stopContribution() {
        coroutineScope.launch {
            contributionSetupJob?.cancelAndJoin()
            contributionSetupJob = null
            _isContributionInitializing.value = false

            val stepCount = contributionModeService.currentStepCount.value
            val task = contributionModeService.currentTask.value
            val sessionId = contributionModeService.getCurrentSessionId()
            var stepsSnapshot: List<ContributionStep>
            try {
                runCatching { BrowserAutomationBridge.stopContributionRecording() }
                    .onFailure {
                        Napier.w("Error stopping contribution recording: ${it.message}", it, tag = Tags.CONTRIBUTION_MODE)
                    }

                contributionModeService.endSession()

                addStatusLog("🏁 기여 모드 완료 - 총 ${stepCount}개 스텝 기록됨", StatusLogType.SUCCESS)

                stepsSnapshot = contributionModeService.getCurrentSession()
                    ?.steps
                    ?.toList()
                    .orEmpty()

                if (sessionId != null && task.isNotBlank() && stepsSnapshot.isNotEmpty()) {
                    addStatusLog("경로 데이터 저장 중...", StatusLogType.INFO)
                    saveContributionPath(sessionId, task, stepsSnapshot)
                }
            } catch (e: CancellationException) {
                Napier.i("Contribution stop cancelled: ${e.message}", tag = Tags.CONTRIBUTION_MODE)
                throw e
            } catch (e: Exception) {
                Napier.e("Failed to stop contribution: ${e.message}", e, tag = Tags.CONTRIBUTION_MODE)
                addStatusLog("기여 모드 종료 실패: ${e.message}", StatusLogType.ERROR)
            }

            runCatching { BrowserAutomationBridge.cleanupContribution() }
                .onFailure {
                    Napier.w("Contribution cleanup failed: ${it.message}", it, tag = Tags.CONTRIBUTION_MODE)
                }

            _awaitingContributionTask.value = true
            _pendingContributionTask.value = null
            contributionModeService.resetSession()
        }
    }

    fun cancelContribution() {
        coroutineScope.launch {
            contributionSetupJob?.cancelAndJoin()
            contributionSetupJob = null
            _isContributionInitializing.value = false

            val stepCount = contributionModeService.currentStepCount.value

            runCatching { BrowserAutomationBridge.stopContributionRecording() }
                .onFailure {
                    Napier.w("Error stopping contribution recording during cancel: ${it.message}", it, tag = Tags.CONTRIBUTION_MODE)
                }

            runCatching { BrowserAutomationBridge.cleanupContribution() }
                .onFailure {
                    Napier.w("Contribution cleanup failed during cancel: ${it.message}", it, tag = Tags.CONTRIBUTION_MODE)
                }

            addStatusLog("작업이 중단되었습니다.", StatusLogType.WARNING)
            if (stepCount > 0) {
                addStatusLog("기여 모드가 중단되어 ${stepCount}개 스텝이 폐기되었습니다.", StatusLogType.WARNING)
            }

            contributionModeService.resetSession()

            _awaitingContributionTask.value = true
            _pendingContributionTask.value = null
        }
    }

    fun notifyContributionInitializing() {
        addStatusLog("브라우저를 준비하는 중입니다. 잠시만 기다려 주세요.", StatusLogType.INFO)
    }

    private fun handleContributionBrowserClosed() {
        coroutineScope.launch {
            if (!_isContributionInitializing.value && !contributionModeService.isSessionActive()) {
                return@launch
            }
            addStatusLog("브라우저 창이 닫혀 기여 모드를 중단합니다.", StatusLogType.WARNING)
            cancelContribution()
        }
    }

    /**
     * 기여 경로를 REST API를 통해 저장
     */
    private suspend fun saveContributionPath(
        sessionId: String,
        taskIntent: String,
        steps: List<ContributionStep>
    ) {
        try {
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
                    val savedSteps = response.data.result.stepsSaved
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
                    val paths = response.data.matchedPaths ?: emptyList()
                    _searchedPaths.value = paths

                    // --- 백엔드 원본 데이터 로깅 ---
                    try {
                        val jsonString = json.encodeToString(PathSearchResponse.serializer(), response)
                        Napier.d("Backend Original Response:\n$jsonString", tag = Tags.NETWORK)
                    } catch (e: Exception) {
                        Napier.e("Failed to serialize backend response: ${e.message}", e, tag = Tags.NETWORK)
                    }
                    // ---------------------------

                    if (paths.isEmpty()) {
                        addStatusLog("검색 결과 없음: $transcript", StatusLogType.WARNING)
                        _graphLoading.value = false
                        return@fold
                    }

                    val searchTime = response.data.performance.searchTime ?: 0L
                    addStatusLog(
                        "${paths.size}개 경로 검색됨 (${searchTime}ms)",
                        StatusLogType.SUCCESS
                    )
                    Napier.i("Found ${paths.size} paths for query: $transcript", tag = Tags.APP_VIEWMODEL)

                    val visualizationData = convertToGraph(
                        paths = paths,
                        query = transcript,
                        searchTimeMs = searchTime
                    )
                    _currentGraphData.value = visualizationData
                    _graphLoading.value = false

                    val firstPath = paths.firstOrNull()
                    if (firstPath != null) {
                        addStatusLog(
                            "최적 경로 실행 중: ${firstPath.taskIntent} (관련도: ${(firstPath.relevanceScore * 100).toInt()}%)",
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
            _currentExecutingPath.value = path
            _currentStepIndex.value = -1
            _executionProgress.value = "0/${path.steps.size}"

            val userInfo = authRepository.getMe().getOrNull()
            if (userInfo != null) {
                addStatusLog("사용자 정보 로드 완료 - 자동 입력 활성화", StatusLogType.INFO)
            }

            val result = pathExecutor.executePath(
                path = path,
                userInfo = userInfo,
                onStepComplete = { current, total, description ->
                    _executionProgress.value = "$current/$total"
                    updateActiveNode(pathIndex = 0, stepIndex = current - 1)
                    addStatusLog("[$current/$total] $description", StatusLogType.INFO)
                },

                getUserInput = ::requestUserInput,
                getUserSelect = ::requestUserSelect,
                onLog = { message ->
                    addStatusLog(message, StatusLogType.INFO)
                },
                onWaitForUser = { message ->
                    waitForUserConfirmation(message)
                }
            )

            if (result.success) {
                addStatusLog("경로 실행 완료: ${path.taskIntent}", StatusLogType.SUCCESS)
                Napier.i("Voice command path execution completed: ${path.taskIntent}", tag = Tags.BROWSER_AUTOMATION)
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
            _currentExecutingPath.value = null
            _currentStepIndex.value = -1
            _currentGraphData.update { it?.copy(activeNodeId = null) }
        }
    }

    /**
     * 전체 경로 실행 (MatchedPath를 받아서 모든 스텝 순차 실행)
     */
    private suspend fun executeFullPath(path: com.vowser.client.websocket.dto.MatchedPath) {
        try {
            _isExecutingPath.value = true
            _executionProgress.value = "0/${path.steps.size}"

            val pathDetail = path.toMatchedPathDetail()

            val userInfo = authRepository.getMe().getOrNull()
            if (userInfo != null) {
                addStatusLog("사용자 정보 로드 완료 - 자동 입력 활성화", StatusLogType.INFO)
            }

            val result = pathExecutor.executePath(
                path = pathDetail,
                userInfo = userInfo,
                onStepComplete = { current, total, description ->
                    _currentStepIndex.value = current - 1
                    _executionProgress.value = "$current/$total"
                    updateActiveNode(pathIndex = 0, stepIndex = current - 1)
                    addStatusLog("[$current/$total] $description", StatusLogType.INFO)
                },
                getUserInput = ::requestUserInput,
                getUserSelect = ::requestUserSelect,
                onLog = { message -> addStatusLog(message, StatusLogType.INFO) },
                onWaitForUser = { message -> waitForUserConfirmation(message) }
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
    private fun convertToGraph(
        paths: List<MatchedPathDetail>?,
        query: String? = null,
        searchTimeMs: Long? = null
    ): GraphVisualizationData {
        if (paths.isNullOrEmpty()) {
            return GraphVisualizationData(emptyList(), emptyList(), null, emptyList())
        }
        val nodes = mutableListOf<GraphNode>()
        val edges = mutableListOf<GraphEdge>()
        val firstPath = paths.firstOrNull()

        firstPath?.steps?.forEachIndexed { stepIndex, step ->
            val nodeId = "path0_step${stepIndex}"

            val nodeType = when (step.action.lowercase()) {
                "navigate" -> com.vowser.client.ui.graph.NodeType.NAVIGATE
                "click" -> com.vowser.client.ui.graph.NodeType.CLICK
                "input", "type" -> com.vowser.client.ui.graph.NodeType.INPUT
                "wait" -> com.vowser.client.ui.graph.NodeType.WAIT
                else -> com.vowser.client.ui.graph.NodeType.ACTION
            }

            nodes.add(
                GraphNode(
                    id = nodeId,
                    label = step.description,
                    type = nodeType
                )
            )

            if (stepIndex > 0) {
                edges.add(
                    GraphEdge(
                        from = "path0_step${stepIndex - 1}",
                        to = nodeId
                    )
                )
            }
        }

        // 검색 정보 생성
        val searchInfo = if (query != null && searchTimeMs != null && paths.isNotEmpty()) {
            com.vowser.client.visualization.SearchInfo(
                query = query,
                totalPaths = paths.size,
                searchTimeMs = searchTimeMs,
                topRelevance = paths.firstOrNull()?.relevanceScore?.toFloat()
            )
        } else null

        return GraphVisualizationData(
            nodes = nodes,
            edges = edges,
            searchInfo = searchInfo,
            allMatchedPaths = paths
        )
    }

    private suspend fun requestUserInput(step: com.vowser.client.api.dto.PathStepDetail): String {
        return suspendCancellableCoroutine { continuation ->
            _isWaitingForUserInput.value = true
            _inputRequest.value = step
            userInputContinuation = continuation

            continuation.invokeOnCancellation {
                _isWaitingForUserInput.value = false
                _inputRequest.value = null
                userInputContinuation = null
            }
        }
    }

    private suspend fun requestUserSelect(step: com.vowser.client.api.dto.PathStepDetail,
                                          options: List<SelectOption>): String {
        return suspendCancellableCoroutine { continuation ->
            _isWaitingForSelect.value = true
            _isWaitingForUserInput.value = false
            _inputRequest.value = step
            _selectOptions.value = options
            userSelectContinuation = continuation
            addStatusLog("옵션 선택 필요: ${step.description}", StatusLogType.INFO)

            continuation.invokeOnCancellation {
                _isWaitingForSelect.value = false
                _selectOptions.value = emptyList()
                _inputRequest.value = null
                userSelectContinuation = null
            }
        }
    }

    /**
     * 사용자 확인을 기다리는 suspend 함수
     * - PathExecutor의 onWaitForUser 콜백에 전달됩니다
     * - UI에서 사용자가 확인 버튼을 누를 때까지 대기합니다
     */
    private suspend fun waitForUserConfirmation(message: String) {
        suspendCancellableCoroutine { continuation ->
            _isWaitingForUser.value = true
            _waitMessage.value = message
            waitContinuation = continuation

            continuation.invokeOnCancellation {
                _isWaitingForUser.value = false
                _waitMessage.value = ""
                waitContinuation = null
            }
        }
    }

    private fun nodeIdFor(pathIndex: Int, stepIndex: Int) = "path${pathIndex}_step${stepIndex}"

    private fun updateActiveNode(pathIndex: Int, stepIndex: Int) {
        val id = nodeIdFor(pathIndex, stepIndex)
        val highlighted = (0..stepIndex).map { idx -> nodeIdFor(pathIndex, idx) }
        _currentGraphData.update { curr ->
            curr?.copy(
                activeNodeId = id,
                highlightedPath = highlighted
            )
        }
    }

    fun submitUserInput(input: String) {
        userInputContinuation?.resume(input)
        userInputContinuation = null
        _isWaitingForUserInput.value = false
        _inputRequest.value = null
        addStatusLog("사용자 입력: $input", StatusLogType.INFO)
    }

    fun submitUserSelect(value: String) {
        val label = _selectOptions.value.firstOrNull { it.value == value }?.label ?: value
        userSelectContinuation?.resume(value)
        userSelectContinuation = null
        _isWaitingForSelect.value = false
        _selectOptions.value = emptyList()
        _inputRequest.value = null
        addStatusLog("사용자 선택: $label", StatusLogType.INFO)
    }

    fun cancelUserInput() {
        userInputContinuation?.resume("")
        userInputContinuation = null
        _isWaitingForUserInput.value = false
        _inputRequest.value = null
        addStatusLog("사용자 입력 취소", StatusLogType.WARNING)
    }

    fun cancelUserSelect() {
        userSelectContinuation?.resume("")
        userSelectContinuation = null
        _isWaitingForSelect.value = false
        _selectOptions.value = emptyList()
        _inputRequest.value = null
        addStatusLog("사용자 선택 취소", StatusLogType.WARNING)
    }

    /**
     * 사용자가 확인 버튼을 누를 때 호출되는 함수
     * - UI에서 호출합니다
     */
    fun confirmUserWait() {
        waitContinuation?.resume(Unit)
        waitContinuation = null
        _isWaitingForUser.value = false
        _waitMessage.value = ""
        addStatusLog("✅ 사용자 확인 완료 - 다음 단계 진행", StatusLogType.SUCCESS)
    }
}

expect suspend fun startPlatformRecording(): Boolean
expect suspend fun stopPlatformRecording(): ByteArray?
expect fun openUrlInBrowser(url: String)
