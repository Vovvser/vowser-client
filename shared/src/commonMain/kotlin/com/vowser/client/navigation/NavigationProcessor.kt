package com.vowser.client.navigation

import com.vowser.client.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 메인 탐색 처리 엔진
 * 음성 명령을 받아 웹 탐색 경로를 찾고 시각화 데이터를 생성
 */
class NavigationProcessor(
    private val graph: WebNavigationGraph
) {
    private val _graphStatistics = MutableStateFlow(calculateGraphStatistics())
    val graphStatistics: StateFlow<GraphStatistics> = _graphStatistics.asStateFlow()

    /**
     * 그래프 통계 정보 반환
     */
    private fun calculateGraphStatistics(): GraphStatistics {
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

    /**
     * 그래프 통계 정보를 업데이트합니다.
     * WebNavigationGraph가 변경될 때 호출되어야 합니다.
     */
    fun updateGraphStatistics() {
        _graphStatistics.value = calculateGraphStatistics()
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