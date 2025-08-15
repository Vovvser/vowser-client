package com.vowser.client.ui.graph

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
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

    val textMeasurer = rememberTextMeasurer()
    val pulseAnimation = rememberInfiniteTransition()
    val pulseScale by pulseAnimation.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        )
    )

    val onSurfaceColor = MaterialTheme.colors.onSurface
    val surfaceColor = MaterialTheme.colors.surface

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures { _, _ -> }
            }
    ) {
        onCanvasSizeChanged(size)

        if (positionedNodes.isNotEmpty()) {
            drawUltraModernGrid(onSurfaceColor.copy(alpha = 0.05f))

            drawUltraModernEdges(
                nodes = positionedNodes,
                edges = edges,
                highlightedPath = highlightedPath,
                scale = scale,
                offset = offset,
                isContributionMode = isContributionMode,
                textMeasurer = textMeasurer,
                defaultColor = onSurfaceColor,
                backgroundColor = surfaceColor
            )

            drawUltraModernNodes(
                nodes = positionedNodes,
                highlightedPath = highlightedPath,
                activeNodeId = activeNodeId,
                selectedNodeId = selectedNode?.id,
                scale = scale,
                offset = offset,
                pulseScale = pulseScale,
                isContributionMode = isContributionMode,
                textMeasurer = textMeasurer,
                defaultColor = onSurfaceColor,
                backgroundColor = surfaceColor
            )
        }
    }
}

private fun DrawScope.drawUltraModernGrid(gridColor: Color) {
    val gridSize = 40f
    var x = 0f
    while (x <= size.width) {
        drawLine(color = gridColor, start = Offset(x, 0f), end = Offset(x, size.height), strokeWidth = 1f)
        x += gridSize
    }
    var y = 0f
    while (y <= size.height) {
        drawLine(color = gridColor, start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = 1f)
        y += gridSize
    }
}

private fun DrawScope.drawUltraModernEdges(
    nodes: List<GraphNode>,
    edges: List<GraphEdge>,
    highlightedPath: List<String>,
    scale: Float,
    offset: Offset,
    isContributionMode: Boolean,
    textMeasurer: TextMeasurer,
    defaultColor: Color,
    backgroundColor: Color
) {
    val nodeMap = nodes.associateBy { it.id }

    edges.forEach { edge ->
        val fromNode = nodeMap[edge.from]
        val toNode = nodeMap[edge.to]

        if (fromNode != null && toNode != null) {
            val isHighlighted = highlightedPath.contains(fromNode.id) && highlightedPath.contains(toNode.id)

            val startPos = Offset(fromNode.x * scale + offset.x, fromNode.y * scale + offset.y)
            val endPos = Offset(toNode.x * scale + offset.x, toNode.y * scale + offset.y)

            val edgeColor = when {
                isHighlighted -> Color(0xFF5dbe50)
                isContributionMode -> Color(0xFF9F4147)
                else -> defaultColor
            }

            val alpha = if (isHighlighted || isContributionMode) 1f else 0.3f

            drawLine(color = edgeColor.copy(alpha = alpha), start = startPos, end = endPos, strokeWidth = if (isHighlighted) 3f else 2f, cap = StrokeCap.Round)

            drawUltraArrowHead(startPos, endPos, isHighlighted, isContributionMode, defaultColor)

            if (scale > 0.7f && !edge.label.isNullOrBlank()) {
                val midPoint = Offset((startPos.x + endPos.x) / 2f, (startPos.y + endPos.y) / 2f)
                val textStyle = TextStyle(
                    color = edgeColor.copy(alpha = if (isHighlighted || isContributionMode) 1f else 0.8f),
                    fontSize = (8 * scale).sp,
                    fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal
                )
                val textLayoutResult = textMeasurer.measure(edge.label, style = textStyle)
                val bgPadding = 4f
                drawRoundRect(
                    color = backgroundColor.copy(alpha = 0.7f),
                    topLeft = Offset(midPoint.x - textLayoutResult.size.width / 2f - bgPadding, midPoint.y - textLayoutResult.size.height / 2f - bgPadding),
                    size = Size(textLayoutResult.size.width.toFloat() + bgPadding * 2, textLayoutResult.size.height.toFloat() + bgPadding * 2),
                    cornerRadius = CornerRadius(4f)
                )
                drawText(textLayoutResult, topLeft = Offset(midPoint.x - textLayoutResult.size.width / 2f, midPoint.y - textLayoutResult.size.height / 2f))
            }
        }
    }
}

private fun DrawScope.drawUltraModernNodes(
    nodes: List<GraphNode>,
    highlightedPath: List<String>,
    activeNodeId: String?,
    selectedNodeId: String?,
    scale: Float,
    offset: Offset,
    pulseScale: Float,
    isContributionMode: Boolean,
    textMeasurer: TextMeasurer,
    defaultColor: Color,
    backgroundColor: Color
) {
    nodes.forEach { node ->
        val isHighlighted = highlightedPath.contains(node.id)
        val isActive = node.id == activeNodeId
        val isSelected = node.id == selectedNodeId

        val position = Offset(node.x * scale + offset.x, node.y * scale + offset.y)
        val baseRadius = node.type.size / 2f * scale
        val radius = when {
            isActive -> baseRadius * pulseScale * 1.3f
            isSelected -> baseRadius * 1.2f
            isHighlighted -> baseRadius * 1.1f
            else -> baseRadius
        }

        val borderColor = when {
            isActive -> Color(0xFFFF6B6B)
            isSelected -> Color(0xFF4ECDC4)
            isHighlighted -> Color(0xFF5dbe50)
            isContributionMode -> Color(0xFF9F4147)
            else -> defaultColor
        }

        if (node.type == NodeType.START) {
            val diamondPath = Path().apply {
                moveTo(position.x, position.y - radius)
                lineTo(position.x + radius, position.y)
                lineTo(position.x, position.y + radius)
                lineTo(position.x - radius, position.y)
                close()
            }
            drawPath(path = diamondPath, brush = Brush.radialGradient(colors = listOf(node.type.color.copy(alpha = 0.8f), node.type.color.copy(alpha = 0.4f)), center = position, radius = radius))
            drawPath(path = diamondPath, color = borderColor.copy(alpha = if (isActive || isSelected) 1f else 0.5f), style = Stroke(width = if (isActive || isSelected) 3f else 2f))
        } else {
            drawCircle(color = defaultColor.copy(alpha = 0.3f), radius = radius + 4f, center = position + Offset(2f, 2f))
            drawCircle(brush = Brush.radialGradient(colors = listOf(node.type.color.copy(alpha = 0.8f), node.type.color.copy(alpha = 0.4f)), center = position, radius = radius), radius = radius, center = position)
            drawCircle(brush = Brush.radialGradient(colors = listOf(defaultColor.copy(alpha = 0.1f), Color.Transparent), center = position - Offset(radius * 0.3f, radius * 0.3f), radius = radius * 0.5f), radius = radius, center = position)
            drawCircle(color = borderColor.copy(alpha = if (isActive || isSelected) 1f else 0.5f), radius = radius, center = position, style = Stroke(width = if (isActive || isSelected) 3f else 2f))
        }

        drawUltraNodeIcon(node.type, position, radius * 0.6f, defaultColor)

        if (scale > 0.8f) {
            val textStyle = TextStyle(
                color = borderColor.copy(alpha = if (isHighlighted || isActive) 1f else 0.9f),
                fontSize = (10 * scale).sp,
                fontWeight = if (isHighlighted || isActive) FontWeight.Bold else FontWeight.Medium
            )
            val displayText = if (node.label.length > 15) node.label.take(12) + "..." else node.label
            val textLayoutResult = textMeasurer.measure(displayText, style = textStyle)
            val textPosition = Offset(position.x - textLayoutResult.size.width / 2f, position.y + radius + 8f)
            val bgPadding = 6f
            drawRoundRect(
                color = backgroundColor.copy(alpha = 0.8f),
                topLeft = Offset(textPosition.x - bgPadding, textPosition.y - bgPadding),
                size = Size(textLayoutResult.size.width.toFloat() + bgPadding * 2, textLayoutResult.size.height.toFloat() + bgPadding * 2),
                cornerRadius = CornerRadius(6f)
            )
            drawText(textLayoutResult, topLeft = textPosition)
        }
    }
}

private fun DrawScope.drawUltraNodeIcon(nodeType: NodeType, center: Offset, size: Float, iconColor: Color) {
    when (nodeType) {
        NodeType.START -> {
            val triangleSize = size * 0.6f
            drawPath(path = Path().apply {
                moveTo(center.x - triangleSize * 0.3f, center.y - triangleSize * 0.5f)
                lineTo(center.x + triangleSize * 0.5f, center.y)
                lineTo(center.x - triangleSize * 0.3f, center.y + triangleSize * 0.5f)
                close()
            }, color = iconColor)
        }
        NodeType.WEBSITE -> {
            drawCircle(color = iconColor, radius = size * 0.4f, center = center, style = Stroke(width = 2f))
            drawLine(color = iconColor, start = Offset(center.x, center.y - size * 0.4f), end = Offset(center.x, center.y + size * 0.4f), strokeWidth = 2f)
            drawLine(color = iconColor, start = Offset(center.x - size * 0.4f, center.y), end = Offset(center.x + size * 0.4f, center.y), strokeWidth = 2f)
        }
        NodeType.ACTION -> {
            drawPath(path = Path().apply {
                moveTo(center.x - size * 0.3f, center.y - size * 0.4f)
                lineTo(center.x + size * 0.2f, center.y)
                lineTo(center.x - size * 0.1f, center.y + size * 0.1f)
                lineTo(center.x - size * 0.2f, center.y + size * 0.4f)
                close()
            }, color = iconColor)
        }
    }
}

private fun DrawScope.drawUltraArrowHead(start: Offset, end: Offset, isHighlighted: Boolean, isRecording: Boolean, defaultColor: Color) {
    val arrowLength = if (isHighlighted || isRecording) 12f else 8f
    val arrowAngle = PI / 6
    val lineAngle = atan2(end.y - start.y, end.x - start.x)
    val adjustedEnd = end - Offset(cos(lineAngle) * 20f, sin(lineAngle) * 20f)

    val arrowColor = when {
        isHighlighted -> Color(0xFF5dbe50)
        isRecording -> Color(0xFF9F4147)
        else -> defaultColor
    }

    val arrowEnd1 = Offset(adjustedEnd.x - arrowLength * cos(lineAngle - arrowAngle).toFloat(), adjustedEnd.y - arrowLength * sin(lineAngle - arrowAngle).toFloat())
    val arrowEnd2 = Offset(adjustedEnd.x - arrowLength * cos(lineAngle + arrowAngle).toFloat(), adjustedEnd.y - arrowLength * sin(lineAngle + arrowAngle).toFloat())

    drawLine(color = arrowColor, start = adjustedEnd, end = arrowEnd1, strokeWidth = if (isHighlighted || isRecording) 3f else 2f, cap = StrokeCap.Round)
    drawLine(color = arrowColor, start = adjustedEnd, end = arrowEnd2, strokeWidth = if (isHighlighted || isRecording) 3f else 2f, cap = StrokeCap.Round)
}