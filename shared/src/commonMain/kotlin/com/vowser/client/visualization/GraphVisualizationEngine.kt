package com.vowser.client.visualization

import com.vowser.client.ui.graph.*
/**
 * 그래프 레이아웃 타입
 */
enum class GraphLayoutType {
    CIRCULAR,
}

/**
 * 시각화 데이터 컨테이너
 */
data class GraphVisualizationData(
    val nodes: List<GraphNode>,
    val edges: List<GraphEdge>,
    val activeNodeId: String? = null,
    val highlightedPath: List<String> = emptyList(),
    val layoutType: GraphLayoutType = GraphLayoutType.CIRCULAR,
    val searchInfo: SearchInfo? = null
)

/**
 * 검색 정보
 */
data class SearchInfo(
    val query: String,
    val totalPaths: Int,
    val searchTimeMs: Long,
    val topRelevance: Float? = null
)