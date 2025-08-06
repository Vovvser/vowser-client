package com.vowser.client.data

import com.vowser.client.websocket.dto.GraphUpdateData
import com.vowser.client.websocket.dto.GraphNode as WsGraphNode
import com.vowser.client.websocket.dto.GraphEdge as WsGraphEdge
import com.vowser.client.websocket.dto.GraphNodeType as WsGraphNodeType
import com.vowser.client.visualization.GraphVisualizationData
import com.vowser.client.ui.graph.GraphNode
import com.vowser.client.ui.graph.GraphEdge
import com.vowser.client.ui.graph.NodeType

/**
 * WebSocket에서 받은 그래프 데이터를 기존 시각화 시스템용으로 변환
 */
object GraphDataConverter {
    
    /**
     * GraphUpdateData를 GraphVisualizationData로 변환
     */
    fun convertToVisualizationData(graphUpdate: GraphUpdateData): GraphVisualizationData {
        val nodes = graphUpdate.nodes.map { convertNode(it) }
        val edges = graphUpdate.edges.map { convertEdge(it) }
        
        return GraphVisualizationData(
            nodes = nodes,
            edges = edges,
            highlightedPath = graphUpdate.highlightedPath,
            activeNodeId = graphUpdate.activeNodeId
        )
    }
    
    /**
     * WebSocket 노드를 시각화용 GraphNode로 변환
     */
    private fun convertNode(wsNode: WsGraphNode): GraphNode {
        return GraphNode(
            id = wsNode.id,
            label = wsNode.label,
            type = convertNodeType(wsNode.type),
            x = wsNode.position?.x?.toFloat() ?: 0f,
            y = wsNode.position?.y?.toFloat() ?: 0f
        )
    }
    
    /**
     * WebSocket 엣지를 시각화용 GraphEdge로 변환
     */
    private fun convertEdge(wsEdge: WsGraphEdge): GraphEdge {
        return GraphEdge(
            from = wsEdge.source,
            to = wsEdge.target,
            label = wsEdge.label
        )
    }
    
    /**
     * WebSocket 노드 타입을 NodeType으로 변환
     */
    private fun convertNodeType(wsType: WsGraphNodeType): NodeType {
        return when (wsType) {
            WsGraphNodeType.ROOT -> NodeType.START
            WsGraphNodeType.WEBSITE -> NodeType.WEBSITE
            WsGraphNodeType.CATEGORY -> NodeType.PAGE
            WsGraphNodeType.PAGE -> NodeType.PAGE
            WsGraphNodeType.ACTION -> NodeType.ACTION
            WsGraphNodeType.VOICE_START -> NodeType.START // 음성 시작은 START로 처리
            WsGraphNodeType.RESULT -> NodeType.ACTION // 결과는 ACTION으로 처리
        }
    }
}
