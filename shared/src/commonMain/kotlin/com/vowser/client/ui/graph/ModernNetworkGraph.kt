package com.vowser.client.ui.graph

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlin.math.max
import androidx.compose.material3.surfaceColorAtElevation

/**
 * 모던 그래프 시각화 컴포넌트 (글래스 HUD 스타일)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernNetworkGraph(
    nodes: List<GraphNode>,
    edges: List<GraphEdge>,
    highlightedPath: List<String> = emptyList(),
    activeNodeId: String? = null,
    isContributionMode: Boolean = false,
    searchInfo: com.vowser.client.visualization.SearchInfo? = null,
    allMatchedPaths: List<com.vowser.client.api.dto.MatchedPathDetail> = emptyList()
) {
    val style = rememberGraphStyle(isContributionMode)
    val cs = MaterialTheme.colorScheme

    var canvasSize by remember { mutableStateOf(Size.Zero) }
    var selectedNode by remember { mutableStateOf<GraphNode?>(null) }
    var showDetailDialog by remember { mutableStateOf(false) }

    val positionedNodes = remember(nodes, edges, canvasSize) {
        if (canvasSize.width > 0 && canvasSize.height > 0) {
            layoutNodesWithPhysics(nodes, edges, canvasSize)
        } else nodes
    }

    val activeNode = remember(activeNodeId, positionedNodes) {
        positionedNodes.find { it.id == activeNodeId }
    }
    val activeX = activeNode?.x ?: 0f
    val maxScroll = remember(positionedNodes, canvasSize) {
        val lastX = positionedNodes.lastOrNull()?.x ?: 0f
        max(0f, lastX - canvasSize.width * 0.5f)
    }
    val targetScrollOffset = remember(activeX, canvasSize, maxScroll, activeNodeId) {
        if (activeNodeId != null && activeX > 0f) {
            (activeX - canvasSize.width * 0.5f).coerceIn(0f, maxScroll)
        } else 0f
    }
    val animatedScrollOffset by animateFloatAsState(
        targetValue = targetScrollOffset,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "graph-auto-scroll"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Canvas: 그리드 숨기고 HUD가 위에 오도록 z-index로 패널을 띄움
        GraphCanvas(
            nodes = positionedNodes,
            edges = edges,
            canvasSize = canvasSize,
            scale = 1f,
            offset = Offset(-animatedScrollOffset, 0f),
            highlightedPath = highlightedPath,
            activeNodeId = activeNodeId,
            isContributionMode = isContributionMode,
            selectedNode = selectedNode,
            onCanvasSizeChanged = { canvasSize = it },
            modifier = Modifier.fillMaxSize(),
            edgeColorOverride = style.edge,
            edgeHighlightColorOverride = style.edgeHighlight,
            style = style,
            showGrid = false
        )

        // -------------------------
        // Top HUD Row (left: search, center: progress, right: details button)
        // -------------------------
        val hudZ = 20f
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .zIndex(hudZ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: search summary (Glass)
            GlassPanel(
                modifier = Modifier
                    .wrapContentWidth()
                    .heightIn(min = 48.dp)
                    .zIndex(hudZ)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "검색:",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = cs.onSurface
                    )
                    Text(
                        text = searchInfo?.query ?: "-",
                        fontSize = 15.sp,
                        color = cs.onSurface.copy(alpha = 0.9f)
                    )
                    Spacer(Modifier.width(8.dp))
                    PillChip(
                        text = "${searchInfo?.totalPaths ?: 0} 경로",
                        bg = cs.primary.copy(alpha = 0.12f),
                        fg = cs.primary
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${searchInfo?.searchTimeMs ?: 0}ms",
                        fontSize = 13.sp,
                        color = cs.onSurface.copy(alpha = 0.65f)
                    )
                }
            }

            // Center: progress summary (Glass)
            val progress = remember(activeNodeId, nodes) {
                val idx = nodes.indexOfFirst { it.id == activeNodeId }.coerceAtLeast(0)
                if (nodes.isNotEmpty()) ((idx + 1).toFloat() / nodes.size.toFloat()).coerceIn(0f, 1f) else 0f
            }
            GlassPanel(
                modifier = Modifier
                    .widthIn(min = 220.dp, max = 420.dp)
                    .zIndex(hudZ)
            ) {
                Column {
                    Text(
                        text = "경로 진행 상황",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = style.nodeClick
                    )
                    Spacer(Modifier.height(8.dp))
                    ProgressPill(progress = progress, modifier = Modifier.fillMaxWidth(), height = 10.dp)
                }
            }

            // Right: 자세히 보기 버튼 (rounded)
            Box(
                Modifier
                    .wrapContentSize()
                    .zIndex(hudZ)
            ) {
                OutlinedButton(
                    onClick = { showDetailDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, cs.outline.copy(alpha = 0.2f))
                ) {
                    Text(
                        text = "자세히 보기",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = cs.onSurface
                    )
                }
            }
        }

        // -------------------------
        // Active node info (center-bottom) as Glass panel
        // -------------------------
        if (activeNode != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(18f),
                contentAlignment = Alignment.BottomCenter
            ) {
                GlassPanel(
                    modifier = Modifier
                        .widthIn(max = 650.dp)
                        .padding(bottom = 110.dp)
                        .zIndex(18f)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val typeColor = when (activeNode.type) {
                            NodeType.NAVIGATE -> style.nodeNavigate
                            NodeType.CLICK    -> style.nodeClick
                            NodeType.INPUT    -> style.nodeInput
                            NodeType.WAIT     -> style.nodeWait
                            NodeType.START, NodeType.WEBSITE, NodeType.ACTION -> cs.primary
                        }
                        Text(
                            text = when (activeNode.type) {
                                NodeType.NAVIGATE -> "페이지 이동"
                                NodeType.CLICK    -> "클릭"
                                NodeType.INPUT    -> "입력"
                                NodeType.WAIT     -> "대기"
                                NodeType.START    -> "시작"
                                NodeType.WEBSITE  -> "웹사이트"
                                NodeType.ACTION   -> "액션"
                            },
                            color = typeColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = activeNode.label,
                            color = cs.onSurface,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // -------------------------
        // Legend (bottom center) - compact pill chips
        // -------------------------
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .zIndex(17f),
            contentAlignment = Alignment.BottomCenter
        ) {
            Row(
                Modifier
                    .padding(bottom = 18.dp)
                    .wrapContentWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PillChip(text = "이동", bg = style.nodeNavigate.copy(alpha = 0.12f), fg = style.nodeNavigate)
                PillChip(text = "클릭", bg = style.nodeClick.copy(alpha = 0.12f), fg = style.nodeClick)
                PillChip(text = "입력", bg = style.nodeInput.copy(alpha = 0.12f), fg = style.nodeInput)
                PillChip(text = "대기", bg = style.nodeWait.copy(alpha = 0.12f), fg = style.nodeWait)
            }
        }

        // -------------------------
        // Details dialog trigger
        // -------------------------
        if (showDetailDialog && searchInfo != null) {
            PathDetailDialog(
                searchInfo = searchInfo,
                allMatchedPaths = allMatchedPaths,
                onDismiss = { showDetailDialog = false }
            )
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), CircleShape)
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            fontWeight = FontWeight.Medium
        )
    }
}