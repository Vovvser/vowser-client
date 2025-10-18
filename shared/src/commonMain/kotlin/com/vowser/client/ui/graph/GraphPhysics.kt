package com.vowser.client.ui.graph

import androidx.compose.ui.geometry.Size

/**
 * 그래프 물리학 기반 레이아웃 함수들
 */

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
    val nodeSpacing = 200f
    val startX = 150f

    nodes.forEachIndexed { index, node ->
        val x = startX + (index * nodeSpacing)
        result.add(node.copy(x = x, y = centerY))
    }

    return result
}