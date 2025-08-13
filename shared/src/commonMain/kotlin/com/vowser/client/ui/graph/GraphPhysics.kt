package com.vowser.client.ui.graph

import androidx.compose.ui.geometry.Size
import kotlin.math.*

/**
 * 그래프 물리학 기반 레이아웃 함수들
 */

//레이아웃 (큰 허브, 작은 액션 노드들을 원형 배치)
fun layoutNodesWithPhysics(
    nodes: List<GraphNode>,
    edges: List<GraphEdge>,
    canvasSize: Size
): List<GraphNode> {
    if (nodes.isEmpty()) return nodes
    
    // 허브-스포크 구조 감지
    val hubNodes = nodes.filter { it.type == NodeType.START || it.type == NodeType.WEBSITE }
    val actionNodes = nodes.filter { it.type == NodeType.ACTION }
    
    if (hubNodes.isEmpty()) {
        return layoutNodesHierarchically(nodes, canvasSize)
    }
    
    val result = mutableListOf<GraphNode>()
    val parentChildMap = mutableMapOf<String, MutableList<String>>()
    edges.forEach { edge ->
        parentChildMap.getOrPut(edge.from) { mutableListOf() }.add(edge.to)
    }
    
    // 허브 노드들을 적절히 배치
    val hubSpacingX = canvasSize.width / (hubNodes.size + 1)
    hubNodes.forEachIndexed { hubIndex, hubNode ->
        val hubX = hubSpacingX * (hubIndex + 1)
        val hubY = canvasSize.height / 3f
        
        result.add(hubNode.copy(x = hubX, y = hubY))
        
        // 이 허브에 연결된 액션 노드들을 원형으로 배치
        val connectedActions = parentChildMap[hubNode.id]?.let { childIds ->
            actionNodes.filter { it.id in childIds }
        } ?: emptyList()
        
        if (connectedActions.isNotEmpty()) {
            val circleRadius = 180f  // 짧은 간선 길이
            
            connectedActions.forEachIndexed { index, actionNode ->
                val angle = (2 * PI * index / connectedActions.size)
                val actionX = hubX + cos(angle).toFloat() * circleRadius
                val actionY = hubY + sin(angle).toFloat() * circleRadius
                
                // 캔버스 경계 체크
                val clampedX = actionX.coerceIn(80f, canvasSize.width - 80f)
                val clampedY = actionY.coerceIn(80f, canvasSize.height - 80f)
                
                result.add(actionNode.copy(x = clampedX, y = clampedY))
            }
        }
    }
    
    // 처리되지 않은 노드들 추가 (navigate 연결 등)
    val processedIds = result.map { it.id }.toSet()
    val remainingNodes = nodes.filter { it.id !in processedIds }
    
    remainingNodes.forEach { node ->
        // 연결된 허브 찾기
        val connectedHub = edges.find { it.to == node.id }?.from?.let { fromId ->
            result.find { it.id == fromId }
        }
        
        if (connectedHub != null) {
            // 허브 오른쪽에 배치 (navigate 연결용)
            val nodeX = (connectedHub.x + 250f).coerceIn(80f, canvasSize.width - 80f)
            val nodeY = connectedHub.y
            result.add(node.copy(x = nodeX, y = nodeY))
        } else {
            // 기본 위치
            val nodeX = canvasSize.width / 2f
            val nodeY = canvasSize.height * 2f / 3f
            result.add(node.copy(x = nodeX, y = nodeY))
        }
    }
    
    return result
}


/**
 * 계층적 레이아웃
 * 노드 타입에 따른 계층 구조로 배치
 */
fun layoutNodesHierarchically(
    nodes: List<GraphNode>,
    canvasSize: Size
): List<GraphNode> {
    if (nodes.isEmpty()) return nodes
    
    // 노드 타입별 우선순위 정의
    val typeOrder = mapOf(
        NodeType.START to 0,
        NodeType.WEBSITE to 1,
        NodeType.ACTION to 3,
    )
    
    val nodesByLevel = nodes.groupBy { typeOrder[it.type] ?: 4 }
    val maxLevel = nodesByLevel.keys.maxOrNull() ?: 0
    
    val result = mutableListOf<GraphNode>()
    val levelHeight = canvasSize.height / (maxLevel + 2)
    
    nodesByLevel.forEach { (level, nodesInLevel) ->
        val y = levelHeight * (level + 1)
        val nodeSpacing = canvasSize.width / (nodesInLevel.size + 1)
        
        nodesInLevel.forEachIndexed { index, node ->
            val x = nodeSpacing * (index + 1)
            result.add(node.copy(x = x, y = y))
        }
    }
    
    return result
}