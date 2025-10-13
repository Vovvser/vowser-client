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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vowser.client.AppViewModel
import com.vowser.client.ui.components.HomeAppBar
import com.vowser.client.ui.theme.AppTheme
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
    viewModel: AppViewModel,
    onScreenChange: (AppScreen) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedMode by remember { mutableStateOf(SearchMode.EXECUTE) }
    val authState by viewModel.authState.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val receivedMessage by viewModel.receivedMessage.collectAsState()

    // 음성 인식 결과 처리
    LaunchedEffect(receivedMessage, isRecording) {
        if (!isRecording && receivedMessage != "No message" && receivedMessage.isNotBlank()) {
            when (selectedMode) {
                SearchMode.SEARCH -> {
                    // TODO: 검색
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
            Divider(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                thickness = 1.dp
            )
        }
    ) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
        ) {
            BoxWithConstraints (
                Modifier
                    .fillMaxSize()
                    .padding(maxWidth * 0.03f, maxHeight * 0.1f,  maxWidth * 0.03f, 0.dp),
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier
                        .fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = AppTheme.Dimensions.paddingXLarge, end = AppTheme.Dimensions.paddingLarge, top = AppTheme.Dimensions.paddingXLarge,
                        bottom = minHeight -  (maxHeight * 0.8f),
                    ),
                    horizontalArrangement = Arrangement.spacedBy(maxWidth * 0.05f),
                    verticalArrangement = Arrangement.spacedBy(maxHeight * 0.03f)
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
                        .fillMaxWidth()
                        .padding(
                            horizontal = maxWidth * 0.1f,
                            vertical = maxHeight * 0.05f
                        )
                ) {
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
                                                    viewModel.executeQuery(searchQuery)
                                                    onScreenChange(AppScreen.GRAPH)
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