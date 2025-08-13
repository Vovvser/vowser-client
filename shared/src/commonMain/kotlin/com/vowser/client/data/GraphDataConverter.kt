package com.vowser.client.data

import com.vowser.client.visualization.GraphVisualizationData
import com.vowser.client.ui.graph.GraphNode
import com.vowser.client.ui.graph.GraphEdge
import com.vowser.client.ui.graph.NodeType
import com.vowser.client.websocket.dto.AllPathsResponse

/**
 * 데이터 변환 유틸리티
 */
object GraphDataConverter {

    /**
     * 서버로부터 받은 모든 경로를 그래프로 변환
     */
    fun convertFromAllPaths(allPaths: AllPathsResponse): GraphVisualizationData {
        val allNodes = mutableMapOf<String, GraphNode>()
        val allEdges = mutableListOf<GraphEdge>()

        allPaths.paths.forEach { path ->
            var currentHub: String? = null
            
            path.steps.forEachIndexed { stepIndex, step ->
                when (step.action) {
                    "navigate" -> {
                        // navigate는 새 페이지 허브 생성
                        val hubId = "hub_${getPageKey(step.url)}"
                        currentHub = hubId
                        
                        allNodes[hubId] = GraphNode(
                            id = hubId,
                            label = step.title,
                            type = NodeType.START
                        )
                    }
                    "click" -> {
                        // click은 작은 액션 노드로 생성
                        val actionNodeId = "action_${path.pathId}_$stepIndex"
                        
                        allNodes[actionNodeId] = GraphNode(
                            id = actionNodeId,
                            label = step.title,
                            type = NodeType.ACTION
                        )
                        
                        if (currentHub != null) {
                            // 현재 허브에서 액션으로 방사형 연결
                            allEdges.add(
                                GraphEdge(
                                    from = currentHub!!,
                                    to = actionNodeId,
                                    label = "click"
                                )
                            )
                        }
                        
                        // 클릭으로 새 페이지 영역으로 이동하는지 확인
                        val nextStep = if (stepIndex < path.steps.size - 1) path.steps[stepIndex + 1] else null
                        if (nextStep != null && isDifferentPage(step.url, nextStep.url)) {
                            // 새 허브 생성
                            val newHubId = "hub_${getPageKey(nextStep.url)}"
                            
                            if (!allNodes.containsKey(newHubId)) {
                                allNodes[newHubId] = GraphNode(
                                    id = newHubId,
                                    label = getPageTitle(nextStep.url),
                                    type = NodeType.WEBSITE
                                )
                            }
                            
                            // 액션에서 새 허브로 navigate 연결
                            allEdges.add(
                                GraphEdge(
                                    from = actionNodeId,
                                    to = newHubId,
                                    label = "navigate"
                                )
                            )
                            
                            currentHub = newHubId
                        }
                    }
                }
            }
        }

        // 날씨까지의 경로만 하이라이트
        val bestPath = allPaths.paths.maxByOrNull { it.score ?: 0.0 }
        val highlightedPath = if (bestPath != null) {
            val weatherActionIndex = bestPath.steps.indexOfFirst { it.title == "날씨" }
            if (weatherActionIndex >= 0) {
                val naverHub = "hub_naver"
                val weatherAction = "action_${bestPath.pathId}_$weatherActionIndex"
                val weatherHub = "hub_weather"
                listOf(naverHub, weatherAction, weatherHub)
            } else emptyList()
        } else emptyList()

        return GraphVisualizationData(
            nodes = allNodes.values.toList(),
            edges = allEdges,
            highlightedPath = highlightedPath,
            activeNodeId = "hub_naver"
        )
    }
    
    /**
     * 두 URL이 다른 페이지/도메인인지 확인
     */
    private fun isDifferentPage(currentUrl: String, nextUrl: String): Boolean {
        val currentDomain = extractDomain(currentUrl)
        val nextDomain = extractDomain(nextUrl)
        
        // 도메인이 다르거나 서브도메인이 다르면 다른 페이지로 판단
        return currentDomain != nextDomain
    }
    
    /**
     * URL에서 도메인/서브도메인 추출
     */
    private fun extractDomain(url: String): String {
        return try {
            val domain = url.substringAfter("://").substringBefore("/")
            // 서브도메인까지 포함하여 구분 (예: www.naver.com vs weather.naver.com)
            domain.lowercase()
        } catch (e: Exception) {
            url
        }
    }
    
    /**
     * URL에서 페이지 키 추출 (동적)
     */
    private fun getPageKey(url: String): String {
        val domain = extractDomain(url)
        return domain.replace(".", "_").replace("-", "_")
    }
    
    /**
     * URL에서 페이지 제목 추출 (동적)
     */
    private fun getPageTitle(url: String): String {
        val domain = extractDomain(url)
        return when {
            domain.contains("naver.com") -> {
                if (domain.contains("weather")) "날씨" else "NAVER"
            }
            else -> {
                // 도메인명을 깔끔하게 변환
                domain.substringBefore(".")
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
        }
    }
}