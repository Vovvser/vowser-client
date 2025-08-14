package com.vowser.client.ui.graph

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import kotlin.math.*

/**
 * 그래프 캔버스 컴포넌트
 */
@Composable
fun GraphCanvas(
    nodes: List<GraphNode>,
    edges: List<GraphEdge>,
    canvasSize: Size,
    scale: Float,
    offset: Offset,
    highlightedPath: List<String>,
    activeNodeId: String?,
    isContributionMode: Boolean,
    selectedNode: GraphNode?,
    onCanvasSizeChanged: (Size) -> Unit,
    modifier: Modifier = Modifier
) {
    val positionedNodes = remember(nodes, edges, canvasSize) {
        if (canvasSize.width > 0 && canvasSize.height > 0) {
            layoutNodesWithPhysics(nodes, edges, canvasSize)
        } else nodes
    }

    // 텍스트 측정을 위한 TextMeasurer
    val textMeasurer = rememberTextMeasurer()

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

            // 엣지 그리기
            drawUltraModernEdges(
                nodes = positionedNodes,
                edges = edges,
                highlightedPath = highlightedPath,
                scale = scale,
                offset = offset,
                isContributionMode = isContributionMode,
                textMeasurer = textMeasurer
            )

            // 노드 그리기
            drawUltraModernNodes(
                nodes = positionedNodes,
                highlightedPath = highlightedPath,
                activeNodeId = activeNodeId,
                selectedNodeId = selectedNode?.id,
                scale = scale,
                offset = offset,
                pulseScale = pulseScale,
                isContributionMode = isContributionMode,
                textMeasurer = textMeasurer
            )
        }
    }
}

//그리드 그리기
fun DrawScope.drawUltraModernGrid() {
    val gridSize = 40f
    val gridColor = Color.Black.copy(alpha = 0.05f)

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

//엣지 그리기
fun DrawScope.drawUltraModernEdges(
    nodes: List<GraphNode>,
    edges: List<GraphEdge>,
    highlightedPath: List<String>,
    scale: Float,
    offset: Offset,
    isContributionMode: Boolean,
    textMeasurer: TextMeasurer
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
                        color = Color(0xFF5dbe50).copy(alpha = 0.3f),
                        start = startPos,
                        end = endPos,
                        strokeWidth = 12f,
                        cap = StrokeCap.Round
                    )
                    // 내부 글로우
                    drawLine(
                        color = Color(0xFF5dbe50).copy(alpha = 0.7f),
                        start = startPos,
                        end = endPos,
                        strokeWidth = 6f,
                        cap = StrokeCap.Round
                    )
                    // 코어 라인
                    drawLine(
                        color = Color(0xFF5dbe50),
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
                        color = Color.Black.copy(alpha = 0.3f),
                        start = startPos,
                        end = endPos,
                        strokeWidth = 2f,
                        cap = StrokeCap.Round
                    )
                }
            }

            // 방향 화살표
            drawUltraArrowHead(startPos, endPos, isHighlighted, isRecording)
            
            // 엣지 라벨 그리기 (스케일 적용 시에만)
            if (scale > 0.7f && !edge.label.isNullOrBlank()) {
                val midPoint = Offset(
                    (startPos.x + endPos.x) / 2f,
                    (startPos.y + endPos.y) / 2f
                )
                
                val textStyle = TextStyle(
                    color = when {
                        isHighlighted -> Color(0xFF5dbe50)
                        isRecording -> Color(0xFFFF4444)
                        else -> Color.Black.copy(alpha = 0.8f)
                    },
                    fontSize = (8 * scale).sp,
                    fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal
                )
                
                val textLayoutResult = textMeasurer.measure(
                    text = edge.label,
                    style = textStyle
                )
                
                // 텍스트 배경 (반투명 박스)
                val textSize = textLayoutResult.size
                val bgPadding = 4f
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.7f),
                    topLeft = Offset(
                        midPoint.x - textSize.width / 2f - bgPadding,
                        midPoint.y - textSize.height / 2f - bgPadding
                    ),
                    size = Size(
                        textSize.width.toFloat() + bgPadding * 2,
                        textSize.height.toFloat() + bgPadding * 2
                    ),
                    cornerRadius = CornerRadius(4f)
                )
                
                // 텍스트 그리기
                drawText(
                    textLayoutResult = textLayoutResult,
                    topLeft = Offset(
                        midPoint.x - textSize.width / 2f,
                        midPoint.y - textSize.height / 2f
                    )
                )
            }
        }
    }
}

//노드 그리기
fun DrawScope.drawUltraModernNodes(
    nodes: List<GraphNode>,
    highlightedPath: List<String>,
    activeNodeId: String?,
    selectedNodeId: String?,
    scale: Float,
    offset: Offset,
    pulseScale: Float,
    isContributionMode: Boolean,
    textMeasurer: TextMeasurer
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

        if (node.type == NodeType.START) {
            // 마름모 모양 그리기
            val diamondPath = Path().apply {
                moveTo(position.x, position.y - radius) // Top
                lineTo(position.x + radius, position.y) // Right
                lineTo(position.x, position.y + radius) // Bottom
                lineTo(position.x - radius, position.y) // Left
                close()
            }

            // 외부 링
            when {
                isActive -> drawPath(path = diamondPath, color = Color(0xFFFF6B6B).copy(alpha = 0.4f), style = Stroke(width = 16f))
                isSelected -> drawPath(path = diamondPath, color = Color(0xFF4ECDC4).copy(alpha = 0.5f), style = Stroke(width = 12f))
                isHighlighted -> drawPath(path = diamondPath, color = Color(0xFF5dbe50).copy(alpha = 0.4f), style = Stroke(width = 8f))
            }

            // 메인 노드
            drawPath(
                path = diamondPath,
                brush = Brush.radialGradient(
                    colors = listOf(
                        node.type.color.copy(alpha = 0.8f),
                        node.type.color.copy(alpha = 0.4f)
                    ),
                    center = position,
                    radius = radius
                )
            )

            // 테두리
            drawPath(
                path = diamondPath,
                color = when {
                    isActive -> Color(0xFFFF6B6B)
                    isSelected -> Color(0xFF4ECDC4)
                    isHighlighted -> Color(0xFF5dbe50)
                    isContributionMode -> Color(0xFFFF4444).copy(alpha = 0.7f)
                    else -> Color.Black.copy(alpha = 0.5f)
                },
                style = Stroke(
                    width = if (isActive || isSelected) 3f else 2f
                )
            )

        } else {
            // 기존 원형 노드 그리기
            // 그림자 효과
            drawCircle(
                color = Color.Black.copy(alpha = 0.3f),
                radius = radius + 4f,
                center = position + Offset(2f, 2f)
            )

            // 외부 링
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
                        color = Color(0xFF5dbe50).copy(alpha = 0.4f),
                        radius = radius + 4f,
                        center = position
                    )
                }
            }

            // 메인 노드
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

            // 내부 하이라이트
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.1f),
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
                    isHighlighted -> Color(0xFF5dbe50)
                    isContributionMode -> Color(0xFFFF4444).copy(alpha = 0.7f)
                    else -> Color.Black.copy(alpha = 0.5f)
                },
                radius = radius,
                center = position,
                style = Stroke(
                    width = if (isActive || isSelected) 3f else 2f
                )
            )
        }

        // 노드 타입 아이콘 (중앙)
        val iconSize = radius * 0.6f
        drawUltraNodeIcon(node.type, position, iconSize)

        // 노드 라벨 그리기 (스케일이 충분할 때만)
        if (scale > 0.8f) {
            val textStyle = TextStyle(
                color = when {
                    isActive -> Color(0xFFFF6B6B)
                    isSelected -> Color(0xFF4ECDC4)
                    isHighlighted -> Color(0xFF5dbe50)
                    else -> Color.Black.copy(alpha = 0.9f)
                },
                fontSize = (10 * scale).sp,
                fontWeight = if (isHighlighted || isActive) FontWeight.Bold else FontWeight.Medium
            )
            
            // 긴 라벨은 줄바꿈
            val displayText = if (node.label.length > 15) {
                node.label.take(12) + "..."
            } else {
                node.label
            }
            
            val textLayoutResult = textMeasurer.measure(
                text = displayText,
                style = textStyle
            )
            
            val textPosition = Offset(
                position.x - textLayoutResult.size.width / 2f,
                position.y + radius + 8f
            )
            
            // 텍스트 배경 (반투명)
            val textSize = textLayoutResult.size
            val bgPadding = 6f
            drawRoundRect(
                color = Color.White.copy(alpha = 0.8f),
                topLeft = Offset(
                    textPosition.x - bgPadding,
                    textPosition.y - bgPadding
                ),
                size = Size(
                    textSize.width.toFloat() + bgPadding * 2,
                    textSize.height.toFloat() + bgPadding * 2
                ),
                cornerRadius = CornerRadius(6f)
            )
            
            // 텍스트 그리기
            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = textPosition
            )
        }
    }
}

//노드 아이콘 그리기
fun DrawScope.drawUltraNodeIcon(nodeType: NodeType, center: Offset, size: Float) {
    val iconColor = Color.Black.copy(alpha = 0.9f)

    when (nodeType) {
        NodeType.START -> {
            // 재생 버튼 아이콘
            val triangleSize = size * 0.6f
            drawPath(
                path = Path().apply {
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
                style = Stroke(width = 2f)
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
        NodeType.ACTION -> {
            // 커서 아이콘
            drawPath(
                path = Path().apply {
                    moveTo(center.x - size * 0.3f, center.y - size * 0.4f)
                    lineTo(center.x + size * 0.2f, center.y)
                    lineTo(center.x - size * 0.1f, center.y + size * 0.1f)
                    lineTo(center.x - size * 0.2f, center.y + size * 0.4f)
                    close()
                },
                color = iconColor
            )
        }
    }
}

//화살표 머리 그리기
fun DrawScope.drawUltraArrowHead(
    start: Offset,
    end: Offset,
    isHighlighted: Boolean,
    isRecording: Boolean
) {
    val arrowLength = if (isHighlighted || isRecording) 12f else 8f
    val arrowAngle = PI / 6

    val lineAngle = atan2(end.y - start.y, end.x - start.x)
    val adjustedEnd = end - Offset(
        cos(lineAngle) * 20f,
        sin(lineAngle) * 20f
    )

    val arrowColor = when {
        isHighlighted -> Color(0xFF5dbe50)
        isRecording -> Color(0xFFFF4444)
        else -> Color.Black.copy(alpha = 0.6f)
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