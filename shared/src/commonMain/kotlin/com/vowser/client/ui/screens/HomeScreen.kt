package com.vowser.client.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.vowser.client.AppViewModel
import com.vowser.client.ui.components.HomeAppBar
import com.vowser.client.ui.theme.AppTheme

/**
 * 검색 모드
 */
enum class SearchMode {
    SEARCH,  // 검색 모드
    EXECUTE  // 실행 모드
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: AppViewModel,
    onScreenChange: (AppScreen) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedMode by remember { mutableStateOf(SearchMode.EXECUTE) }
    val authState by viewModel.authState.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val receivedMessage by viewModel.receivedMessage.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // 음성 인식 결과 처리
    LaunchedEffect(receivedMessage, isRecording) {
        if (!isRecording && receivedMessage != "No message" && receivedMessage.isNotBlank()) {
            when (selectedMode) {
                SearchMode.SEARCH -> {
                    // TODO: 검색 API 구현 대기 중
                }
                SearchMode.EXECUTE -> {
                    searchQuery = receivedMessage
                    onScreenChange(AppScreen.GRAPH)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            HomeAppBar(
                isLoggedIn = authState is com.vowser.client.model.AuthState.Authenticated,
                onScreenChange = onScreenChange,
                onLogin = { viewModel.login() }
            )
        }
    ) { paddingValues ->
        Box(Modifier.fillMaxSize().padding(paddingValues)) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 32.dp, end = 32.dp, top = 16.dp,
                    bottom = 108.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(50) { index ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Webpage ${index + 1}",
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // 하단 검색창
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = AppTheme.Dimensions.paddingXLarge, vertical = AppTheme.Dimensions.paddingXLarge)
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("검색어를 입력하세요.") },
                    leadingIcon = {
                        Column(
                            modifier = Modifier.padding(start = AppTheme.Dimensions.paddingSmall),
                            verticalArrangement = Arrangement.spacedBy(AppTheme.Dimensions.paddingXSmall)
                        ) {
                            // 검색 라디오 버튼
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .height(AppTheme.Dimensions.iconSizeMedium)
                                    .clickable { selectedMode = SearchMode.SEARCH }
                            ) {
                                RadioButton(
                                    selected = selectedMode == SearchMode.SEARCH,
                                    onClick = { selectedMode = SearchMode.SEARCH },
                                    modifier = Modifier.size(AppTheme.Dimensions.iconSizeSmall)
                                )
                                Spacer(modifier = Modifier.width(AppTheme.Dimensions.paddingXSmall))
                                Text(
                                    "검색",
                                    fontSize = AppTheme.Typography.overline,
                                    color = if (selectedMode == SearchMode.SEARCH)
                                        MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }

                            // 실행 라디오 버튼
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .height(AppTheme.Dimensions.iconSizeMedium)
                                    .clickable { selectedMode = SearchMode.EXECUTE }
                            ) {
                                RadioButton(
                                    selected = selectedMode == SearchMode.EXECUTE,
                                    onClick = { selectedMode = SearchMode.EXECUTE },
                                    modifier = Modifier.size(AppTheme.Dimensions.iconSizeSmall)
                                )
                                Spacer(modifier = Modifier.width(AppTheme.Dimensions.paddingXSmall))
                                Text(
                                    "실행",
                                    fontSize = AppTheme.Typography.overline,
                                    color = if (selectedMode == SearchMode.EXECUTE)
                                        MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    },
                    trailingIcon = {
                        TextButton(
                            onClick = {
                                viewModel.toggleRecording()
                            },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = if (isRecording) AppTheme.Colors.Error
                                              else MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(
                                text = if (isRecording) "녹음 중지" else "마이크",
                                fontSize = AppTheme.Typography.bodyMedium
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = AppTheme.Dimensions.cardElevation, shape = RoundedCornerShape(AppTheme.Dimensions.borderRadiusXLarge)),
                    shape = RoundedCornerShape(AppTheme.Dimensions.borderRadiusXLarge),
                    singleLine = true,
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
                                        viewModel.executeQuery(searchQuery)
                                        onScreenChange(AppScreen.GRAPH)
                                    }
                                }
                            }
                        }
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        errorIndicatorColor = Color.Transparent,
                    )
                )
            }
        }
    }
}