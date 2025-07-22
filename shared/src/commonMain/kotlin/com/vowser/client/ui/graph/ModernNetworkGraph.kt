package com.vowser.client.ui.graph

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

/**
 * 2025년 빅테크 디자인 트렌드를 반영한 모던 그래프 시각화 컴포넌트
 * - 글래스모피즘(Glassmorphism) 효과
 * - 부드러운 애니메이션
 * - 반응형 인터랙션
 * - 접근성 친화적 디자인
 */

@Composable
fun ModernNetworkGraph(
    nodes: List<GraphNode>,
    edges: List<GraphEdge>,
    modifier: Modifier = Modifier,
    highlightedPath: List<String> = emptyList(),
    activeNodeId: String? = null,
    isContributionMode: Boolean = false,
    isLoading: Boolean = false,
    onNodeClick: (GraphNode) -> Unit = {},
    onNodeLongClick: (GraphNode) -> Unit = {},
    onGraphInteraction: (GraphInteractionType) -> Unit = {}
) {
    var canvasSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var selectedNode by remember { mutableStateOf<GraphNode?>(null) }
    
    // 애니메이션 상태
    val animatedScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        )
    )
    
    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.5f, 3f)
        val maxX = (canvasSize.width * (scale - 1)) / 2f
        val maxY = (canvasSize.height * (scale - 1)) / 2f
        offset = Offset(
            x = (offset.x + panChange.x).coerceIn(-maxX, maxX),
            y = (offset.y + panChange.y).coerceIn(-maxY, maxY)
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF0D1117), // GitHub dark background
                        Color(0xFF161B22),
                        Color(0xFF21262D)
                    )
                )
            )
    ) {
        // 로딩 상태 처리
        if (isLoading) {
            LoadingGraphAnimation(
                modifier = Modifier.fillMaxSize(),
                isContributionMode = isContributionMode
            )
        } else {
            // 메인 그래프 영역
            GraphCanvas(
                nodes = nodes,
                edges = edges,
                canvasSize = canvasSize,
                scale = animatedScale,
                offset = offset,
                highlightedPath = highlightedPath,
                activeNodeId = activeNodeId,
                isContributionMode = isContributionMode,
                selectedNode = selectedNode,
                onCanvasSizeChanged = { canvasSize = it },
                onNodeClick = { node ->
                    selectedNode = node
                    onNodeClick(node)
                },
                onNodeLongClick = onNodeLongClick,
                modifier = Modifier
                    .fillMaxSize()
                    .transformable(state = transformableState)
            )
            
            // 글래스모피즘 헤더
            GlassmorphismHeader(
                title = if (isContributionMode) "기여 모드 - 경로 기록중" else "웹 탐색 경로",
                nodeCount = nodes.size,
                isContributionMode = isContributionMode,
                onModeToggle = { onGraphInteraction(GraphInteractionType.ToggleMode) },
                modifier = Modifier.align(Alignment.TopCenter)
            )
            
            // 플로팅 컨트롤 패널
            FloatingControlPanel(
                scale = scale,
                onZoomIn = { scale = (scale * 1.2f).coerceAtMost(3f) },
                onZoomOut = { scale = (scale / 1.2f).coerceAtLeast(0.5f) },
                onReset = { 
                    scale = 1f
                    offset = Offset.Zero
                },
                onCenterView = { onGraphInteraction(GraphInteractionType.CenterView) },
                modifier = Modifier.align(Alignment.BottomEnd)
            )
            
            // 선택된 노드 정보 패널
            selectedNode?.let { node ->
                NodeInfoPanel(
                    node = node,
                    onClose = { selectedNode = null },
                    modifier = Modifier.align(Alignment.BottomStart)
                )
            }
            
            // 모던 범례
            ModernLegend(
                isContributionMode = isContributionMode,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            )
        }
    }
}

/**
 * 현대적인 노드 컴포넌트
 */
@Composable
private fun ModernNodeComponent(
    node: GraphNode,
    isHighlighted: Boolean,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 애니메이션
    val scale by animateFloatAsState(
        targetValue = when {
            isActive -> 1.2f
            isHighlighted -> 1.1f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )
    
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isActive -> Color(0xFFFF6B35)
            isHighlighted -> node.type.color
            else -> node.type.color.copy(alpha = 0.9f)
        },
        animationSpec = tween(300)
    )
    
    val borderColor by animateColorAsState(
        targetValue = when {
            isActive -> Color(0xFFFF6B35)
            isHighlighted -> Color(0xFF4CAF50)
            else -> Color.Transparent
        },
        animationSpec = tween(300)
    )
    
    Card(
        modifier = modifier
            .width(120.dp)
            .height(60.dp)
            .scale(scale)
            .clickable { onClick() }
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(12.dp)
            )
            .shadow(
                elevation = if (isActive || isHighlighted) 12.dp else 4.dp,
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        elevation = if (isActive || isHighlighted) 8.dp else 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 아이콘
            Icon(
                imageVector = getNodeIcon(node.type),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Color.White
            )
            
            // 라벨
            Text(
                text = node.label,
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

/**
 * 노드 타입별 아이콘 반환
 */
fun getNodeIcon(nodeType: NodeType) = when (nodeType) {
    NodeType.START -> Icons.Filled.PlayArrow
    NodeType.WEBSITE -> Icons.Filled.Home // Language 대신
    NodeType.PAGE -> Icons.Filled.Person // Folder 대신
    NodeType.ACTION -> Icons.Filled.Star // TouchApp 대신
    NodeType.DEFAULT -> Icons.Filled.Settings // Circle 대신
}

/**
 * 레이어 라벨 컴포넌트
 */
@Composable
private fun LayerLabels(modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        val layers = listOf(
            "음성 명령" to 0.1f,
            "웹사이트" to 0.35f,
            "카테고리" to 0.6f,
            "콘텐츠" to 0.85f
        )
        
        layers.forEach { (label, yRatio) ->
            Card(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = 16.dp, y = with(LocalDensity.current) { 
                        (yRatio * 400.dp.toPx()).toDp() - 15.dp
                    })
                    .background(
                        color = Color(0xFF263238).copy(alpha = 0.8f),
                        shape = RoundedCornerShape(20.dp)
                    ),
                shape = RoundedCornerShape(20.dp),
                elevation = 4.dp
            ) {
                Text(
                    text = label,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * 배경 그리드 그리기
 */
private fun DrawScope.drawBackgroundGrid() {
    val gridSpacing = 40.dp.toPx()
    val gridColor = Color(0xFFDEE2E6).copy(alpha = 0.5f)
    
    // 세로선
    var x = 0f
    while (x <= size.width) {
        drawLine(
            color = gridColor,
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 1.dp.toPx()
        )
        x += gridSpacing
    }
    
    // 가로선
    var y = 0f
    while (y <= size.height) {
        drawLine(
            color = gridColor,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1.dp.toPx()
        )
        y += gridSpacing
    }
}

/**
 * 계층 구분선 그리기
 */
private fun DrawScope.drawLayerSeparators(nodes: List<GraphNode>) {
    val layerYPositions = nodes.groupBy { it.type }.values.mapNotNull { layerNodes ->
        layerNodes.firstOrNull()?.y
    }.sorted()
    
    layerYPositions.forEach { y ->
        if (y > 100f && y < size.height - 100f) { // 첫번째와 마지막 레이어는 제외
            drawLine(
                color = Color(0xFF6C757D).copy(alpha = 0.3f),
                start = Offset(50f, y + 50f),
                end = Offset(size.width - 50f, y + 50f),
                strokeWidth = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
            )
        }
    }
}

/**
 * 현대적인 엣지 그리기
 */
private fun DrawScope.drawModernEdges(
    nodes: List<GraphNode>,
    edges: List<GraphEdge>,
    highlightedPath: List<String>
) {
    val nodeMap = nodes.associateBy { it.id }
    
    edges.forEach { edge ->
        val fromNode = nodeMap[edge.from]
        val toNode = nodeMap[edge.to]
        
        if (fromNode != null && toNode != null) {
            val isHighlighted = highlightedPath.contains(fromNode.id) && highlightedPath.contains(toNode.id)
            
            // 곡선 경로 계산
            val startPoint = Offset(fromNode.x, fromNode.y + 30f) // 노드 하단에서 시작
            val endPoint = Offset(toNode.x, toNode.y - 30f)       // 노드 상단에서 끝
            
            val controlPoint1 = Offset(startPoint.x, startPoint.y + (endPoint.y - startPoint.y) * 0.5f)
            val controlPoint2 = Offset(endPoint.x, startPoint.y + (endPoint.y - startPoint.y) * 0.5f)
            
            // 곡선 경로 그리기
            val path = Path().apply {
                moveTo(startPoint.x, startPoint.y)
                cubicTo(
                    controlPoint1.x, controlPoint1.y,
                    controlPoint2.x, controlPoint2.y,
                    endPoint.x, endPoint.y
                )
            }
            
            // 그림자 효과
            if (isHighlighted) {
                drawPath(
                    path = path,
                    color = Color(0xFF4CAF50).copy(alpha = 0.3f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 8.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )
            }
            
            // 메인 선
            drawPath(
                path = path,
                color = if (isHighlighted) Color(0xFF4CAF50) else Color(0xFF6C757D).copy(alpha = 0.6f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = if (isHighlighted) 4.dp.toPx() else 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )
            
            // 화살표 그리기
            drawArrowHead(endPoint, startPoint, isHighlighted)
        }
    }
}

/**
 * 화살표 머리 그리기
 */
private fun DrawScope.drawArrowHead(
    endPoint: Offset,
    startPoint: Offset,
    isHighlighted: Boolean
) {
    val arrowLength = 12f
    val arrowAngle = PI / 6 // 30도
    
    val lineAngle = atan2(endPoint.y - startPoint.y, endPoint.x - startPoint.x)
    
    val arrowEnd1 = Offset(
        endPoint.x - arrowLength * cos(lineAngle - arrowAngle).toFloat(),
        endPoint.y - arrowLength * sin(lineAngle - arrowAngle).toFloat()
    )
    
    val arrowEnd2 = Offset(
        endPoint.x - arrowLength * cos(lineAngle + arrowAngle).toFloat(),
        endPoint.y - arrowLength * sin(lineAngle + arrowAngle).toFloat()
    )
    
    val arrowColor = if (isHighlighted) Color(0xFF4CAF50) else Color(0xFF6C757D).copy(alpha = 0.6f)
    val strokeWidth = if (isHighlighted) 3.dp.toPx() else 2.dp.toPx()
    
    drawLine(
        color = arrowColor,
        start = endPoint,
        end = arrowEnd1,
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )
    
    drawLine(
        color = arrowColor,
        start = endPoint,
        end = arrowEnd2,
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )
}

// 새로운 모던 컴포넌트들

@Composable
private fun GraphCanvas(
    nodes: List<GraphNode>,
    edges: List<GraphEdge>,
    canvasSize: androidx.compose.ui.geometry.Size,
    scale: Float,
    offset: Offset,
    highlightedPath: List<String>,
    activeNodeId: String?,
    isContributionMode: Boolean,
    selectedNode: GraphNode?,
    onCanvasSizeChanged: (androidx.compose.ui.geometry.Size) -> Unit,
    onNodeClick: (GraphNode) -> Unit,
    onNodeLongClick: (GraphNode) -> Unit,
    modifier: Modifier = Modifier
) {
    val positionedNodes = remember(nodes, edges, canvasSize) {
        if (canvasSize.width > 0 && canvasSize.height > 0) {
            layoutNodesWithPhysics(nodes, edges, canvasSize)
        } else nodes
    }
    
    // 펄스 애니메이션
    val pulseAnimation = rememberInfiniteTransition()
    val pulseScale by pulseAnimation.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures { _, _ ->
                    // 드래그 제스처 처리
                }
            }
    ) {
        onCanvasSizeChanged(size)
        
        if (positionedNodes.isNotEmpty()) {
            // 백그라운드 그리드
            drawUltraModernGrid()
            
            // 엣지 그리기 (글로우 효과 포함)
            drawUltraModernEdges(
                nodes = positionedNodes,
                edges = edges,
                highlightedPath = highlightedPath,
                scale = scale,
                offset = offset,
                isContributionMode = isContributionMode
            )
            
            // 노드 그리기 (글래스모피즘 효과)
            drawUltraModernNodes(
                nodes = positionedNodes,
                highlightedPath = highlightedPath,
                activeNodeId = activeNodeId,
                selectedNodeId = selectedNode?.id,
                scale = scale,
                offset = offset,
                pulseScale = pulseScale,
                isContributionMode = isContributionMode
            )
        }
    }
}

@Composable
private fun GlassmorphismHeader(
    title: String,
    nodeCount: Int,
    isContributionMode: Boolean,
    onModeToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.2f),
                            Color.White.copy(alpha = 0.1f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.3f),
                            Color.White.copy(alpha = 0.1f)
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$nodeCount 개 노드",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                }
                
                Row {
                    // 모드 토글 스위치
                    Switch(
                        checked = isContributionMode,
                        onCheckedChange = { onModeToggle() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF00D4AA),
                            checkedTrackColor = Color(0xFF00D4AA).copy(alpha = 0.5f),
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color.White.copy(alpha = 0.3f)
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isContributionMode) "기여" else "탐색",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun FloatingControlPanel(
    scale: Float,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onReset: () -> Unit,
    onCenterView: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(16.dp)
            .background(
                color = Color.Black.copy(alpha = 0.7f),
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        elevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 줌 인
            FloatingActionButton(
                onClick = onZoomIn,
                modifier = Modifier.size(40.dp),
                backgroundColor = Color(0xFF238636),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Zoom In", modifier = Modifier.size(20.dp))
            }
            
            // 줌 아웃
            FloatingActionButton(
                onClick = onZoomOut,
                modifier = Modifier.size(40.dp),
                backgroundColor = Color(0xFF238636),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Zoom Out", modifier = Modifier.size(20.dp))
            }
            
            // 리셋
            FloatingActionButton(
                onClick = onReset,
                modifier = Modifier.size(40.dp),
                backgroundColor = Color(0xFF8250DF),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Reset", modifier = Modifier.size(20.dp))
            }
            
            // 중앙 정렬
            FloatingActionButton(
                onClick = onCenterView,
                modifier = Modifier.size(40.dp),
                backgroundColor = Color(0xFF0969DA),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = "Center", modifier = Modifier.size(20.dp))
            }
            
            // 확대/축소 레벨 표시
            Text(
                text = "${(scale * 100).toInt()}%",
                color = Color.White,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun NodeInfoPanel(
    node: GraphNode,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(16.dp)
            .background(
                color = Color.Black.copy(alpha = 0.8f),
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        elevation = 12.dp
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1C2128),
                            Color(0xFF0D1117)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "노드 정보",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 노드 타입 배지
                Box(
                    modifier = Modifier
                        .background(
                            color = node.type.color.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = node.type.color,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = when (node.type) {
                            NodeType.START -> "시작점"
                            NodeType.WEBSITE -> "웹사이트"
                            NodeType.PAGE -> "페이지"
                            NodeType.ACTION -> "액션"
                            NodeType.DEFAULT -> "기본"
                        },
                        color = node.type.color,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = node.label,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                
                Text(
                    text = "ID: ${node.id}",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 추가 정보
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    InfoChip("위치", "(${node.x.toInt()}, ${node.y.toInt()})")
                    InfoChip("크기", "${node.type.size.toInt()}px")
                }
            }
        }
    }
}

@Composable
private fun InfoChip(label: String, value: String) {
    Column {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 10.sp
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ModernLegend(
    isContributionMode: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .background(
                color = Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "범례",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            
            NodeType.values().forEach { nodeType ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(nodeType.color)
                    )
                    Text(
                        text = when (nodeType) {
                            NodeType.START -> "시작점"
                            NodeType.WEBSITE -> "웹사이트"
                            NodeType.PAGE -> "페이지"
                            NodeType.ACTION -> "액션"
                            NodeType.DEFAULT -> "기본"
                        },
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 11.sp
                    )
                }
            }
            
            if (isContributionMode) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "🔴 기여 모드 활성",
                    color = Color(0xFFFF4444),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun LoadingGraphAnimation(
    modifier: Modifier = Modifier,
    isContributionMode: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition()
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 로딩 스피너
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .scale(scale)
                    .graphicsLayer { rotationZ = rotation }
            ) {
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val strokeWidth = 6.dp.toPx()
                    drawArc(
                        color = if (isContributionMode) Color(0xFF00D4AA) else Color(0xFF0969DA),
                        startAngle = 0f,
                        sweepAngle = 270f,
                        useCenter = false,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(strokeWidth, cap = StrokeCap.Round),
                        size = androidx.compose.ui.geometry.Size(size.width, size.height)
                    )
                }
            }
            
            Text(
                text = if (isContributionMode) "경로를 기록하고 있습니다..." else "그래프를 로드하고 있습니다...",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = "잠시만 기다려 주세요",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
        }
    }
}

// 물리학 기반 노드 레이아웃
fun layoutNodesWithPhysics(
    nodes: List<GraphNode>,
    edges: List<GraphEdge>,
    canvasSize: androidx.compose.ui.geometry.Size
): List<GraphNode> {
    if (nodes.isEmpty()) return nodes
    
    // 개선된 계층형 레이아웃 (물리학 시뮬레이션 적용)
    val nodesByType = nodes.groupBy { it.type }
    val layers = listOf(
        NodeType.START,
        NodeType.WEBSITE,
        NodeType.PAGE,
        NodeType.ACTION
    )
    
    val layerHeight = canvasSize.height / (layers.size + 1.5f)
    val result = mutableListOf<GraphNode>()
    
    val parentChildMap = mutableMapOf<String, MutableList<String>>()
    edges.forEach { edge ->
        parentChildMap.getOrPut(edge.from) { mutableListOf() }.add(edge.to)
    }
    
    layers.forEachIndexed { layerIndex, nodeType ->
        val nodesInLayer = nodesByType[nodeType] ?: emptyList()
        if (nodesInLayer.isNotEmpty()) {
            val y = layerHeight * (layerIndex + 1)
            
            when (layerIndex) {
                0 -> {
                    // ROOT 노드 - 중앙 상단
                    val x = canvasSize.width / 2f
                    result.addAll(nodesInLayer.map { it.copy(x = x, y = y) })
                }
                else -> {
                    // 자식 노드들을 부모 중심으로 배치 (물리적 거리 고려)
                    val parentNodes = result.filter { parentNode ->
                        parentChildMap[parentNode.id]?.any { childId ->
                            nodesInLayer.any { it.id == childId }
                        } == true
                    }
                    
                    if (parentNodes.isNotEmpty()) {
                        parentNodes.forEach { parentNode ->
                            val childrenOfParent = nodesInLayer.filter { child ->
                                parentChildMap[parentNode.id]?.contains(child.id) == true
                            }
                            
                            childrenOfParent.forEachIndexed { index, child ->
                                val angle = (2 * PI * index / childrenOfParent.size) - PI / 2
                                val radius = 120f + (layerIndex * 20f)
                                
                                val x = (parentNode.x + cos(angle).toFloat() * radius)
                                    .coerceIn(60f, canvasSize.width - 60f)
                                
                                result.add(child.copy(x = x.toFloat(), y = y))
                            }
                        }
                    } else {
                        // 부모가 없는 노드들 균등 분산
                        val spacing = (canvasSize.width - 120f) / maxOf(1, nodesInLayer.size - 1)
                        nodesInLayer.forEachIndexed { index, node ->
                            val x = 60f + index * spacing
                            result.add(node.copy(x = x, y = y))
                        }
                    }
                }
            }
        }
    }
    
    return result
}

// 울트라 모던 그리드 그리기
private fun DrawScope.drawUltraModernGrid() {
    val gridSize = 40f
    val gridColor = Color.White.copy(alpha = 0.05f)
    
    // 세로선
    var x = 0f
    while (x <= size.width) {
        drawLine(
            color = gridColor,
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 1f
        )
        x += gridSize
    }
    
    // 가로선
    var y = 0f
    while (y <= size.height) {
        drawLine(
            color = gridColor,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1f
        )
        y += gridSize
    }
}

// 울트라 모던한 엣지 그리기 (글로우 효과)
private fun DrawScope.drawUltraModernEdges(
    nodes: List<GraphNode>,
    edges: List<GraphEdge>,
    highlightedPath: List<String>,
    scale: Float,
    offset: Offset,
    isContributionMode: Boolean
) {
    val nodeMap = nodes.associateBy { it.id }
    
    edges.forEach { edge ->
        val fromNode = nodeMap[edge.from]
        val toNode = nodeMap[edge.to]
        
        if (fromNode != null && toNode != null) {
            val isHighlighted = highlightedPath.contains(fromNode.id) && highlightedPath.contains(toNode.id)
            val isRecording = isContributionMode
            
            val startPos = Offset(
                fromNode.x * scale + offset.x,
                fromNode.y * scale + offset.y
            )
            val endPos = Offset(
                toNode.x * scale + offset.x,
                toNode.y * scale + offset.y
            )
            
            // 글로우 효과를 위한 여러 레이어
            when {
                isHighlighted -> {
                    // 외부 글로우
                    drawLine(
                        color = Color(0xFF00D4AA).copy(alpha = 0.3f),
                        start = startPos,
                        end = endPos,
                        strokeWidth = 12f,
                        cap = StrokeCap.Round
                    )
                    // 내부 글로우
                    drawLine(
                        color = Color(0xFF00D4AA).copy(alpha = 0.7f),
                        start = startPos,
                        end = endPos,
                        strokeWidth = 6f,
                        cap = StrokeCap.Round
                    )
                    // 코어 라인
                    drawLine(
                        color = Color(0xFF00D4AA),
                        start = startPos,
                        end = endPos,
                        strokeWidth = 3f,
                        cap = StrokeCap.Round
                    )
                }
                isRecording -> {
                    // 기여 모드 - 빨간색 펄스
                    drawLine(
                        color = Color(0xFFFF4444).copy(alpha = 0.4f),
                        start = startPos,
                        end = endPos,
                        strokeWidth = 8f,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = Color(0xFFFF4444),
                        start = startPos,
                        end = endPos,
                        strokeWidth = 2f,
                        cap = StrokeCap.Round
                    )
                }
                else -> {
                    // 일반 상태
                    drawLine(
                        color = Color.White.copy(alpha = 0.3f),
                        start = startPos,
                        end = endPos,
                        strokeWidth = 2f,
                        cap = StrokeCap.Round
                    )
                }
            }
            
            // 방향 화살표
            drawUltraArrowHead(startPos, endPos, isHighlighted, isRecording)
        }
    }
}

// 울트라 모던한 노드 그리기 (글래스모피즘)
private fun DrawScope.drawUltraModernNodes(
    nodes: List<GraphNode>,
    highlightedPath: List<String>,
    activeNodeId: String?,
    selectedNodeId: String?,
    scale: Float,
    offset: Offset,
    pulseScale: Float,
    isContributionMode: Boolean
) {
    nodes.forEach { node ->
        val isHighlighted = highlightedPath.contains(node.id)
        val isActive = node.id == activeNodeId
        val isSelected = node.id == selectedNodeId
        
        val position = Offset(
            node.x * scale + offset.x,
            node.y * scale + offset.y
        )
        
        val baseRadius = node.type.size / 2f * scale
        val radius = when {
            isActive -> baseRadius * pulseScale * 1.3f
            isSelected -> baseRadius * 1.2f
            isHighlighted -> baseRadius * 1.1f
            else -> baseRadius
        }
        
        // 그림자 효과
        drawCircle(
            color = Color.Black.copy(alpha = 0.3f),
            radius = radius + 4f,
            center = position + Offset(2f, 2f)
        )
        
        // 외부 링 (글로우 효과)
        when {
            isActive -> {
                drawCircle(
                    color = Color(0xFFFF6B6B).copy(alpha = 0.4f),
                    radius = radius + 8f,
                    center = position
                )
            }
            isSelected -> {
                drawCircle(
                    color = Color(0xFF4ECDC4).copy(alpha = 0.5f),
                    radius = radius + 6f,
                    center = position
                )
            }
            isHighlighted -> {
                drawCircle(
                    color = Color(0xFF00D4AA).copy(alpha = 0.4f),
                    radius = radius + 4f,
                    center = position
                )
            }
        }
        
        // 메인 노드 (글래스모피즘 효과)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    node.type.color.copy(alpha = 0.8f),
                    node.type.color.copy(alpha = 0.4f)
                ),
                center = position,
                radius = radius
            ),
            radius = radius,
            center = position
        )
        
        // 내부 하이라이트 (글래스 효과)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.3f),
                    Color.Transparent
                ),
                center = position - Offset(radius * 0.3f, radius * 0.3f),
                radius = radius * 0.5f
            ),
            radius = radius,
            center = position
        )
        
        // 테두리
        drawCircle(
            color = when {
                isActive -> Color(0xFFFF6B6B)
                isSelected -> Color(0xFF4ECDC4)
                isHighlighted -> Color(0xFF00D4AA)
                isContributionMode -> Color(0xFFFF4444).copy(alpha = 0.7f)
                else -> Color.White.copy(alpha = 0.5f)
            },
            radius = radius,
            center = position,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = if (isActive || isSelected) 3f else 2f
            )
        )
        
        // 노드 타입 아이콘 (중앙)
        val iconSize = radius * 0.6f
        drawUltraNodeIcon(node.type, position, iconSize)
    }
}

// 울트라 노드 아이콘 그리기
private fun DrawScope.drawUltraNodeIcon(nodeType: NodeType, center: Offset, size: Float) {
    val iconColor = Color.White.copy(alpha = 0.9f)
    
    when (nodeType) {
        NodeType.START -> {
            // 재생 버튼 아이콘
            val triangleSize = size * 0.6f
            drawPath(
                path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(center.x - triangleSize * 0.3f, center.y - triangleSize * 0.5f)
                    lineTo(center.x + triangleSize * 0.5f, center.y)
                    lineTo(center.x - triangleSize * 0.3f, center.y + triangleSize * 0.5f)
                    close()
                },
                color = iconColor
            )
        }
        NodeType.WEBSITE -> {
            // 글로브 아이콘
            drawCircle(
                color = iconColor,
                radius = size * 0.4f,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
            )
            drawLine(
                color = iconColor,
                start = Offset(center.x, center.y - size * 0.4f),
                end = Offset(center.x, center.y + size * 0.4f),
                strokeWidth = 2f
            )
            drawLine(
                color = iconColor,
                start = Offset(center.x - size * 0.4f, center.y),
                end = Offset(center.x + size * 0.4f, center.y),
                strokeWidth = 2f
            )
        }
        NodeType.PAGE -> {
            // 문서 아이콘
            val rectSize = size * 0.6f
            drawRoundRect(
                color = iconColor,
                topLeft = Offset(center.x - rectSize * 0.4f, center.y - rectSize * 0.5f),
                size = androidx.compose.ui.geometry.Size(rectSize * 0.8f, rectSize),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
            )
        }
        NodeType.ACTION -> {
            // 커서 아이콘
            drawPath(
                path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(center.x - size * 0.3f, center.y - size * 0.4f)
                    lineTo(center.x + size * 0.2f, center.y)
                    lineTo(center.x - size * 0.1f, center.y + size * 0.1f)
                    lineTo(center.x - size * 0.2f, center.y + size * 0.4f)
                    close()
                },
                color = iconColor
            )
        }
        NodeType.DEFAULT -> {
            // 기본 점
            drawCircle(
                color = iconColor,
                radius = size * 0.2f,
                center = center
            )
        }
    }
}

// 울트라 화살표 머리 그리기
private fun DrawScope.drawUltraArrowHead(
    start: Offset,
    end: Offset,
    isHighlighted: Boolean,
    isRecording: Boolean
) {
    val arrowLength = if (isHighlighted || isRecording) 12f else 8f
    val arrowAngle = PI / 6
    
    val lineAngle = atan2(end.y - start.y, end.x - start.x)
    val adjustedEnd = end - Offset(
        cos(lineAngle).toFloat() * 20f,
        sin(lineAngle).toFloat() * 20f
    )
    
    val arrowColor = when {
        isHighlighted -> Color(0xFF00D4AA)
        isRecording -> Color(0xFFFF4444)
        else -> Color.White.copy(alpha = 0.6f)
    }
    
    val arrowEnd1 = Offset(
        adjustedEnd.x - arrowLength * cos(lineAngle - arrowAngle).toFloat(),
        adjustedEnd.y - arrowLength * sin(lineAngle - arrowAngle).toFloat()
    )
    
    val arrowEnd2 = Offset(
        adjustedEnd.x - arrowLength * cos(lineAngle + arrowAngle).toFloat(),
        adjustedEnd.y - arrowLength * sin(lineAngle + arrowAngle).toFloat()
    )
    
    drawLine(
        color = arrowColor,
        start = adjustedEnd,
        end = arrowEnd1,
        strokeWidth = if (isHighlighted || isRecording) 3f else 2f,
        cap = StrokeCap.Round
    )
    
    drawLine(
        color = arrowColor,
        start = adjustedEnd,
        end = arrowEnd2,
        strokeWidth = if (isHighlighted || isRecording) 3f else 2f,
        cap = StrokeCap.Round
    )
}

// 그래프 상호작용 타입
enum class GraphInteractionType {
    ToggleMode,
    CenterView,
    ZoomIn,
    ZoomOut,
    Reset
}

