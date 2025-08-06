package com.vowser.client.data

import com.vowser.client.websocket.dto.GraphUpdateData
import com.vowser.client.websocket.dto.GraphNode as WsGraphNode
import com.vowser.client.websocket.dto.GraphEdge as WsGraphEdge
import com.vowser.client.websocket.dto.GraphNodeType as WsGraphNodeType
import com.vowser.client.visualization.GraphVisualizationData
import com.vowser.client.ui.graph.GraphNode
import com.vowser.client.ui.graph.GraphEdge
import com.vowser.client.ui.graph.NodeType
import com.vowser.client.websocket.dto.NavigationPath

/**
 * 데이터 변환 유틸리티
 */
object GraphDataConverter {

    /**
     * NavigationPath를 시각화용 데이터로 변환하는 함수
     */
    fun convertFromNavigationPath(path: NavigationPath): GraphVisualizationData {
        val nodes = mutableListOf<GraphNode>()
        val edges = mutableListOf<GraphEdge>()

        path.steps.forEachIndexed { index, step ->
            val nodeId = "step_${path.pathId}_$index"
            nodes.add(
                GraphNode(
                    id = nodeId,
                    label = step.title,
                    type = when {
                        index == 0 -> NodeType.START
                        step.action == "navigate" -> NodeType.WEBSITE
                        step.action == "click" -> NodeType.ACTION
                        else -> NodeType.PAGE
                    }
                )
            )

            if (index > 0) {
                val previousNodeId = "step_${path.pathId}_${index - 1}"
                edges.add(
                    GraphEdge(
                        from = previousNodeId,
                        to = nodeId,
                        label = step.action
                    )
                )
            }
        }

        return GraphVisualizationData(
            nodes = nodes,
            edges = edges,
            highlightedPath = nodes.map { it.id },
            activeNodeId = nodes.firstOrNull()?.id
        )
    }

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
            WsGraphNodeType.VOICE_START -> NodeType.START
            WsGraphNodeType.RESULT -> NodeType.ACTION
        }
    }
}