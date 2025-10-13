package com.vowser.client.ui.graph

import androidx.compose.ui.geometry.Size

/**
 * 그래프 물리학 기반 레이아웃 함수들
 */

//레이아웃
fun layoutNodesWithPhysics(
    nodes: List<GraphNode>,
    edges: List<GraphEdge>,
    canvasSize: Size
): List<GraphNode> {
    if (nodes.isEmpty()) return nodes

    return layoutNodesHorizontally(nodes, canvasSize)
}

/**
 * 가로 일렬 레이아웃
 */
fun layoutNodesHorizontally(
    nodes: List<GraphNode>,
    canvasSize: Size
): List<GraphNode> {
    if (nodes.isEmpty()) return nodes

    val result = mutableListOf<GraphNode>()
    val centerY = canvasSize.height / 2f
    val nodeSpacing = 200f // 노드 간 간격
    val startX = 150f // 시작 X 위치

    nodes.forEachIndexed { index, node ->
        val x = startX + (index * nodeSpacing)
        result.add(node.copy(x = x, y = centerY))
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