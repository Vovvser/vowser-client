import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
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
    // 데이터 초기화 - 확장된 네이버 데이터 사용
    val navigationGraph = remember { ExpandedNaverDataGenerator.createExpandedNaverData() }
    val navigationProcessor = remember { NavigationProcessor(navigationGraph) }
    val coroutineScope = rememberCoroutineScope()
    
    // UI 상태
    var currentCommand by remember { mutableStateOf("") }
    var navigationResult by remember { mutableStateOf<NavigationResult?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var selectedCommand by remember { mutableStateOf<String?>(null) }
    
    // 사전 정의된 명령어들 - 확장된 명령어 세트
    val sampleCommands = remember {
        listOf(
            "인기 웹툰 보고 싶어",
            "로맨스 웹툰 읽고 싶어",
            "정치 뉴스 알려줘",
            "야구 뉴스 보고 싶어",
            "화장품 쇼핑하고 싶어",
            "요리 레시피 찾아줘",
            "드라마 보고 싶어",
            "케이팝 듣고 싶어",
            "트로트 음악 듣고 싶어",
            "갤럭시 폰 사고 싶어",
            "오늘 날씨 어때?",
            "심심한데 재밌는 거 없을까?",
            "뭐 할 수 있어?"
        )
    }
    
    MaterialTheme {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 좌측 패널: 명령어 입력 및 결과
            Column(
                modifier = Modifier
                    .weight(0.4f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 헤더
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 4.dp,
                    backgroundColor = MaterialTheme.colors.primary
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "🎙️ 가던길 (Desire Path)",
                            style = MaterialTheme.typography.h5,
                            color = Color.White
                        )
                        Text(
                            text = "음성 명령 기반 웹 접근성 도구",
                            style = MaterialTheme.typography.caption,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
                
                // 명령어 입력
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "음성 명령어",
                            style = MaterialTheme.typography.h6
                        )
                        
                        OutlinedTextField(
                            value = currentCommand,
                            onValueChange = { currentCommand = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("예: 인기 웹툰 보고 싶어") },
                            enabled = !isProcessing
                        )
                        
                        Button(
                            onClick = {
                                if (currentCommand.isNotBlank()) {
                                    coroutineScope.launch {
                                        isProcessing = true
                                        try {
                                            navigationResult = navigationProcessor.processVoiceCommand(currentCommand)
                                        } finally {
                                            isProcessing = false
                                        }
                                    }
                                }
                            },
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
                                Text("🔍 경로 찾기")
                            }
                        }
                    }
                }
                
                // 샘플 명령어
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "💡 샘플 명령어",
                            style = MaterialTheme.typography.h6
                        )
                        
                        sampleCommands.forEach { command ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { 
                                        currentCommand = command
                                        selectedCommand = command
                                        // 자동으로 명령 실행
                                        coroutineScope.launch {
                                            isProcessing = true
                                            try {
                                                navigationResult = navigationProcessor.processVoiceCommand(command)
                                            } finally {
                                                isProcessing = false
                                            }
                                        }
                                    },
                                backgroundColor = if (selectedCommand == command) {
                                    MaterialTheme.colors.primary.copy(alpha = 0.1f)
                                } else Color.White,
                                elevation = if (selectedCommand == command) 4.dp else 1.dp,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colors.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = command,
                                        style = MaterialTheme.typography.body2,
                                        fontWeight = if (selectedCommand == command) FontWeight.Medium else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
                
                // 결과 표시
                navigationResult?.let { result ->
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
                            Text(
                                text = if (result.success) "✅ 성공" else "⚠️ 알림",
                                style = MaterialTheme.typography.h6,
                                color = if (result.success) Color(0xFF2E7D32) else Color(0xFFE65100)
                            )
                            
                            Text(
                                text = result.message,
                                style = MaterialTheme.typography.body2
                            )
                            
                            if (result.success && result.estimatedTime > 0) {
                                Divider()
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("예상 시간: ${result.estimatedTime}초")
                                    Text("클릭 수: ${result.totalClicks}번")
                                }
                            }
                        }
                    }
                }
            }
            
            // 우측 패널: 그래프 시각화
            Column(
                modifier = Modifier.weight(0.6f)
            ) {
                val currentVisualizationData = remember(navigationResult) {
                    navigationResult?.visualizationData 
                        ?: navigationProcessor.getCurrentVisualizationData()
                }
                
                AnimatedNavigationGraph(
                    nodes = currentVisualizationData.nodes,
                    edges = currentVisualizationData.edges,
                    highlightedPath = currentVisualizationData.highlightedPath,
                    activeNodeId = currentVisualizationData.activeNodeId,
                    isNavigationActive = isProcessing || (navigationResult?.success == true),
                    modifier = Modifier.fillMaxSize()
                ) { node ->
                    println("클릭된 노드: ${node.label} (${node.id})")
                    // 노드 클릭 시 상세 정보 표시 등 추가 기능 구현 가능
                }
            }
        }
    }
}

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "네트워크 그래프 테스트 - 가던길 프로토타입"
    ) {
        TestGraphApp()
    }
}