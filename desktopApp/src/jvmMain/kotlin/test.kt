import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.vowser.client.ui.graph.*
import com.vowser.client.data.*
import com.vowser.client.navigation.*
import kotlinx.coroutines.launch

@Composable
@Preview
fun TestGraphApp() {
    var currentCommand by remember { mutableStateOf("") }
    var navigationResult by remember { mutableStateOf<NavigationResult?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var selectedCommand by remember { mutableStateOf<String?>(null) }
    var isDataLoaded by remember { mutableStateOf(false) }
    
    var navigationGraph by remember { 
        mutableStateOf<WebNavigationGraph?>(null)
    }
    val navigationProcessor = remember(navigationGraph) { 
        navigationGraph?.let { NavigationProcessor(it) }
    }
    
    val coroutineScope = rememberCoroutineScope()
    
    LaunchedEffect(Unit) {
        if (!isDataLoaded) {
            val graph = ExpandedNaverDataGenerator.createExpandedNaverData()
            navigationGraph = graph
            isDataLoaded = true
        }
    }
    
    MaterialTheme {
        if (!isDataLoaded) {
            LoadingScreen()
        } else {
            MainAppContent(
                currentCommand = currentCommand,
                onCommandChange = { currentCommand = it },
                navigationResult = navigationResult,
                isProcessing = isProcessing,
                selectedCommand = selectedCommand,
                onCommandSelected = { command ->
                    selectedCommand = command
                    currentCommand = command
                },
                onProcessCommand = { command ->
                    navigationProcessor?.let { processor ->
                        coroutineScope.launch {
                            isProcessing = true
                            try {
                                navigationResult = processor.processVoiceCommand(command)
                            } finally {
                                isProcessing = false
                            }
                        }
                    }
                },
                navigationProcessor = navigationProcessor
            )
        }
    }
}

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colors.primary
            )
            Text(
                text = "데이터 로딩 중...",
                style = MaterialTheme.typography.h6
            )
            Text(
                text = "웹 탐색 그래프를 준비하고 있습니다",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun MainAppContent(
    currentCommand: String,
    onCommandChange: (String) -> Unit,
    navigationResult: NavigationResult?,
    isProcessing: Boolean,
    selectedCommand: String?,
    onCommandSelected: (String) -> Unit,
    onProcessCommand: (String) -> Unit,
    navigationProcessor: NavigationProcessor?
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        LeftControlPanel(
            currentCommand = currentCommand,
            onCommandChange = onCommandChange,
            navigationResult = navigationResult,
            isProcessing = isProcessing,
            selectedCommand = selectedCommand,
            onCommandSelected = onCommandSelected,
            onProcessCommand = onProcessCommand,
            modifier = Modifier.weight(0.4f)
        )
        
        RightVisualizationPanel(
            navigationResult = navigationResult,
            navigationProcessor = navigationProcessor,
            isProcessing = isProcessing,
            modifier = Modifier.weight(0.6f)
        )
    }
}

@Composable
fun LeftControlPanel(
    currentCommand: String,
    onCommandChange: (String) -> Unit,
    navigationResult: NavigationResult?,
    isProcessing: Boolean,
    selectedCommand: String?,
    onCommandSelected: (String) -> Unit,
    onProcessCommand: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AppHeader()
        
        CommandInputCard(
            currentCommand = currentCommand,
            onCommandChange = onCommandChange,
            onProcessCommand = onProcessCommand,
            isProcessing = isProcessing
        )
        
        SampleCommandsCard(
            selectedCommand = selectedCommand,
            onCommandSelected = onCommandSelected,
            onProcessCommand = onProcessCommand,
            isProcessing = isProcessing
        )
        
        navigationResult?.let { result ->
            ResultCard(result = result)
        }
    }
}

@Composable
fun AppHeader() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp,
        backgroundColor = MaterialTheme.colors.primary
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "가던길 (Desire Path)",
                    style = MaterialTheme.typography.h5,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "음성 명령 기반 웹 접근성 도구",
                style = MaterialTheme.typography.caption,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun CommandInputCard(
    currentCommand: String,
    onCommandChange: (String) -> Unit,
    onProcessCommand: (String) -> Unit,
    isProcessing: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colors.primary
                )
                Text(
                    text = "음성 명령어",
                    style = MaterialTheme.typography.h6
                )
            }
            
            OutlinedTextField(
                value = currentCommand,
                onValueChange = onCommandChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("예: 인기 웹툰 보고 싶어") },
                enabled = !isProcessing,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Create,
                        contentDescription = null
                    )
                }
            )
            
            Button(
                onClick = { onProcessCommand(currentCommand) },
                modifier = Modifier.fillMaxWidth(),
                enabled = currentCommand.isNotBlank() && !isProcessing
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("처리 중...")
                } else {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("경로 찾기")
                }
            }
        }
    }
}

@Composable
fun SampleCommandsCard(
    selectedCommand: String?,
    onCommandSelected: (String) -> Unit,
    onProcessCommand: (String) -> Unit,
    isProcessing: Boolean
) {
    val sampleCommands = remember {
        listOf(
            "인기 웹툰 보고 싶어" to Icons.Default.Star,
            "로맨스 웹툰 읽고 싶어" to Icons.Default.Favorite,
            "정치 뉴스 알려줘" to Icons.Default.Info,
            "야구 뉴스 보고 싶어" to Icons.Default.Star,
            "화장품 쇼핑하고 싶어" to Icons.Default.ShoppingCart,
            "요리 레시피 찾아줘" to Icons.Default.Home
        )
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.List,
                    contentDescription = null,
                    tint = MaterialTheme.colors.primary
                )
                Text(
                    text = "샘플 명령어",
                    style = MaterialTheme.typography.h6
                )
            }
            
            sampleCommands.forEach { (command, icon) ->
                SampleCommandItem(
                    command = command,
                    icon = icon,
                    isSelected = selectedCommand == command,
                    onClick = {
                        onCommandSelected(command)
                        onProcessCommand(command)
                    },
                    enabled = !isProcessing
                )
            }
        }
    }
}

@Composable
fun SampleCommandItem(
    command: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() },
        backgroundColor = if (isSelected) {
            MaterialTheme.colors.primary.copy(alpha = 0.1f)
        } else Color.White,
        elevation = if (isSelected) 4.dp else 1.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (isSelected) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )
            Text(
                text = command,
                style = MaterialTheme.typography.body2,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                modifier = Modifier.weight(1f)
            )
            if (enabled) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colors.primary.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun ResultCard(result: NavigationResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp,
        backgroundColor = if (result.success) {
            Color(0xFFE8F5E8)
        } else {
            Color(0xFFFFF3E0)
        }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (result.success) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (result.success) Color(0xFF2E7D32) else Color(0xFFE65100)
                )
                Text(
                    text = if (result.success) "성공" else "알림",
                    style = MaterialTheme.typography.h6,
                    color = if (result.success) Color(0xFF2E7D32) else Color(0xFFE65100)
                )
            }
            
            Text(
                text = result.message,
                style = MaterialTheme.typography.body2
            )
            
            if (result.success && result.estimatedTime > 0) {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "${result.estimatedTime}초",
                            style = MaterialTheme.typography.caption
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "${result.totalClicks}번",
                            style = MaterialTheme.typography.caption
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RightVisualizationPanel(
    navigationResult: NavigationResult?,
    navigationProcessor: NavigationProcessor?,
    isProcessing: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = 2.dp
    ) {
        Column {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = 0.dp,
                backgroundColor = MaterialTheme.colors.surface
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = MaterialTheme.colors.primary
                    )
                    Text(
                        text = "웹 탐색 경로",
                        style = MaterialTheme.typography.h6
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            }
            
            Divider()
            
            Box(modifier = Modifier.fillMaxSize()) {
                val currentVisualizationData = remember(navigationResult) {
                    navigationResult?.visualizationData 
                        ?: navigationProcessor?.getCurrentVisualizationData()
                }
                
                if (currentVisualizationData != null) {
                    AnimatedNavigationGraph(
                        nodes = currentVisualizationData.nodes,
                        edges = currentVisualizationData.edges,
                        highlightedPath = currentVisualizationData.highlightedPath,
                        activeNodeId = currentVisualizationData.activeNodeId,
                        isNavigationActive = isProcessing || (navigationResult?.success == true),
                        modifier = Modifier.fillMaxSize()
                    ) { node ->
                        println("클릭된 노드: ${node.label} (${node.id})")
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                            )
                            Text(
                                text = "음성 명령을 입력하여\n웹 탐색 경로를 확인하세요",
                                style = MaterialTheme.typography.body1,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "가던길 - 음성 기반 웹 접근성 도구"
    ) {
        TestGraphApp()
    }
}