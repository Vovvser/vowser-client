package com.vowser.client.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vowser.client.AppViewModel
import com.vowser.client.ui.components.HomeAppBar
import com.vowser.client.ui.components.SttModeSelector
import com.vowser.client.ui.theme.AppTheme
import com.vowser.client.ui.navigation.LocalScreenNavigator
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

/**
 * 검색 모드
 */
enum class SearchMode {
    SEARCH,  // 검색 모드
    EXECUTE  // 실행 모드
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalResourceApi::class)
@Composable
fun HomeScreen(
    viewModel: AppViewModel
) {
    val navigator = LocalScreenNavigator.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedMode by remember { mutableStateOf(SearchMode.EXECUTE) }
    val authState by viewModel.authState.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val receivedMessage by viewModel.receivedMessage.collectAsState()
    val pendingCommand by viewModel.pendingCommand.collectAsState()
    val selectedSttModes by viewModel.selectedSttModes.collectAsState()
    val isContributionScreenActive by viewModel.isContributionScreenActive.collectAsState()

    // 음성 인식 결과 처리
    LaunchedEffect(receivedMessage, isRecording, pendingCommand) {
        val command = pendingCommand
        if (!isRecording &&
            !command.isNullOrBlank() &&
            receivedMessage != "No message" &&
            receivedMessage.isNotBlank() &&
            !isContributionScreenActive
        ) {
            when (selectedMode) {
                SearchMode.SEARCH -> { /* TODO : 검색*/
                }

                SearchMode.EXECUTE -> {
                    viewModel.setPendingCommand(command)
                    navigator.push(AppScreen.GRAPH)
                }
            }
        }
    }
    Scaffold(
        topBar = {
            Column {
                HomeAppBar(
                    isLoggedIn = authState is com.vowser.client.model.AuthState.Authenticated,
                    onContribution = { navigator.push(AppScreen.CONTRIBUTION) },
                    onOpenSettings = { navigator.push(AppScreen.SETTINGS) },
                    onOpenUser = {
                        if (authState is com.vowser.client.model.AuthState.Authenticated) {
                            navigator.push(AppScreen.USER)
                        } else viewModel.login()
                    },
                    onLogin = { viewModel.login() }
                )
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), thickness = 1.dp)
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 32.dp, vertical = 20.dp),
            contentPadding = PaddingValues(bottom = 132.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            @Composable
            fun CardContent(text: String) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(124.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = AppTheme.Typography.titleMedium
                    )
                }
            }

            item {
                Text(
                    "인기 경로",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = AppTheme.Typography.titleLarge
                )
            }

//                LazyVerticalGrid(
//                    columns = GridCells.Fixed(4),
//                    modifier = Modifier
//                        .fillMaxSize(),
//                    contentPadding = PaddingValues(
//                        start = AppTheme.Dimensions.paddingMedium,
//                        end = AppTheme.Dimensions.paddingMedium,
//                        top = AppTheme.Dimensions.paddingLarge,
//                        bottom = minHeight - (maxHeight * 0.8f),
//                    ),
//                    horizontalArrangement = Arrangement.spacedBy(maxWidth * 0.06f),
//                    verticalArrangement = Arrangement.spacedBy(maxHeight * 0.04f)
//                ) {
//                    items(3) { index -> // TODO: 나중에 API로 항목 동적 로드
//                    }
//                }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Card(modifier = Modifier.weight(1f), onClick = {
                        selectedMode = SearchMode.EXECUTE
                        val q = "SRT 예매 자동화 (Python Playwright)"
                        searchQuery = q; viewModel.setPendingCommand(q); navigator.push(AppScreen.GRAPH)
                    }) { CardContent("SRT 예매") }
                    Card(modifier = Modifier.weight(1f), onClick = {
                        selectedMode = SearchMode.EXECUTE
                        val q = "주민등록등본 발급해줘"
                        searchQuery = q; viewModel.setPendingCommand(q); navigator.push(AppScreen.GRAPH)
                    }) { CardContent("주민등록등본 발급") }
                    Card(modifier = Modifier.weight(1f), onClick = {
                        selectedMode = SearchMode.EXECUTE
                        val q = "오늘 날씨 보여줘"
                        searchQuery = q; viewModel.setPendingCommand(q); navigator.push(AppScreen.GRAPH)
                    }) { CardContent("오늘 날씨") }
                }
            }

            item {
                Text(
                    "전체 경로",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = AppTheme.Typography.titleLarge
                )
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Card(modifier = Modifier.weight(1f), onClick = {
                        selectedMode = SearchMode.EXECUTE
                        searchQuery = "한국장애인복지시설협회 장애인 시설 검색"
                        viewModel.setPendingCommand(searchQuery)
                        navigator.push(AppScreen.GRAPH)
                    }) { CardContent("한국 장애인 복지시설협회\n장애인 시설 검색") }

                    Card(modifier = Modifier.weight(1f), onClick = {
                        selectedMode = SearchMode.EXECUTE
                        searchQuery = "중앙보조기기센터 보조기기 검색"
                        viewModel.setPendingCommand(searchQuery)
                        navigator.push(AppScreen.GRAPH)
                    }) { CardContent("중앙보조기기센터\n보조기기 검색") }

                    Card(modifier = Modifier.weight(1f), onClick = {
                        selectedMode = SearchMode.EXECUTE
                        searchQuery = "다음 페이지로 이동"
                        viewModel.setPendingCommand(searchQuery)
                        navigator.push(AppScreen.GRAPH)
                    }) { CardContent("위키피디아로 이동") }
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Card(modifier = Modifier.weight(1f), onClick = {
                        selectedMode = SearchMode.EXECUTE
                        searchQuery = "네이버 증권"
                        viewModel.setPendingCommand(searchQuery)
                        navigator.push(AppScreen.GRAPH)
                    }) { CardContent("네이버 증권으로 이동") }

                    Card(modifier = Modifier.weight(1f), onClick = {
                        selectedMode = SearchMode.EXECUTE
                        searchQuery = "네이버 미세먼지 날씨"
                        viewModel.setPendingCommand(searchQuery)
                        navigator.push(AppScreen.GRAPH)
                    }) { CardContent("네이버 미세먼지 날씨") }

                    Card(modifier = Modifier.weight(1f), onClick = {
                        selectedMode = SearchMode.EXECUTE
                        searchQuery = "네이버 로맨스 웹툰 추천"
                        viewModel.setPendingCommand(searchQuery)
                        navigator.push(AppScreen.GRAPH)
                    }) { CardContent("네이버 로맨스 웹툰 가기 ") }
                }
            }
        }

        // 하단 검색창
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            SttModeSelector(
                selectedMode = selectedSttModes.firstOrNull(),
                onModeSelect = { mode -> viewModel.toggleSttMode(mode) },
                isVisible = !isRecording,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = AppTheme.Dimensions.cardElevation,
                        shape = RoundedCornerShape(AppTheme.Dimensions.borderRadiusXLarge),
                        clip = true
                    )
                    .background(
                        MaterialTheme.colorScheme.background,
                        RoundedCornerShape(AppTheme.Dimensions.borderRadiusXLarge)
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(AppTheme.Dimensions.borderRadiusXLarge)
                    )
                    .padding(0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val dotColor = MaterialTheme.colorScheme.onBackground

                Row(
                    modifier = Modifier.padding(horizontal = AppTheme.Dimensions.paddingMedium),
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.Dimensions.paddingMedium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 검색 모드 버튼
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { selectedMode = SearchMode.SEARCH }
                    ) {
                        Spacer(modifier = Modifier.width(AppTheme.Dimensions.paddingSmall))
                        if (selectedMode == SearchMode.SEARCH) {
                            Canvas(modifier = Modifier.size(10.dp)) {
                                drawCircle(color = dotColor)
                            }
                        } else {
                            Spacer(modifier = Modifier.width(10.dp))
                        }
                        Spacer(modifier = Modifier.width(AppTheme.Dimensions.paddingSmall))
                        Text(
                            "검색",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.width(AppTheme.Dimensions.paddingSmall))
                    }

                    // 실행 모드 버튼
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { selectedMode = SearchMode.EXECUTE }
                    ) {
                        Spacer(modifier = Modifier.width(AppTheme.Dimensions.paddingSmall))
                        if (selectedMode == SearchMode.EXECUTE) {
                            Canvas(modifier = Modifier.size(12.dp)) {
                                drawCircle(color = dotColor)
                            }
                        } else {
                            Spacer(modifier = Modifier.width(10.dp))
                        }
                        Spacer(modifier = Modifier.width(AppTheme.Dimensions.paddingSmall))
                        Text(
                            "실행",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.width(AppTheme.Dimensions.paddingSmall))
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            MaterialTheme.colorScheme.surface,
                            RoundedCornerShape(AppTheme.Dimensions.borderRadiusXLarge)
                        )
                        .padding(0.dp)
                ) {
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        keyboardOptions = KeyboardOptions.Default.copy(
                            imeAction = ImeAction.Search
                        ),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                if (searchQuery.isNotBlank()) {
                                    when (selectedMode) {
                                        SearchMode.SEARCH -> {
                                            // TODO: 검색
                                        }

                                        SearchMode.EXECUTE -> {
                                            viewModel.setPendingCommand(searchQuery)
                                            navigator.push(AppScreen.GRAPH)
                                        }
                                    }
                                }
                            }
                        ),
                        decorationBox = { inner ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = when (selectedMode) {
                                        SearchMode.SEARCH -> "검색어를 입력하세요..."
                                        SearchMode.EXECUTE -> "실행 명령어를 입력하세요..."
                                    },
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            inner()
                        }
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 녹음 버튼
                IconButton(
                    onClick = {
                        viewModel.toggleRecording()
                    }
                ) {
                    if (isRecording) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Stop Recording",
                            tint = AppTheme.Colors.Error
                        )
                    } else {
                        Icon(
                            painter = painterResource("drawable/microphone.png"),
                            contentDescription = "Start Recording",
                            Modifier.size(AppTheme.Dimensions.iconSizeMedium),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

            }
        }
    }
}
