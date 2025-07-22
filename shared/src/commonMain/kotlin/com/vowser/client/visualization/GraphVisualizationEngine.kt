package com.vowser.client.visualization

import com.vowser.client.data.*
import com.vowser.client.ui.graph.*

/**
 * 그래프 시각화 엔진 인터페이스
 * D3.js와 Kotlin Compose 구현을 쉽게 교체할 수 있도록 추상화
 */
interface GraphVisualizationEngine {
    /**
     * 웹 탐색 그래프를 시각화용 노드/엣지로 변환
     */
    fun convertToVisualizationData(graph: WebNavigationGraph): GraphVisualizationData
    
    /**
     * 특정 경로를 하이라이트하여 시각화
     */
    fun highlightPath(
        graph: WebNavigationGraph, 
        path: List<WebRelationship>
    ): GraphVisualizationData
    
    /**
     * 현재 활성 노드를 표시
     */
    fun setActiveNode(nodeId: String)
    
    /**
     * 그래프 레이아웃 타입 설정
     */
    fun setLayoutType(layoutType: GraphLayoutType)
}

/**
 * 그래프 레이아웃 타입
 */
enum class GraphLayoutType {
    CIRCULAR,      // 원형 배치 (현재 구현)
    HIERARCHICAL,  // 계층형 배치 (depth 기반)
    FORCE_DIRECTED, // 힘-지향 배치 (D3.js 스타일)
    TREE           // 트리 구조 배치
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

/**
 * Kotlin Compose 기반 시각화 엔진 구현
 */
class ComposeVisualizationEngine : GraphVisualizationEngine {
    
    private var activeNodeId: String? = null
    private var layoutType: GraphLayoutType = GraphLayoutType.HIERARCHICAL
    
    override fun convertToVisualizationData(graph: WebNavigationGraph): GraphVisualizationData {
        val nodes = graph.getAllNodes().map { webNode ->
            GraphNode(
                id = webNode.id,
                label = webNode.name,
                type = mapWebNodeTypeToGraphNodeType(webNode.type),
                // 위치는 레이아웃 타입에 따라 calculateNodePosition에서 결정됨
            )
        }
        
        val edges = graph.getAllRelationships().map { relationship ->
            GraphEdge(
                from = relationship.fromNodeId,
                to = relationship.toNodeId,
                label = relationship.description
            )
        }
        
        return GraphVisualizationData(
            nodes = nodes,
            edges = edges,
            activeNodeId = activeNodeId,
            layoutType = layoutType
        )
    }
    
    override fun highlightPath(
        graph: WebNavigationGraph, 
        path: List<WebRelationship>
    ): GraphVisualizationData {
        val visualizationData = convertToVisualizationData(graph)
        val pathNodeIds = mutableSetOf<String>()
        
        // 경로에 포함된 모든 노드 ID 수집
        path.forEach { relationship ->
            pathNodeIds.add(relationship.fromNodeId)
            pathNodeIds.add(relationship.toNodeId)
        }
        
        return visualizationData.copy(
            highlightedPath = pathNodeIds.toList()
        )
    }
    
    override fun setActiveNode(nodeId: String) {
        activeNodeId = nodeId
    }
    
    override fun setLayoutType(layoutType: GraphLayoutType) {
        this.layoutType = layoutType
    }
    
    /**
     * 웹 노드 타입을 그래프 노드 타입으로 매핑
     */
    private fun mapWebNodeTypeToGraphNodeType(webNodeType: WebNodeType): NodeType {
        return when (webNodeType) {
            WebNodeType.ROOT -> NodeType.START
            WebNodeType.WEBSITE -> NodeType.WEBSITE
            WebNodeType.CATEGORY -> NodeType.PAGE
            WebNodeType.CONTENT -> NodeType.ACTION
        }
    }
}

/**
 * D3.js 기반 시각화 엔진 구현 (Future Implementation)
 * WebView를 통해 D3.js와 통신하는 구현체
 */
class D3VisualizationEngine : GraphVisualizationEngine {
    // TODO: WebView를 통한 D3.js 통신 구현
    
    override fun convertToVisualizationData(graph: WebNavigationGraph): GraphVisualizationData {
        // D3.js로 데이터 전송 및 시각화
        throw NotImplementedError("D3.js integration not implemented yet")
    }
    
    override fun highlightPath(
        graph: WebNavigationGraph, 
        path: List<WebRelationship>
    ): GraphVisualizationData {
        throw NotImplementedError("D3.js integration not implemented yet")
    }
    
    override fun setActiveNode(nodeId: String) {
        // D3.js에 노드 활성화 신호 전송
    }
    
    override fun setLayoutType(layoutType: GraphLayoutType) {
        // D3.js에 레이아웃 변경 신호 전송
    }
}

/**
 * 시각화 엔진 팩토리
 */
object GraphVisualizationEngineFactory {
    
    enum class EngineType {
        COMPOSE,  // Kotlin Compose 기반
        D3JS      // D3.js 기반 (WebView)
    }
    
    fun createEngine(type: EngineType): GraphVisualizationEngine {
        return when (type) {
            EngineType.COMPOSE -> ComposeVisualizationEngine()
            EngineType.D3JS -> D3VisualizationEngine()
        }
    }
}

/**
 * 그래프 레이아웃 계산기
 */
object GraphLayoutCalculator {
    
    /**
     * 레이아웃 타입에 따른 노드 위치 계산
     */
    fun calculateNodePositions(
        nodes: List<GraphNode>,
        relationships: List<WebRelationship>,
        layoutType: GraphLayoutType,
        canvasWidth: Float,
        canvasHeight: Float
    ): List<GraphNode> {
        return when (layoutType) {
            GraphLayoutType.CIRCULAR -> calculateCircularLayout(nodes, canvasWidth, canvasHeight)
            GraphLayoutType.HIERARCHICAL -> calculateHierarchicalLayout(nodes, relationships, canvasWidth, canvasHeight)
            GraphLayoutType.FORCE_DIRECTED -> calculateForceDirectedLayout(nodes, relationships, canvasWidth, canvasHeight)
            GraphLayoutType.TREE -> calculateTreeLayout(nodes, relationships, canvasWidth, canvasHeight)
        }
    }
    
    private fun calculateCircularLayout(
        nodes: List<GraphNode>,
        canvasWidth: Float,
        canvasHeight: Float
    ): List<GraphNode> {
        if (nodes.isEmpty()) return nodes
        
        val centerX = canvasWidth / 2f
        val centerY = canvasHeight / 2f
        val radius = minOf(canvasWidth, canvasHeight) * 0.3f
        
        return nodes.mapIndexed { index, node ->
            val angle = (2 * kotlin.math.PI * index / nodes.size).toFloat()
            val x = centerX + radius * kotlin.math.cos(angle.toDouble()).toFloat()
            val y = centerY + radius * kotlin.math.sin(angle.toDouble()).toFloat()
            
            node.copy(x = x, y = y)
        }
    }
    
    private fun calculateHierarchicalLayout(
        nodes: List<GraphNode>,
        relationships: List<WebRelationship>,
        canvasWidth: Float,
        canvasHeight: Float
    ): List<GraphNode> {
        if (nodes.isEmpty()) return nodes
        
        // 노드를 타입(depth)별로 그룹화
        val nodesByType = nodes.groupBy { it.type }
        val layers = listOf(
            NodeType.START,
            NodeType.WEBSITE,
            NodeType.PAGE,
            NodeType.ACTION
        )
        
        val layerHeight = canvasHeight / (layers.size + 1)
        val result = mutableListOf<GraphNode>()
        
        layers.forEachIndexed { layerIndex, nodeType ->
            val nodesInLayer = nodesByType[nodeType] ?: emptyList()
            if (nodesInLayer.isNotEmpty()) {
                val y = layerHeight * (layerIndex + 1)
                val nodeWidth = canvasWidth / (nodesInLayer.size + 1)
                
                nodesInLayer.forEachIndexed { nodeIndex, node ->
                    val x = nodeWidth * (nodeIndex + 1)
                    result.add(node.copy(x = x, y = y))
                }
            }
        }
        
        return result
    }
    
    private fun calculateForceDirectedLayout(
        nodes: List<GraphNode>,
        relationships: List<WebRelationship>,
        canvasWidth: Float,
        canvasHeight: Float
    ): List<GraphNode> {
        // 간단한 힘-지향 레이아웃 시뮬레이션
        // 실제 구현에서는 더 정교한 물리 시뮬레이션 필요
        return calculateCircularLayout(nodes, canvasWidth, canvasHeight)
    }
    
    private fun calculateTreeLayout(
        nodes: List<GraphNode>,
        relationships: List<WebRelationship>,
        canvasWidth: Float,
        canvasHeight: Float
    ): List<GraphNode> {
        // 트리 레이아웃 (좌측에서 우측으로 확장)
        return calculateHierarchicalLayout(nodes, relationships, canvasWidth, canvasHeight)
    }
}