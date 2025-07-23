package com.vowser.client.ui.graph

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.dp

/**
 * 모던 그래프 시각화 컴포넌트
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

// 그래프 상호작용 타입
enum class GraphInteractionType {
    ToggleMode,
    CenterView,
    ZoomIn,
    ZoomOut,
    Reset
}