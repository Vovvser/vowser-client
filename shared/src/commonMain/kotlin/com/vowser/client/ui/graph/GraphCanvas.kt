package com.vowser.client.ui.graph

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.PathEffect
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
    modifier: Modifier = Modifier,
    edgeColorOverride: Color? = null,
    edgeHighlightColorOverride: Color? = null,
    style: GraphStyle,
    showGrid: Boolean = false,
) {
    val textMeasurer = rememberTextMeasurer()

    val pulse by rememberInfiniteTransition().animateFloat(
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
            .pointerInput(Unit) { detectDragGestures { _, _ -> } }
    ) {
        onCanvasSizeChanged(size)

        drawRect(color = style.background)
        if (showGrid) {
            drawGrid(style.gridMinor, 40f)
            drawGrid(style.gridMajor, 200f)
        }

        if (nodes.isNotEmpty()) {
            drawUltraModernEdges(
                nodes = nodes,
                edges = edges,
                highlightedPath = highlightedPath,
                scale = scale,
                offset = offset,
                isContributionMode = isContributionMode,
                textMeasurer = textMeasurer,
                defaultColor = style.edge,
                backgroundColor = style.labelBg,
                edgeColorOverride = edgeColorOverride,
                edgeHighlightColorOverride = edgeHighlightColorOverride,
                style = style
            )

            drawUltraModernNodes(
                nodes = nodes,
                highlightedPath = highlightedPath,
                activeNodeId = activeNodeId,
                selectedNodeId = selectedNode?.id,
                scale = scale,
                offset = offset,
                pulseScale = pulse,
                isContributionMode = isContributionMode,
                textMeasurer = textMeasurer,
                defaultColor = style.labelText,
                backgroundColor = style.labelBg,
                style = style
            )
        }
    }
}

/** 격자 유틸 */
private fun DrawScope.drawGrid(color: Color, cell: Float) {
    var x = 0f
    while (x <= size.width) {
        drawLine(color, Offset(x, 0f), Offset(x, size.height), 1f)
        x += cell
    }
    var y = 0f
    while (y <= size.height) {
        drawLine(color, Offset(0f, y), Offset(size.width, y), 1f)
        y += cell
    }
}

// 중심→중심 선을 원둘레→원둘레 선으로 깎아내는 보정 함수
private fun circleEdgePoints(
    start: Offset, end: Offset,
    rFrom: Float, rTo: Float
): Pair<Offset, Offset> {
    val dx = end.x - start.x
    val dy = end.y - start.y
    val d  = hypot(dx, dy).coerceAtLeast(1e-3f)
    val ux = dx / d
    val uy = dy / d
    val startEdge = Offset(start.x + ux * rFrom, start.y + uy * rFrom)
    val endEdge   = Offset(end.x   - ux * rTo,   end.y   - uy * rTo)
    return startEdge to endEdge
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
    backgroundColor: Color,
    edgeColorOverride: Color? = null,
    edgeHighlightColorOverride: Color? = null,
    style: GraphStyle,
) {
    val nodeMap = nodes.associateBy { it.id }
    val labelSp = (11f * scale).sp
    val dashed = PathEffect.dashPathEffect(floatArrayOf(4f, 3f), 0f)

    edges.forEach { edge ->
        val from = nodeMap[edge.from] ?: return@forEach
        val to   = nodeMap[edge.to]   ?: return@forEach

        val isHi = if (highlightedPath.size > 1) {
            val fi = highlightedPath.indexOf(from.id)
            val ti = highlightedPath.indexOf(to.id)
            fi >= 0 && ti >= 0 && abs(fi - ti) == 1
        } else false

        val c1 = Offset(from.x * scale + offset.x, from.y * scale + offset.y)
        val c2 = Offset(to.x   * scale + offset.x, to.y   * scale + offset.y)
        val rFrom = from.type.size / 2f * scale
        val rTo   = to.type.size   / 2f * scale
        val gap = 6f //원과 선 사이 간격
        val (startEdge, endEdge) = circleEdgePoints(c1, c2, rFrom + gap, rTo + gap)
        val dx = endEdge.x - startEdge.x
        val dy = endEdge.y - startEdge.y
        val d  = hypot(dx, dy).coerceAtLeast(1e-3f)
        val ux = dx / d
        val uy = dy / d
        // 화살촉 밑부분만큼 안쪽으로 라인 끝자락을 당겨서 자연스럽게 연결
        val arrowBaseBack = 6f
        val lineEnd = Offset(endEdge.x - ux * arrowBaseBack, endEdge.y - uy * arrowBaseBack)
        val effect = if (isHi || isContributionMode) null else dashed

        val color = when {
            isContributionMode -> style.edgeError
            isHi               -> style.edgeHighlight
            else               -> style.edge
        }
        val w = if (isHi || isContributionMode) 3f else 2f

        // 바탕 라인(연한색) + 메인 라인(진한색)
        drawLine(
            color = style.edgeSoft,
            start = startEdge, end = lineEnd,
            strokeWidth = w + 2f,
            cap = StrokeCap.Round,
            pathEffect = effect
        )
        drawLine(
            color = color,
            start = startEdge, end = lineEnd,
            strokeWidth = w,
            cap = StrokeCap.Round,
            pathEffect = effect
        )

        // 새 화살표:
        drawArrowHeadAtTip(
            tip = endEdge,
            angle = atan2(dy, dx),
            color = color,
            width = w,
            softColor = style.edgeSoft,
            arrowLen = 8f,                 // ← 길이 더 짧게
            arrowAngleRad = (PI / 7).toFloat()   // 날개 각도(살짝 좁게/넓게 조절 가능)
        )

        if (scale > 0.7f && !edge.label.isNullOrBlank()) {
            val mid = Offset((startEdge.x + endEdge.x) / 2f, (startEdge.y + endEdge.y) / 2f)
            val ts = TextStyle(
                color = color.copy(alpha = 0.95f),
                fontSize = labelSp,
                fontWeight = if (isHi) FontWeight.Bold else FontWeight.Medium
            )
            val layout = textMeasurer.measure(edge.label, style = ts)
            val pad = 4f
            drawRoundRect(
                color = backgroundColor,
                topLeft = Offset(mid.x - layout.size.width / 2f - pad, mid.y - layout.size.height / 2f - pad),
                size = Size(layout.size.width.toFloat() + pad * 2, layout.size.height.toFloat() + pad * 2),
                cornerRadius = CornerRadius(4f)
            )
            drawText(layout, topLeft = Offset(mid.x - layout.size.width / 2f, mid.y - layout.size.height / 2f))
        }
    }
}

private fun DrawScope.drawArrowHead(start: Offset, end: Offset, color: Color, width: Float) {
    val arrowLen = 11f
    val arrowAng = PI / 6
    val ang = atan2(end.y - start.y, end.x - start.x)
    val tip = end - Offset(cos(ang) * 20f, sin(ang) * 20f)

    val e1 = Offset(
        tip.x - arrowLen * cos(ang - arrowAng).toFloat(),
        tip.y - arrowLen * sin(ang - arrowAng).toFloat()
    )
    val e2 = Offset(
        tip.x - arrowLen * cos(ang + arrowAng).toFloat(),
        tip.y - arrowLen * sin(ang + arrowAng).toFloat()
    )
    drawLine(color, tip, e1, width, StrokeCap.Round)
    drawLine(color, tip, e2, width, StrokeCap.Round)
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
    backgroundColor: Color,
    style: GraphStyle,
) {
    val nodeLabelSp = (13f * scale).sp

    nodes.forEach { node ->
        val isHi = highlightedPath.contains(node.id)
        val isActive = node.id == activeNodeId
        val isSel = node.id == selectedNodeId

        val pos = Offset(node.x * scale + offset.x, node.y * scale + offset.y)
        val baseR = node.type.size / 2f * scale
        val r = when {
            isActive -> baseR * pulseScale * 1.25f
            isSel    -> baseR * 1.15f
            isHi     -> baseR * 1.08f
            else     -> baseR
        }

        val fill = when (node.type) {
            NodeType.NAVIGATE -> style.nodeNavigate
            NodeType.CLICK    -> style.nodeClick
            NodeType.INPUT    -> style.nodeInput
            NodeType.WAIT     -> style.nodeWait
            NodeType.START    -> style.nodeStart
            NodeType.WEBSITE  -> style.nodeWebsite
            NodeType.ACTION   -> style.nodeAction
        }

        val border = when {
            node.id == activeNodeId        -> style.edgeActive
            isContributionMode             -> style.edgeError
            isSel || isHi                  -> style.edgeHighlight
            else                           -> style.edge
        }

        drawCircle(color = fill.copy(0.20f), radius = r + 10f, center = pos)
        drawCircle(color = fill.copy(0.10f), radius = r + 18f, center = pos)

        if (node.type == NodeType.START) {
            val p = Path().apply {
                moveTo(pos.x, pos.y - r)
                lineTo(pos.x + r, pos.y)
                lineTo(pos.x, pos.y + r)
                lineTo(pos.x - r, pos.y)
                close()
            }
            drawPath(
                path = p,
                brush = Brush.radialGradient(
                    colors = listOf(fill.copy(0.95f), fill.copy(0.45f)),
                    center = pos, radius = r
                )
            )
            drawPath(path = p, color = border, style = Stroke(width = if (isActive) 3.5f else 2.5f))
            // 아이콘(▶︎)
            drawUltraNodeIcon(NodeType.START, pos, r * 0.6f, style.nodeIcon)
        } else {
            // 본체
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(fill.copy(0.95f), fill.copy(0.45f)),
                    center = pos, radius = r
                ),
                radius = r, center = pos
            )
            // 하이라이트 스팟
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.12f), Color.Transparent),
                    center = pos - Offset(r * 0.35f, r * 0.35f),
                    radius = r * 0.6f
                ),
                radius = r, center = pos
            )
            // 듀얼 링
            drawCircle(color = style.edgeSoft, radius = r + 3f, center = pos, style = Stroke(3f))
            drawCircle(color = border,        radius = r,     center = pos, style = Stroke(if (isActive) 3.5f else 2.5f))

            // WEBSITE는 중앙 코어 도트 추가
            if (node.type == NodeType.WEBSITE) {
                drawCircle(color = style.nodeIcon.copy(alpha = 0.75f), radius = r * 0.12f, center = pos)
            }

            // ACTION은 ▶︎ 아이콘, 나머지는 타입별 아이콘
            val iconType = if (node.type == NodeType.ACTION) NodeType.START else node.type
            drawUltraNodeIcon(iconType, pos, r * 0.60f, style.nodeIcon)
        }

        // 라벨
        if (scale > 0.8f) {
            val textStyle = TextStyle(
                color = style.labelText,
                fontSize = nodeLabelSp,
                fontWeight = if (isHi || isActive) FontWeight.Bold else FontWeight.Medium
            )
            val txt = if (node.label.length > 15) node.label.take(12) + "..." else node.label
            val layout = textMeasurer.measure(txt, style = textStyle)
            val topLeft = Offset(pos.x - layout.size.width / 2f, pos.y + r + 8f)
            val pad = 6f
            drawRoundRect(
                color = style.labelBg,
                topLeft = Offset(topLeft.x - pad, topLeft.y - pad),
                size = Size(layout.size.width.toFloat() + pad * 2, layout.size.height.toFloat() + pad * 2),
                cornerRadius = CornerRadius(6f)
            )
            drawText(layout, topLeft = topLeft)
        }
    }
}

private fun DrawScope.drawUltraNodeIcon(nodeType: NodeType, center: Offset, size: Float, iconColor: Color) {
    when (nodeType) {
        NodeType.NAVIGATE -> {
            drawPath(Path().apply {
                moveTo(center.x - size * 0.3f, center.y)
                lineTo(center.x + size * 0.3f, center.y)
                moveTo(center.x + size * 0.1f, center.y - size * 0.2f)
                lineTo(center.x + size * 0.3f, center.y)
                lineTo(center.x + size * 0.1f, center.y + size * 0.2f)
            }, iconColor, style = Stroke(2f, cap = StrokeCap.Round))
        }
        NodeType.CLICK -> {
            drawCircle(iconColor, size * 0.3f, center, style = Stroke(2f))
            drawCircle(iconColor, size * 0.1f, center)
        }
        NodeType.INPUT -> {
            val rect = size * 0.6f
            drawRoundRect(iconColor, Offset(center.x - rect / 2, center.y - rect / 3), Size(rect, rect * 0.6f), style = Stroke(2f), cornerRadius = CornerRadius(2f))
            drawLine(iconColor, Offset(center.x - rect * 0.2f, center.y), Offset(center.x + rect * 0.2f, center.y), 2f)
        }
        NodeType.WAIT -> {
            drawCircle(iconColor, size * 0.4f, center, style = Stroke(2f))
            drawLine(iconColor, center, Offset(center.x, center.y - size * 0.3f), 2f)
            drawLine(iconColor, center, Offset(center.x + size * 0.2f, center.y), 2f)
        }
        NodeType.START -> {
            val s = size * 0.6f
            drawPath(Path().apply {
                moveTo(center.x - s * 0.3f, center.y - s * 0.5f)
                lineTo(center.x + s * 0.5f, center.y)
                lineTo(center.x - s * 0.3f, center.y + s * 0.5f)
                close()
            }, iconColor)
        }
        NodeType.WEBSITE -> {
            drawCircle(iconColor, size * 0.4f, center, style = Stroke(2f))
            drawLine(iconColor, Offset(center.x, center.y - size * 0.4f), Offset(center.x, center.y + size * 0.4f), 2f)
            drawLine(iconColor, Offset(center.x - size * 0.4f, center.y), Offset(center.x + size * 0.4f, center.y), 2f)
        }
        NodeType.ACTION -> {
            drawPath(Path().apply {
                moveTo(center.x - size * 0.3f, center.y - size * 0.4f)
                lineTo(center.x + size * 0.2f, center.y)
                lineTo(center.x - size * 0.1f, center.y + size * 0.1f)
                lineTo(center.x - size * 0.2f, center.y + size * 0.4f)
                close()
            }, iconColor)
        }
    }
}

private fun DrawScope.drawArrowHeadAtTip(
    tip: Offset,
    angle: Float,
    color: Color,
    width: Float,
    softColor: Color? = null,
    arrowLen: Float = 8f,
    arrowAngleRad: Float = (PI / 7).toFloat()
) {
    val e1 = Offset(
        tip.x - arrowLen * cos(angle - arrowAngleRad),
        tip.y - arrowLen * sin(angle - arrowAngleRad)
    )
    val e2 = Offset(
        tip.x - arrowLen * cos(angle + arrowAngleRad),
        tip.y - arrowLen * sin(angle + arrowAngleRad)
    )
    if (softColor != null) {
        drawLine(color = softColor, start = tip, end = e1, strokeWidth = width + 1f, cap = StrokeCap.Round)
        drawLine(color = softColor, start = tip, end = e2, strokeWidth = width + 1f, cap = StrokeCap.Round)
    }
    drawLine(color = color, start = tip, end = e1, strokeWidth = width, cap = StrokeCap.Round)
    drawLine(color = color, start = tip, end = e2, strokeWidth = width, cap = StrokeCap.Round)
}