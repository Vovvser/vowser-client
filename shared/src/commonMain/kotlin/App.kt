import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.Alignment
import com.vowser.client.AppViewModel
import com.vowser.client.ui.navigation.LocalScreenNavigator
import com.vowser.client.ui.navigation.StackScreenNavigator
import com.vowser.client.ui.screens.AppScreen
import com.vowser.client.ui.screens.ContributionScreen
import com.vowser.client.ui.screens.GraphScreen
import com.vowser.client.ui.screens.HomeScreen
import com.vowser.client.ui.screens.SettingsScreen
import com.vowser.client.ui.screens.UserScreen
import com.vowser.client.ui.theme.AppTheme

@Composable
fun App(viewModel: AppViewModel) {
    val screenStack = remember { mutableStateListOf(AppScreen.HOME) }
    val navigator = remember { StackScreenNavigator(screenStack) }
    val currentScreen = navigator.current()

    var isDarkTheme by remember { mutableStateOf(false) }
    var isDeveloperMode by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.checkAuthStatus()
    }

    // 사용자 대기 상태 구독
    val isWaitingForUser by viewModel.isWaitingForUser.collectAsState()
    val waitMessage by viewModel.waitMessage.collectAsState()

    // 사용자 입력 대기 상태 구독
    val isWaitingForUserInput by viewModel.isWaitingForUserInput.collectAsState()
    val inputRequest by viewModel.inputRequest.collectAsState()
    val isWaitingForSelect by viewModel.isWaitingForSelect.collectAsState()
    val selectOptions by viewModel.selectOptions.collectAsState()

    // 테마 적용
    val colors = if (isDarkTheme) {
        AppTheme.DarkTheme
    } else {
        AppTheme.LightTheme
    }

    MaterialTheme(
        colorScheme = colors
    ) {
        CompositionLocalProvider(LocalScreenNavigator provides navigator) {
            when (currentScreen) {
                AppScreen.HOME -> {
                    HomeScreen(viewModel = viewModel)
                }

                AppScreen.GRAPH -> {
                    GraphScreen(
                        appViewModel = viewModel,
                        isDeveloperMode = isDeveloperMode,
                        onReconnect = { viewModel.reconnect() },
                        onClearStatusHistory = { viewModel.clearStatusHistory() },
                        onConfirmUserWait = { viewModel.confirmUserWait() },
                        onNavigateBack = { navigator.pop() },
                    )
                }

                AppScreen.CONTRIBUTION -> ContributionScreen(appViewModel = viewModel)
                AppScreen.SETTINGS -> {
                    SettingsScreen(
                        isDarkTheme = isDarkTheme,
                        isDeveloperMode = isDeveloperMode,
                        onThemeToggle = { isDarkTheme = it },
                        onDeveloperModeToggle = { isDeveloperMode = it },
                    )
                }

                AppScreen.USER -> {
                    UserScreen(
                        viewModel = viewModel
                    )
                }
            }
        }
        if (isWaitingForSelect) {
            AlertDialog(
                containerColor = MaterialTheme.colorScheme.background,
                onDismissRequest = { viewModel.cancelUserSelect() },
                title = {
                    Text(
                        inputRequest?.description ?: "옵션 선택",
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                text = {
                    val listState = rememberLazyListState()
                    Column(
                        verticalArrangement = Arrangement.spacedBy(AppTheme.Dimensions.paddingSmall)
                    ) {
                        Text(
                            text = "아래의 옵션 중에서 선택해주세요.",
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Box(modifier = Modifier.heightIn(max = 280.dp)) {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth(),
                                state = listState,
                                verticalArrangement = Arrangement.spacedBy(AppTheme.Dimensions.paddingXSmall)
                            ) {
                                items(selectOptions) { option ->
                                    OutlinedButton(
                                        onClick = { viewModel.submitUserSelect(option.value) },
                                        modifier = Modifier.fillMaxWidth()
                                            .padding(end = AppTheme.Dimensions.paddingMedium),
                                        shape = RoundedCornerShape(AppTheme.Dimensions.borderRadiusLarge),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.background,
                                            contentColor = MaterialTheme.colorScheme.onBackground
                                        )
                                    ) {
                                        Text(option.label, color = MaterialTheme.colorScheme.onBackground)
                                    }
                                }
                            }
                            VerticalScrollbar(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .fillMaxHeight(),
                                adapter = androidx.compose.foundation.rememberScrollbarAdapter(listState)
                            )
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onBackground,
                            contentColor = MaterialTheme.colorScheme.background
                        ),
                        onClick = { viewModel.cancelUserSelect() }
                    ) {
                        Text(
                            "취소",
                            color = MaterialTheme.colorScheme.background
                        )
                    }
                }
            )
        } else if (isWaitingForUserInput) {
            var inputText by remember { mutableStateOf("") }
            AlertDialog(
                containerColor = MaterialTheme.colorScheme.background,
                onDismissRequest = { viewModel.cancelUserInput() },
                title = {
                    Text(
                        "사용자 입력",
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                },
                text = {
                    Column {
                        Text(
                            inputRequest?.description ?: "입력 값이 필요합니다.",
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(AppTheme.Dimensions.paddingSmall))
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            label = { Text(inputRequest?.textLabels?.joinToString(", ") ?: "입력") }
                        )
                    }
                },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onBackground,
                            contentColor = MaterialTheme.colorScheme.background
                        ),
                        onClick = { viewModel.submitUserInput(inputText) }) {
                        Text(
                            "확인",
                            color = MaterialTheme.colorScheme.background
                        )
                    }
                },
                dismissButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onBackground,
                            contentColor = MaterialTheme.colorScheme.background
                        ),
                        onClick = { viewModel.cancelUserInput() }) {
                        Text("취소")
                    }
                }
            )
        }
    }
}

expect fun getPlatformName(): String
