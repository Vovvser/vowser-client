package com.vowser.client.ui.graph

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlin.math.*

private val NodeWidth: Dp = 60.dp
private val NodeHeight: Dp = 30.dp

/**
 * 애니메이션이 포함된 네트워크 그래프 컴포넌트
 */
@Composable
fun AnimatedNavigationGraph(
    nodes: List<GraphNode>,
    edges: List<GraphEdge>,
    modifier: Modifier = Modifier,
    highlightedPath: List<String> = emptyList(),
    activeNodeId: String? = null,
    isNavigationActive: Boolean = false,
    onNodeClick: (GraphNode) -> Unit = {}
) {
    var canvasSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }

    // 애니메이션을 위한 현재 활성 경로 단계
    var currentPathStep by remember { mutableStateOf(0) }

    // 경로 탐색 애니메이션
    LaunchedEffect(isNavigationActive, highlightedPath) {
        if (isNavigationActive && highlightedPath.isNotEmpty()) {
            currentPathStep = 0
            for (i in highlightedPath.indices) {
                currentPathStep = i + 1
                delay(800) // 각 단계별 0.8초 지연
            }
        } else {
            currentPathStep = highlightedPath.size
        }
    }

    // 노드 위치 계산
    val positionedNodes = remember(nodes, canvasSize) {
        if (canvasSize.width > 0 && canvasSize.height > 0) {
            com.vowser.client.ui.graph.layoutNodesHierarchically(nodes, edges, canvasSize)
        } else {
            nodes
        }
    }

    // 현재 단계까지의 활성 경로
    val currentActivePath = remember(currentPathStep, highlightedPath) {
        if (highlightedPath.isEmpty()) emptyList()
        else highlightedPath.take(currentPathStep)
    }

    Box(modifier = modifier) {
        // 배경 및 엣지 Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFF8F9FA),
                            Color(0xFFE3F2FD),
                            Color(0xFFE1F5FE)
                        ),
                        center = Offset.Infinite
                    )
                )
        ) {
            canvasSize = size

            if (positionedNodes.isNotEmpty()) {
                // 배경 패턴 그리기
                drawBackgroundPattern()

                // 계층 구분 영역 그리기
                drawLayerRegions(positionedNodes)

                // 엣지 그리기 (애니메이션 적용)
                drawAnimatedEdges(positionedNodes, edges, currentActivePath, currentPathStep)
            }
        }

        // 노드 오버레이
        positionedNodes.forEach { node ->
            val density = LocalDensity.current
            AnimatedNodeComponent(
                node = node,
                isHighlighted = currentActivePath.contains(node.id),
                isActive = node.id == activeNodeId,
                isCurrentStep = node.id == highlightedPath.getOrNull(currentPathStep - 1),
                onClick = { onNodeClick(node) },
                modifier = Modifier
                    .offset(
                        // 💡 수정된 오프셋 계산 (상수 사용)
                        x = with(density) { node.x.toDp() - (NodeWidth / 2) },
                        y = with(density) { node.y.toDp() - (NodeHeight / 2) }
                    )
                    .zIndex(
                        when {
                            node.id == activeNodeId -> 15f
                            node.id == highlightedPath.getOrNull(currentPathStep - 1) -> 12f
                            currentActivePath.contains(node.id) -> 10f
                            else -> 5f
                        }
                    )
            )
        }

        // 진행률 표시기
        if (isNavigationActive && highlightedPath.isNotEmpty()) {
            NavigationProgressIndicator(
                currentStep = currentPathStep,
                totalSteps = highlightedPath.size,
                pathLabels = highlightedPath.mapNotNull { nodeId ->
                    positionedNodes.find { it.id == nodeId }?.label
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            )
        }
    }
}

/**
 * 애니메이션이 적용된 노드 컴포넌트
 */
@Composable
private fun AnimatedNodeComponent(
    node: GraphNode,
    isHighlighted: Boolean,
    isActive: Boolean,
    isCurrentStep: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    // 애니메이션 상태들
    val scale by animateFloatAsState(
        targetValue = when {
            isCurrentStep -> 1.3f
            isActive -> 1.2f
            isHighlighted -> 1.1f
            isHovered -> 1.05f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )

    val backgroundColor by animateColorAsState(
        targetValue = when {
            isCurrentStep -> Color(0xFFFF3D00) // 현재 단계는 빨간색
            isActive -> Color(0xFFFF6B35)
            isHighlighted -> node.type.color
            isHovered -> node.type.color.copy(alpha = 0.9f)
            else -> node.type.color.copy(alpha = 0.8f)
        },
        animationSpec = tween(400)
    )

    val elevation by animateFloatAsState(
        targetValue = when {
            isCurrentStep -> 16f
            isActive -> 12f
            isHighlighted -> 8f
            isHovered -> 6f
            else -> 2f
        },
        animationSpec = tween(300)
    )

    // 펄스 애니메이션 (현재 단계인 경우)
    val pulseAnimation = rememberInfiniteTransition()
    val pulseAlpha by pulseAnimation.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        )
    )

    Card(
        modifier = modifier
            // 💡 수정된 크기 (상수 사용)
            .width(NodeWidth)
            .height(NodeHeight)
            .scale(scale)
            .hoverable(interactionSource)
            .clickable { onClick() }
            .shadow(elevation.dp, shape = RoundedCornerShape(12.dp)) // 모서리 둥글기 조절
            .then(
                if (isCurrentStep) {
                    Modifier.border(
                        width = 2.dp, // 테두리 두께 조절
                        color = Color(0xFFFF3D00).copy(alpha = pulseAlpha),
                        shape = RoundedCornerShape(12.dp)
                    )
                } else Modifier
            ),
        shape = RoundedCornerShape(12.dp),
        backgroundColor = backgroundColor,
        elevation = elevation.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 2.dp), // 패딩 조절
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 아이콘 (애니메이션 적용)
            val iconScale by animateFloatAsState(
                targetValue = if (isCurrentStep) 1.2f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
            )

            Icon(
                imageVector = getNodeIcon(node.type),
                contentDescription = null,
                modifier = Modifier
                    .size(16.dp) // 아이콘 크기 조절
                    .scale(iconScale),
                tint = Color.White
            )

            // 라벨
            Text(
                text = node.label,
                color = Color.White,
                fontSize = 8.sp, // 폰트 크기 조절
                fontWeight = if (isCurrentStep || isActive) FontWeight.Bold else FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 1.dp) // 아이콘과의 간격 조절
            )
        }
    }

    // 현재 단계 표시 효과
    if (isCurrentStep) {
        // 방사형 파동 효과
        LaunchedEffect(isCurrentStep) {
            // 추가적인 시각 효과는 여기에 구현 가능
        }
    }
}

/**
 * 진행률 표시기
 */
@Composable
private fun NavigationProgressIndicator(
    currentStep: Int,
    totalSteps: Int,
    pathLabels: List<String>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        backgroundColor = Color(0xFF263238).copy(alpha = 0.9f),
        shape = RoundedCornerShape(12.dp),
        elevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "탐색 진행률",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 진행률 바
            LinearProgressIndicator(
                progress = currentStep.toFloat() / totalSteps.toFloat(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = Color(0xFF4CAF50),
                backgroundColor = Color.White.copy(alpha = 0.3f)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "$currentStep / $totalSteps 단계",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp
            )

            if (currentStep > 0 && currentStep <= pathLabels.size) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "현재: ${pathLabels[currentStep - 1]}",
                    color = Color(0xFFFF6B35),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * 개선된 레이어 라벨
 */
@Composable
private fun ImprovedLayerLabels(
    modifier: Modifier = Modifier,
    currentActivePath: List<String>
) {
    Box(modifier = modifier) {
        val layers = listOf(
            LayerInfo("🎙️ 음성 명령", 0.1f, NodeType.START),
            LayerInfo("🌐 웹사이트", 0.35f, NodeType.WEBSITE),
            LayerInfo("📁 카테고리", 0.6f, NodeType.PAGE),
            LayerInfo("📄 콘텐츠", 0.85f, NodeType.ACTION)
        )

        layers.forEach { layer ->
            val isActiveLayer = currentActivePath.any { nodeId ->
                // 실제로는 노드 타입을 확인해야 함
                true // 간단히 처리
            }

            val backgroundColor by animateColorAsState(
                targetValue = if (isActiveLayer) {
                    Color(0xFF4CAF50).copy(alpha = 0.9f)
                } else {
                    Color(0xFF263238).copy(alpha = 0.8f)
                },
                animationSpec = tween(400)
            )

            Card(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(
                        x = 16.dp,
                        y = with(LocalDensity.current) {
                            (layer.yRatio * 400.dp.toPx()).toDp() - 15.dp
                        }
                    ),
                backgroundColor = backgroundColor,
                shape = RoundedCornerShape(25.dp),
                elevation = if (isActiveLayer) 6.dp else 4.dp
            ) {
                Text(
                    text = layer.name,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = if (isActiveLayer) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

private data class LayerInfo(
    val name: String,
    val yRatio: Float,
    val nodeType: NodeType
)

/**
 * 배경 패턴 그리기
 */
private fun DrawScope.drawBackgroundPattern() {
    val patternSpacing = 60.dp.toPx()
    val dotRadius = 2.dp.toPx()
    val dotColor = Color(0xFFBDBDBD).copy(alpha = 0.3f)

    var x = patternSpacing
    while (x < size.width) {
        var y = patternSpacing
        while (y < size.height) {
            drawCircle(
                color = dotColor,
                radius = dotRadius,
                center = Offset(x, y)
            )
            y += patternSpacing
        }
        x += patternSpacing
    }
}

/**
 * 계층 구분 영역 그리기
 */
private fun DrawScope.drawLayerRegions(nodes: List<GraphNode>) {
    val layerYPositions = nodes.groupBy { it.type }.mapNotNull { (nodeType, layerNodes) ->
        nodeType to (layerNodes.firstOrNull()?.y ?: 0f)
    }.sortedBy { it.second }

    layerYPositions.forEachIndexed { index, (nodeType, y) ->
        val regionColor = when (nodeType) {
            NodeType.START -> Color(0xFF4CAF50).copy(alpha = 0.05f)
            NodeType.WEBSITE -> Color(0xFF2196F3).copy(alpha = 0.05f)
            NodeType.PAGE -> Color(0xFFFF9800).copy(alpha = 0.05f)
            NodeType.ACTION -> Color(0xFF9C27B0).copy(alpha = 0.05f)
            NodeType.DEFAULT -> Color(0xFF607D8B).copy(alpha = 0.05f)
        }

        val regionHeight = if (index < layerYPositions.size - 1) {
            layerYPositions[index + 1].second - y
        } else {
            size.height - y
        }

        drawRect(
            color = regionColor,
            topLeft = Offset(0f, y - 50f),
            size = androidx.compose.ui.geometry.Size(size.width, regionHeight + 100f)
        )
    }
}

/**
 * 애니메이션이 적용된 엣지 그리기
 */
private fun DrawScope.drawAnimatedEdges(
    nodes: List<GraphNode>,
    edges: List<GraphEdge>,
    activePath: List<String>,
    currentStep: Int
) {
    val nodeMap = nodes.associateBy { it.id }

    edges.forEach { edge ->
        val fromNode = nodeMap[edge.from]
        val toNode = nodeMap[edge.to]

        if (fromNode != null && toNode != null) {
            val isHighlighted = activePath.contains(fromNode.id) && activePath.contains(toNode.id)
            val isCurrentConnection = (activePath.indexOf(fromNode.id) == currentStep - 2 &&
                                     activePath.indexOf(toNode.id) == currentStep - 1)

            // 곡선 경로 계산
            val startPoint = Offset(fromNode.x, fromNode.y + 30f)
            val endPoint = Offset(toNode.x, toNode.y - 30f)

            val controlPoint1 = Offset(startPoint.x, startPoint.y + (endPoint.y - startPoint.y) * 0.3f)
            val controlPoint2 = Offset(endPoint.x, startPoint.y + (endPoint.y - startPoint.y) * 0.7f)

            val path = Path().apply {
                moveTo(startPoint.x, startPoint.y)
                cubicTo(
                    controlPoint1.x, controlPoint1.y,
                    controlPoint2.x, controlPoint2.y,
                    endPoint.x, endPoint.y
                )
            }

            // 현재 연결선에 특별 효과
            if (isCurrentConnection) {
                // 그림자 효과
                drawPath(
                    path = path,
                    color = Color(0xFFFF3D00).copy(alpha = 0.4f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 12.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )
            } else if (isHighlighted) {
                // 일반 하이라이트 그림자
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
                color = when {
                    isCurrentConnection -> Color(0xFFFF3D00)
                    isHighlighted -> Color(0xFF4CAF50)
                    else -> Color(0xFF9E9E9E).copy(alpha = 0.4f)
                },
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = when {
                        isCurrentConnection -> 6.dp.toPx()
                        isHighlighted -> 4.dp.toPx()
                        else -> 2.dp.toPx()
                    },
                    cap = StrokeCap.Round
                )
            )

            // 화살표
            drawAnimatedArrowHead(endPoint, startPoint, isHighlighted, isCurrentConnection)
        }
    }
}

/**
 * 애니메이션이 적용된 화살표 머리 그리기
 */
private fun DrawScope.drawAnimatedArrowHead(
    endPoint: Offset,
    startPoint: Offset,
    isHighlighted: Boolean,
    isCurrentConnection: Boolean
) {
    val arrowLength = if (isCurrentConnection) 16f else if (isHighlighted) 14f else 10f
    val arrowAngle = PI / 6

    val lineAngle = atan2(endPoint.y - startPoint.y, endPoint.x - startPoint.x)

    val arrowEnd1 = Offset(
        endPoint.x - arrowLength * cos(lineAngle - arrowAngle).toFloat(),
        endPoint.y - arrowLength * sin(lineAngle - arrowAngle).toFloat()
    )

    val arrowEnd2 = Offset(
        endPoint.x - arrowLength * cos(lineAngle + arrowAngle).toFloat(),
        endPoint.y - arrowLength * sin(lineAngle + arrowAngle).toFloat()
    )

    val arrowColor = when {
        isCurrentConnection -> Color(0xFFFF3D00)
        isHighlighted -> Color(0xFF4CAF50)
        else -> Color(0xFF9E9E9E).copy(alpha = 0.4f)
    }

    val strokeWidth = when {
        isCurrentConnection -> 4.dp.toPx()
        isHighlighted -> 3.dp.toPx()
        else -> 2.dp.toPx()
    }

    drawLine(color = arrowColor, start = endPoint, end = arrowEnd1, strokeWidth = strokeWidth, cap = StrokeCap.Round)
    drawLine(color = arrowColor, start = endPoint, end = arrowEnd2, strokeWidth = strokeWidth, cap = StrokeCap.Round)
}

/**
 * 노드 타입에 따른 아이콘 반환
 */
private fun getNodeIcon(nodeType: NodeType) = when (nodeType) {
    NodeType.START -> Icons.Default.PlayArrow
    NodeType.WEBSITE -> Icons.Default.Home
    NodeType.PAGE -> Icons.Default.Info
    NodeType.ACTION -> Icons.Default.CheckCircle
    NodeType.DEFAULT -> Icons.Default.CheckCircle
}