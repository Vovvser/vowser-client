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
    NAVIGATE(Color(0xFF2196F3), 70f),   // 이동 - 파란색
    CLICK(Color(0xFF4CAF50), 60f),      // 클릭 - 녹색
    INPUT(Color(0xFFFF9800), 60f),      // 입력 - 주황색
    WAIT(Color(0xFF9C27B0), 60f),       // 대기 - 보라색
    START(Color(0xFFDD896B), 80f),      // 시작 노드
    WEBSITE(Color(0xFF5776CD), 70f),    // 웹사이트 노드
    ACTION(Color(0xFF8160BD), 35f)      // 액션 노드
}