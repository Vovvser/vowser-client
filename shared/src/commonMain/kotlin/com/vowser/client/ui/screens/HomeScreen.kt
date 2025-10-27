package com.vowser.client.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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
                SearchMode.SEARCH -> { /* TODO : 검색*/ }
                SearchMode.EXECUTE -> {
                    viewModel.setPendingCommand(command)
                    navigator.push(AppScreen.GRAPH)
                }
            }
        }
    }
    Scaffold(
        topBar = {
            HomeAppBar(
                isLoggedIn = authState is com.vowser.client.model.AuthState.Authenticated,
                onContribution = { navigator.push(AppScreen.CONTRIBUTION) },
                onOpenSettings = { navigator.push(AppScreen.SETTINGS) },
                onOpenUser = {
                    if (authState is com.vowser.client.model.AuthState.Authenticated) {
                        navigator.push(AppScreen.USER)
                    } else {
                        viewModel.login()
                    }
                },
                onLogin = { viewModel.login() }
            )
            Divider(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                thickness = 1.dp
            )
        }
    ) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .padding(30.dp, 80.dp, 0.dp, 0.dp),
            ) {

            Text("인기 경로",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = AppTheme.Typography.titleLarge)
            Spacer(modifier = Modifier.size(16.dp))

            BoxWithConstraints(
                Modifier
                    .fillMaxSize()
                    .padding(top = 20.dp)
            ) {
                val srtQuery = "SRT 예매 자동화 (Python Playwright)"

                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier
                        .fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = AppTheme.Dimensions.paddingMedium,
                        end = AppTheme.Dimensions.paddingMedium,
                        top = AppTheme.Dimensions.paddingLarge,
                        bottom = minHeight - (maxHeight * 0.8f),
                    ),
                    horizontalArrangement = Arrangement.spacedBy(maxWidth * 0.06f),
                    verticalArrangement = Arrangement.spacedBy(maxHeight * 0.04f)
                ) {
                    items(1) { index -> // TODO: 나중에 API로 항목 동적 로드
                        // SRT 예매 카드
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Transparent)
                                .border(0.dp, Color.Transparent, RoundedCornerShape(0.dp))
                                .aspectRatio(1f)
                                .clickable {
                                    selectedMode = SearchMode.EXECUTE
                                    searchQuery = srtQuery
                                    viewModel.setPendingCommand(srtQuery)
                                    navigator.push(AppScreen.GRAPH)
                                },
                        ) {
                            Column (
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(AppTheme.Dimensions.paddingSmall),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize()
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .border(0.dp, MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(0.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "SRT 예매",
                                        color = MaterialTheme.colorScheme.onBackground,
                                        fontSize = AppTheme.Typography.titleLarge,
                                    )
                                }
                                Text(
                                    "SRT 예매",
                                    fontSize = MaterialTheme.typography.bodySmall.fontSize,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // 하단 검색창
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(
                            horizontal = maxWidth * 0.1f,
                            vertical = maxHeight * 0.05f
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally
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
    }
}
