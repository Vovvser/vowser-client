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

        val bestPath = allPaths.paths.maxByOrNull { it.score ?: 0.0 }
        val highlightedPath = if (bestPath != null) {
            // 1. 시작 허브 찾기
            val startStep = bestPath.steps.firstOrNull { it.action == "navigate" }
            if (startStep != null) {
                val startHubId = "hub_${getPageKey(startStep.url)}"
                
                // 2. 첫 번째 웹사이트 전환을 일으키는 클릭 찾기
                var targetActionNodeId: String? = null
                var targetWebsiteHubId: String? = null
                
                bestPath.steps.forEachIndexed { stepIndex, step ->
                    if (step.action == "click" && targetActionNodeId == null) {
                        val nextStep = if (stepIndex < bestPath.steps.size - 1) bestPath.steps[stepIndex + 1] else null
                        if (nextStep != null && isDifferentPage(startStep.url, nextStep.url)) {
                            targetActionNodeId = "action_${bestPath.pathId}_$stepIndex"
                            targetWebsiteHubId = "hub_${getPageKey(nextStep.url)}"
                        }
                    }
                }
                
                // 3. 경로 구성: 시작허브 → 액션노드 → 새웹사이트허브
                val actionId = targetActionNodeId
                val websiteId = targetWebsiteHubId
                if (actionId != null && websiteId != null) {
                    listOf(startHubId, actionId, websiteId)
                } else {
                    emptyList()
                }
            } else emptyList()
        } else emptyList()
        
        println("[GraphDataConverter] highlightedPath: $highlightedPath")
        println("[GraphDataConverter] Total nodes: ${allNodes.size}, Total edges: ${allEdges.size}")
        allNodes.values.forEach { node ->
            println("[GraphDataConverter] Node: ${node.id} (${node.label})")
        }

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
        
        return currentDomain != nextDomain
    }
    
    /**
     * URL에서 도메인/서브도메인 추출
     */
    private fun extractDomain(url: String): String {
        return try {
            val domain = url.substringAfter("://").substringBefore("/")
            domain.lowercase()
        } catch (_: Exception) {
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
                domain.substringBefore(".")
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
        }
    }
}