package com.vowser.client.ui.graph

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.*

data class GraphNode(
    val id: String,
    val label: String,
    val type: NodeType = NodeType.DEFAULT,
    val x: Float = 0f,
    val y: Float = 0f
)

data class GraphEdge(
    val from: String,
    val to: String,
    val label: String? = null
)

enum class NodeType(val color: Color, val size: Float) {
    START(Color(0xFF4CAF50), 40f),      // 시작 노드 (초록)
    WEBSITE(Color(0xFF2196F3), 35f),    // 웹사이트 노드 (파랑)
    PAGE(Color(0xFFFF9800), 30f),       // 페이지 노드 (주황)
    ACTION(Color(0xFF9C27B0), 25f),     // 액션 노드 (보라)
    DEFAULT(Color(0xFF607D8B), 30f)     // 기본 노드 (회색)
}

@Composable
fun NetworkGraphComponent(
    nodes: List<GraphNode>,
    edges: List<GraphEdge>,
    modifier: Modifier = Modifier,
    highlightedPath: List<String> = emptyList(), // 하이라이트할 경로
    activeNodeId: String? = null,                // 활성 노드
    onNodeClick: (GraphNode) -> Unit = {}
) {
    var canvasSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }
    
    // 노드 위치 계산 (계층형 사이트맵 배치)
    val positionedNodes = remember(nodes, canvasSize) {
        if (canvasSize.width > 0 && canvasSize.height > 0) {
            layoutNodesHierarchically(nodes, edges, canvasSize)
        } else {
            nodes
        }
    }
    
    Card(
        modifier = modifier,
        elevation = 4.dp
    ) {
        Column {
            // 헤더
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colors.primary)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "웹 탐색 경로",
                    color = Color.White,
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${nodes.size}개 노드",
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.caption
                )
            }
            
            // 그래프 캔버스
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .background(Color.White)
            ) {
                canvasSize = size
                
                if (positionedNodes.isNotEmpty()) {
                    // 엣지 그리기 (하이라이트 적용)
                    drawEdges(positionedNodes, edges, highlightedPath)
                    
                    // 노드 그리기 (하이라이트 적용)
                    drawNodes(positionedNodes, highlightedPath, activeNodeId)
                }
            }
            
            // 범례
            NetworkGraphLegend(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }
    }
}

@Composable
private fun NetworkGraphLegend(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = "범례",
            style = MaterialTheme.typography.subtitle2,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyVerticalGrid(
            columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NodeType.values().forEach { nodeType ->
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(nodeType.color)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when (nodeType) {
                                NodeType.START -> "시작점"
                                NodeType.WEBSITE -> "웹사이트"
                                NodeType.PAGE -> "페이지"
                                NodeType.ACTION -> "액션"
                                NodeType.DEFAULT -> "기본"
                            },
                            style = MaterialTheme.typography.caption
                        )
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawEdges(
    nodes: List<GraphNode>, 
    edges: List<GraphEdge>, 
    highlightedPath: List<String>
) {
    val nodeMap = nodes.associateBy { it.id }
    
    edges.forEach { edge ->
        val fromNode = nodeMap[edge.from]
        val toNode = nodeMap[edge.to]
        
        if (fromNode != null && toNode != null) {
            // 하이라이트된 경로인지 확인
            val isHighlighted = highlightedPath.contains(fromNode.id) && highlightedPath.contains(toNode.id)
            
            // 화살표가 있는 선 그리기
            drawArrowLine(
                start = Offset(fromNode.x, fromNode.y),
                end = Offset(toNode.x, toNode.y),
                color = if (isHighlighted) Color(0xFF4CAF50) else Color.Gray, // 하이라이트시 초록색
                strokeWidth = if (isHighlighted) 4.dp.toPx() else 2.dp.toPx()  // 하이라이트시 두껍게
            )
        }
    }
}

private fun DrawScope.drawNodes(
    nodes: List<GraphNode>, 
    highlightedPath: List<String>, 
    activeNodeId: String?
) {
    nodes.forEach { node ->
        val isHighlighted = highlightedPath.contains(node.id)
        val isActive = node.id == activeNodeId
        
        // 노드 크기 조정 (하이라이트/활성시 크게)
        val nodeRadius = when {
            isActive -> node.type.size / 2f * 1.3f
            isHighlighted -> node.type.size / 2f * 1.1f
            else -> node.type.size / 2f
        }
        
        // 노드 원 그리기
        drawCircle(
            color = when {
                isActive -> Color(0xFFFF5722)     // 활성 노드는 주황색
                isHighlighted -> node.type.color.copy(alpha = 1f) // 하이라이트시 선명하게
                else -> node.type.color.copy(alpha = 0.7f)         // 일반 노드는 반투명
            },
            radius = nodeRadius,
            center = Offset(node.x, node.y)
        )
        
        // 노드 테두리
        drawCircle(
            color = when {
                isActive -> Color(0xFFFF5722)
                isHighlighted -> Color(0xFF4CAF50)
                else -> Color.Black.copy(alpha = 0.5f)
            },
            radius = nodeRadius,
            center = Offset(node.x, node.y),
            style = Stroke(width = if (isHighlighted || isActive) 3.dp.toPx() else 2.dp.toPx())
        )
        
        // 노드 라벨은 일단 생략 (Canvas 텍스트 렌더링 복잡함)
        // 대신 별도 Text Composable로 표시하거나 나중에 구현
    }
}

private fun DrawScope.drawArrowLine(
    start: Offset,
    end: Offset,
    color: Color,
    strokeWidth: Float
) {
    // 선 그리기
    drawLine(
        color = color,
        start = start,
        end = end,
        strokeWidth = strokeWidth
    )
    
    // 화살표 머리 그리기
    val arrowLength = 15f
    val arrowAngle = PI / 6 // 30도
    
    val lineAngle = atan2(end.y - start.y, end.x - start.x)
    
    val arrowEnd1 = Offset(
        end.x - arrowLength * cos(lineAngle - arrowAngle).toFloat(),
        end.y - arrowLength * sin(lineAngle - arrowAngle).toFloat()
    )
    
    val arrowEnd2 = Offset(
        end.x - arrowLength * cos(lineAngle + arrowAngle).toFloat(),
        end.y - arrowLength * sin(lineAngle + arrowAngle).toFloat()
    )
    
    drawLine(color = color, start = end, end = arrowEnd1, strokeWidth = strokeWidth)
    drawLine(color = color, start = end, end = arrowEnd2, strokeWidth = strokeWidth)
}

// 확장 함수 추가
@Composable
private fun LazyVerticalGrid(
    columns: androidx.compose.foundation.lazy.grid.GridCells,
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    content: androidx.compose.foundation.lazy.grid.LazyGridScope.() -> Unit
) {
    // KoalaPlot에서 GridCells가 없다면 간단한 Row/Column으로 대체
    Column(
        modifier = modifier,
        verticalArrangement = verticalArrangement
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = horizontalArrangement
        ) {
            NodeType.START.let { nodeType ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(nodeType.color)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "시작점",
                        style = MaterialTheme.typography.caption
                    )
                }
            }
            
            NodeType.WEBSITE.let { nodeType ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(nodeType.color)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "웹사이트",
                        style = MaterialTheme.typography.caption
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = horizontalArrangement
        ) {
            NodeType.PAGE.let { nodeType ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(nodeType.color)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "페이지",
                        style = MaterialTheme.typography.caption
                    )
                }
            }
            
            NodeType.ACTION.let { nodeType ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(nodeType.color)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "액션",
                        style = MaterialTheme.typography.caption
                    )
                }
            }
        }
    }
}