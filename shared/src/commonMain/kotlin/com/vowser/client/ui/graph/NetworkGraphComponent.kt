package com.vowser.client.ui.graph

import androidx.compose.ui.graphics.*

data class GraphNode(
    val id: String,
    val label: String,
    val type: NodeType = NodeType.ACTION,
    val x: Float = 0f,
    val y: Float = 0f
)

data class GraphEdge(
    val from: String,
    val to: String,
    val label: String? = null
)

enum class NodeType(val color: Color, val size: Float) {
    START(Color(0xFFDD896B), 80f),      // 시작 허브 노드 - navigate
    WEBSITE(Color(0xFF5776CD), 70f),    // 웹사이트 허브 - navigate
    ACTION(Color(0xFF8160BD), 35f)      // 클릭 액션 노드 - click
}