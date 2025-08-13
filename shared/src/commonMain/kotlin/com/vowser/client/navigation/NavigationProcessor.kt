package com.vowser.client.navigation

import com.vowser.client.data.*

/**
 * 메인 탐색 처리 엔진
 * 음성 명령을 받아 웹 탐색 경로를 찾고 시각화 데이터를 생성
 */
class NavigationProcessor(
    private val graph: WebNavigationGraph
) {
    /**
     * 그래프 통계 정보 반환
     */
    fun getGraphStatistics(): GraphStatistics {
        val nodes = graph.getAllNodes()
        val relationships = graph.getAllRelationships()
        
        return GraphStatistics(
            totalNodes = nodes.size,
            totalRelationships = relationships.size,
            nodesByType = nodes.groupBy { it.type }.mapValues { it.value.size },
            averageClicks = relationships.map { it.requiredClicks }.average(),
            averageTime = relationships.map { it.estimatedTime }.average()
        )
    }
}

/**
 * 그래프 통계 정보
 */
data class GraphStatistics(
    val totalNodes: Int,
    val totalRelationships: Int,
    val nodesByType: Map<WebNodeType, Int>,
    val averageClicks: Double,
    val averageTime: Double
)