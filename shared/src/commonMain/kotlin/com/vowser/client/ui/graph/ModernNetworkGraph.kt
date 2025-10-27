package com.vowser.client.ui.graph

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import com.vowser.client.ui.theme.AppTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import kotlin.math.max

/**
 * 모던 그래프 시각화 컴포넌트
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalResourceApi::class)
@Composable
fun ModernNetworkGraph(
    nodes: List<GraphNode>,
    edges: List<GraphEdge>,
    highlightedPath: List<String> = emptyList(),
    activeNodeId: String? = null,
    isContributionMode: Boolean = false,
    searchInfo: com.vowser.client.visualization.SearchInfo? = null,
    allMatchedPaths: List<com.vowser.client.api.dto.MatchedPathDetail> = emptyList(),
    modifier: Modifier = Modifier,
    contentScale: Float = 1.0f,
) {
    val style = rememberGraphStyle(isContributionMode)
    val cs = MaterialTheme.colorScheme
    val textColor = Color.Black

    var canvasSize by remember { mutableStateOf(Size.Zero) }
    var selectedNode by remember { mutableStateOf<GraphNode?>(null) }
    var showDetailDialog by remember { mutableStateOf(false) }

    val densityScale = LocalDensity.current.density
    val totalScale = densityScale * contentScale
    val positionedNodes = remember(nodes, edges, canvasSize, densityScale) {
        if (canvasSize.width > 0 && canvasSize.height > 0) {
            val logicalSize = Size(canvasSize.width / densityScale, canvasSize.height / densityScale)
            layoutNodesWithPhysics(nodes, edges, logicalSize)
        } else nodes
    }

    val activeNode = remember(activeNodeId, positionedNodes) {
        positionedNodes.find { it.id == activeNodeId }
    }
    val activeX = activeNode?.x ?: 0f
    val maxScroll = remember(positionedNodes, canvasSize, densityScale, contentScale) {
        val lastX = positionedNodes.lastOrNull()?.x ?: 0f
        val logicalWidth = canvasSize.width / totalScale
        max(0f, lastX - logicalWidth * 0.5f)
    }
    val targetScrollOffset = remember(activeX, canvasSize, maxScroll, activeNodeId, densityScale, contentScale) {
        if (activeNodeId != null && activeX > 0f) {
            (activeX - (canvasSize.width / totalScale) * 0.5f).coerceIn(0f, maxScroll)
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

    Box(modifier = modifier) {
        GraphCanvas(
            nodes = positionedNodes,
            edges = edges,
            scale = totalScale,
            offset = Offset(-animatedScrollOffset * totalScale, 0f),
            highlightedPath = highlightedPath,
            activeNodeId = activeNodeId,
            isContributionMode = isContributionMode,
            selectedNode = selectedNode,
            onCanvasSizeChanged = { canvasSize = it },
            modifier = Modifier.fillMaxSize(),
            style = style,
            showGrid = false
        )

        val hudZ = 20f

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .zIndex(hudZ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassPanel(
                modifier = Modifier
                    .wrapContentWidth()
                    .heightIn(min = 30.dp)
                    .zIndex(hudZ)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
                ) {
                    Text(
                        text = "검색:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Text(
                        text = searchInfo?.query ?: "-",
                        fontSize = 14.sp,
                        color = textColor
                    )
                }
            }

            val activeIndex by remember(activeNodeId, nodes) {
                mutableStateOf(nodes.indexOfFirst { it.id == activeNodeId })
            }

            val currentStep by remember(activeIndex) { mutableStateOf(if (activeIndex >= 0) activeIndex + 1 else 0) }
            val totalSteps = nodes.size

            Surface(
                color = Color.Transparent,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .widthIn(min = 260.dp, max = 460.dp)
                    .zIndex(hudZ)
                    .padding(vertical = 6.dp, horizontal = 10.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "경로 진행 상황",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.Black
                    )

                    Spacer(Modifier.height(10.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .wrapContentWidth()
                            .padding(horizontal = 8.dp)
                    ) {
                        repeat(totalSteps) { i ->
                            Box(
                                modifier = Modifier
                                    .size(13.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (i < currentStep)
                                            AppTheme.Colors.StatusBackground // 진행된 단계 색
                                        else
                                            Color.LightGray.copy(alpha = 0.3f) // 남은 단계 색
                                    )
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = "$currentStep / $totalSteps 단계 완료",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .zIndex(hudZ + 1),
            contentAlignment = Alignment.BottomEnd
        ) {
            FilledTonalButton(
                onClick = { showDetailDialog = true },
                shape = CircleShape,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = Color(0xFFF4F4F4),
                    contentColor = Color.Black
                ),
                modifier = Modifier
                    .size(68.dp)
            ) {
                Icon(
                    painter = painterResource("drawable/reading_glasses.png"),
                    contentDescription = "자세히 보기",
                    modifier = Modifier.size(360.dp),
                    tint = Color.Black
                )
            }
        }

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
                        .padding(top = 380.dp)
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
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = activeNode.label,
                            color = textColor,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 150.dp)
                .zIndex(17f),
            contentAlignment = Alignment.TopCenter
        ) {
            Row(
                Modifier
                    .padding(bottom = 22.dp)
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

        if (showDetailDialog && searchInfo != null) {
            PathDetailDialog(
                searchInfo = searchInfo,
                allMatchedPaths = allMatchedPaths,
                onDismiss = { showDetailDialog = false }
            )
        }
    }
}