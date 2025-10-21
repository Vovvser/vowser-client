package com.vowser.client.ui.graph

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlin.math.max

/**
 * 모던 그래프 시각화 컴포넌트
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

        val hudZ = 20f

        // 🔹 상단 HUD 전체 박스
        // Row의 padding 값을 조정하여 전체 상단 UI의 위치를 변경할 수 있습니다.
        // horizontalArrangement는 패널 사이의 수평 정렬을 제어합니다.
        // - Arrangement.SpaceBetween: 양 끝에 붙이고 나머지를 균등 배치
        // - Arrangement.SpaceAround: 모든 패널 둘레에 균등 공간 부여
        // - Arrangement.SpaceEvenly: 모든 패널 사이에 균등 공간 부여
        // - Arrangement.Start: 왼쪽에 모두 붙임
        // - Arrangement.End: 오른쪽에 모두 붙임
        // - Arrangement.Center: 중앙에 모두 모음
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                // 예: .padding(top = 40.dp) // 전체 상단 HUD를 아래로 28dp 더 내림 (기존 12dp + 28dp)
                .zIndex(hudZ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 🔹 왼쪽: 검색 정보 패널
            // padding: 다른 UI에 영향을 주며 패널의 공간을 확보 후 이동
            // offset: 다른 UI에 영향을 주지 않고 패널만 시각적으로 이동 (겹칠 수 있음)
            GlassPanel(
                modifier = Modifier
                    .wrapContentWidth()
                    .heightIn(min = 48.dp)
                    // --- 위치 조정 예시 ---
                    // .padding(top = 20.dp) // 아래로 이동
                    // .padding(bottom = 20.dp) // 위로 이동
                    // .padding(start = 20.dp) // 오른쪽으로 이동
                    // .padding(end = 20.dp) // 왼쪽으로 이동
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

            // 🔹 중앙: "자세히 보기" 버튼 패널
            // 패널 순서를 바꾸려면 Row 내에서 이 Box 블록의 위치를 직접 옮기세요.
            Box(
                Modifier
                    .wrapContentSize()
                    // --- 위치 조정 예시 ---
                    // .offset(y = 20.dp) // 아래로 이동
                    // .offset(y = -20.dp) // 위로 이동
                    // .offset(x = 20.dp) // 오른쪽으로 이동
                    // .offset(x = -20.dp) // 왼쪽으로 이동
                    .zIndex(hudZ)
            ) {
                OutlinedButton(
                    onClick = { showDetailDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, cs.outline.copy(alpha = 0.2f))
                ) {
                    Text(
                        text = "경로 진행 상황",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = cs.onSurface
                    )
                }
            }

            // 🔹 오른쪽: 진행 상태 패널
            val progress = remember(activeNodeId, nodes) {
                val idx = nodes.indexOfFirst { it.id == activeNodeId }.coerceAtLeast(0)
                if (nodes.isNotEmpty()) ((idx + 1).toFloat() / nodes.size.toFloat()).coerceIn(0f, 1f) else 0f
            }
            GlassPanel(
                modifier = Modifier
                    .widthIn(min = 220.dp, max = 420.dp)
                    // --- 위치 조정 예시 ---
                    // .padding(top = 20.dp) // 아래로 이동
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
        }


        // 🔹 노드 유형 범례 패널 (현재 상단 기준 80dp)
        // Box의 contentAlignment와 padding을 조합하여 위치를 조정합니다.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 400.dp) // 이 값을 늘리면 아래로, 줄이면 위로 이동
                .zIndex(17f),
            contentAlignment = Alignment.TopCenter // 현재 상단 중앙. (TopStart, TopEnd 등으로 변경 가능)
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

        // 🔹 경로 상세 정보 다이얼로그
        // 다이얼로그는 항상 화면 중앙에 표시되며, 위치를 직접 제어할 수 없습니다.
        if (showDetailDialog && searchInfo != null) {
            PathDetailDialog(
                searchInfo = searchInfo,
                allMatchedPaths = allMatchedPaths,
                onDismiss = { showDetailDialog = false }
            )
        }
    }
}