package com.vowser.client.visualization

import com.vowser.client.ui.graph.*
/**
 * 그래프 레이아웃 타입
 */
enum class GraphLayoutType {
    CIRCULAR,      // 원형 배치
}

/**
 * 시각화 데이터 컨테이너
 */
data class GraphVisualizationData(
    val nodes: List<GraphNode>,
    val edges: List<GraphEdge>,
    val activeNodeId: String? = null,
    val highlightedPath: List<String> = emptyList(),
    val layoutType: GraphLayoutType = GraphLayoutType.CIRCULAR
)