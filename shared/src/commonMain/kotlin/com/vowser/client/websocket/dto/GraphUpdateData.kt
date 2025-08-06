package com.vowser.client.websocket.dto

import kotlinx.serialization.Serializable

/**
 * 그래프 업데이트 데이터
 * vowser-mcp-server에서 생성된 실시간 그래프 데이터
 */
@Serializable
data class GraphUpdateData(
    val sessionId: String,
    val nodes: List<GraphNode>,
    val edges: List<GraphEdge>,
    val highlightedPath: List<String> = emptyList(),
    val activeNodeId: String? = null,
    val metadata: GraphMetadata? = null
)

/**
 * 그래프 노드 (Neo4j의 노드를 클라이언트용으로 변환)
 */
@Serializable
data class GraphNode(
    val id: String,
    val label: String,
    val type: GraphNodeType,
    val url: String? = null,
    val description: String? = null,
    val keywords: List<String> = emptyList(),
    val position: NodePosition? = null,
    val accessibility: AccessibilityData? = null
)

/**
 * 그래프 엣지 (Neo4j의 관계를 클라이언트용으로 변환)
 */
@Serializable
data class GraphEdge(
    val id: String,
    val source: String,
    val target: String,
    val type: GraphEdgeType,
    val weight: Double = 1.0,
    val label: String? = null
)

/**
 * 노드 타입
 */
@Serializable
enum class GraphNodeType {
    ROOT,           // 도메인 루트
    WEBSITE,        // 메인 웹사이트
    CATEGORY,       // 카테고리 (뉴스, 쇼핑 등)
    PAGE,           // 개별 페이지
    ACTION,         // 사용자 액션
    VOICE_START,    // 음성 명령 시작점
    RESULT          // 결과/완료 상태
}

/**
 * 엣지 타입
 */
@Serializable
enum class GraphEdgeType {
    HAS_PAGE,                    // ROOT → PAGE
    NAVIGATES_TO,               // 같은 도메인 내 이동
    NAVIGATES_TO_CROSS_DOMAIN,  // 다른 도메인으로 이동
    CONTAINS,                   // 포함 관계
    SIMILAR_TO,                 // 유사성 관계
    EXECUTES,                   // 명령 실행
    LEADS_TO                    // 결과로 이어짐
}

/**
 * 노드 위치 정보 (시각화용)
 */
@Serializable
data class NodePosition(
    val x: Double,
    val y: Double,
    val z: Double = 0.0
)

/**
 * 접근성 데이터
 */
@Serializable
data class AccessibilityData(
    val hasKeyboardNavigation: Boolean = false,
    val hasScreenReaderSupport: Boolean = false,
    val hasVoiceControl: Boolean = false,
    val difficulty: AccessibilityLevel = AccessibilityLevel.MEDIUM
)

@Serializable
enum class AccessibilityLevel {
    EASY, MEDIUM, HARD
}

/**
 * 그래프 메타데이터
 */
@Serializable
data class GraphMetadata(
    val totalNodes: Int? = null,
    val totalEdges: Int? = null,
    val currentDomain: String? = null,
    val voiceCommand: String? = null,
    val timestamp: String? = null,
    val processingTime: Int? = null
)