package com.vowser.client.ui.graph

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.vowser.client.ui.theme.AppTheme
import kotlin.math.max

/**
 * 모던 그래프 시각화 컴포넌트
 */

@Composable
fun ModernNetworkGraph(
    nodes: List<GraphNode>,
    edges: List<GraphEdge>,
    highlightedPath: List<String> = emptyList(),
    activeNodeId: String? = null,
    isContributionMode: Boolean = false,
    searchInfo: com.vowser.client.visualization.SearchInfo? = null
) {
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    var selectedNode by remember { mutableStateOf<GraphNode?>(null) }

    // 레이아웃이 적용된 노드들 계산
    val positionedNodes = remember(nodes, edges, canvasSize) {
        if (canvasSize.width > 0 && canvasSize.height > 0) {
            layoutNodesWithPhysics(nodes, edges, canvasSize)
        } else nodes
    }

    // activeNodeId가 변경되면 해당 노드로 자동 스크롤
    val activeNode = remember(activeNodeId, positionedNodes) {
        positionedNodes.find { it.id == activeNodeId }
    }

    val activeX = activeNode?.x ?: 0f

    // 스크롤 한계(마지막 노드가 오른쪽 끝 살짝 여유를 두고 보이게)
    val maxScroll = remember(positionedNodes, canvasSize) {
        val lastX = positionedNodes.lastOrNull()?.x ?: 0f
        max(0f, lastX - canvasSize.width * 0.5f)
    }

    val targetScrollOffset = remember(activeX, canvasSize, maxScroll, activeNodeId) {
        if (activeNodeId != null && activeX > 0f) {
            // 화면 중앙 = canvasWidth / 2
            val centerAligned = (activeX - canvasSize.width * 0.5f)
            centerAligned.coerceIn(0f, maxScroll)
        } else {
            0f
        }
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
            modifier = Modifier.fillMaxSize()
        )

        // 진행률 바 및 카운터 (상단 좌측)
        if (activeNodeId != null && nodes.isNotEmpty()) {
            val currentStepIndex = nodes.indexOfFirst { it.id == activeNodeId }.coerceAtLeast(0)
            val totalSteps = nodes.size
            val progress = (currentStepIndex + 1).toFloat() / totalSteps.toFloat()

            Card(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .widthIn(min = 250.dp, max = 350.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 진행 카운터
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "경로 진행 상황",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${currentStepIndex + 1} / $totalSteps",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = AppTheme.Colors.Success,
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    )
                }
            }
        }

        // 상단 중앙에 검색 정보 표시
        if (searchInfo != null) {
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp),
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🔍 '${searchInfo.query}'",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "→",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )

                    Text(
                        text = "${searchInfo.totalPaths}개 경로",
                        color = AppTheme.Colors.Success,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )

                    if (searchInfo.topRelevance != null) {
                        Text(
                            text = "·",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            fontSize = 12.sp
                        )

                        Text(
                            text = "관련도 ${(searchInfo.topRelevance * 100).toInt()}%",
                            color = AppTheme.Colors.Info,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Text(
                        text = "·",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        fontSize = 12.sp
                    )

                    Text(
                        text = "${searchInfo.searchTimeMs}ms",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                }
            }
        }

        // 하단 중앙에 현재 노드 description 표시
        if (activeNode != null) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp)
                    .widthIn(max = 600.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 노드 타입 표시
                    Text(
                        text = when (activeNode.type) {
                            NodeType.NAVIGATE -> "🧭 페이지 이동"
                            NodeType.CLICK -> "👆 클릭"
                            NodeType.INPUT -> "⌨️ 입력"
                            NodeType.WAIT -> "⏳ 대기"
                            NodeType.START -> "🚀 시작"
                            NodeType.WEBSITE -> "🌐 웹사이트"
                            NodeType.ACTION -> "⚡ 액션"
                        },
                        color = activeNode.type.color,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Description 표시
                    Text(
                        text = activeNode.label,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // 하단에 범례 표시
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendItem(color = Color(0xFF2196F3), label = "🧭 이동")
                LegendItem(color = Color(0xFF4CAF50), label = "👆 클릭")
                LegendItem(color = Color(0xFFFF9800), label = "⌨️ 입력")
                LegendItem(color = Color(0xFF9C27B0), label = "⏳ 대기")
            }
        }
    }
}

/**
 * 범례 아이템
 */
@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            fontWeight = FontWeight.Medium
        )
    }
}