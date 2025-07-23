package com.vowser.client.navigation

import com.vowser.client.data.*
import com.vowser.client.voice.*
import com.vowser.client.visualization.*

/**
 * 탐색 결과 데이터 클래스
 */
data class NavigationResult(
    val success: Boolean,
    val targetNode: WebNode? = null,
    val path: List<WebRelationship> = emptyList(),
    val visualizationData: GraphVisualizationData? = null,
    val message: String = "",
    val estimatedTime: Int = 0, // 초 단위
    val totalClicks: Int = 0,
    val error: NavigationError? = null
)

/**
 * 탐색 오류 타입
 */
enum class NavigationError {
    COMMAND_NOT_FOUND,      // 명령어를 인식할 수 없음
    PATH_NOT_FOUND,         // 목표까지의 경로를 찾을 수 없음
    NODE_NOT_ACCESSIBLE,    // 접근성 문제로 노드에 도달할 수 없음
    MULTIPLE_PATHS_FOUND,   // 여러 경로가 있어 명확하지 않음
    GENERAL_ERROR           // 일반적인 오류
}

/**
 * 메인 탐색 처리 엔진
 * 음성 명령을 받아 웹 탐색 경로를 찾고 시각화 데이터를 생성
 */
class NavigationProcessor(
    private val graph: WebNavigationGraph,
    private val visualizationEngine: GraphVisualizationEngine = 
        GraphVisualizationEngineFactory.createEngine(GraphVisualizationEngineFactory.EngineType.COMPOSE)
) {
    
    /**
     * 음성 명령을 처리하여 탐색 결과 반환
     */
    suspend fun processVoiceCommand(command: String): NavigationResult {
        try {
            // 1. 음성 명령어 인식 및 매칭 (확장된 명령어 세트 우선 시도)
            val voiceCommand = ExpandedVoiceCommands.findMatchingExpandedCommand(command)
                ?: AccessibilityVoiceCommands.findMatchingCommand(command)
                ?: return NavigationResult(
                    success = false,
                    message = "죄송합니다. '$command' 명령을 인식할 수 없습니다.\n\n사용 가능한 명령어 예시:\n${getHelpMessage()}",
                    error = NavigationError.COMMAND_NOT_FOUND
                )
            
            // 2. 목표 노드 찾기
            val targetNodeId = voiceCommand.expectedPath.lastOrNull()
                ?: return NavigationResult(
                    success = false,
                    message = "명령어 처리 중 오류가 발생했습니다.",
                    error = NavigationError.GENERAL_ERROR
                )
            
            val targetNode = graph.getNode(targetNodeId)
                ?: return NavigationResult(
                    success = false,
                    message = "요청하신 콘텐츠를 찾을 수 없습니다.",
                    error = NavigationError.PATH_NOT_FOUND
                )
            
            // 3. 경로 찾기
            val path = findOptimalPath(voiceCommand.expectedPath)
                ?: return NavigationResult(
                    success = false,
                    message = "${targetNode.name}까지의 경로를 찾을 수 없습니다.",
                    error = NavigationError.PATH_NOT_FOUND
                )
            
            // 4. 접근성 검사
            val accessibilityCheck = checkAccessibility(targetNode, path)
            if (!accessibilityCheck.accessible) {
                return NavigationResult(
                    success = false,
                    message = accessibilityCheck.message,
                    error = NavigationError.NODE_NOT_ACCESSIBLE
                )
            }
            
            // 5. 시각화 데이터 생성
            val visualizationData = visualizationEngine.highlightPath(graph, path)
            visualizationEngine.setActiveNode(targetNodeId)
            
            // 6. 결과 생성
            val totalTime = path.sumOf { it.estimatedTime }
            val totalClicks = path.sumOf { it.requiredClicks }
            
            return NavigationResult(
                success = true,
                targetNode = targetNode,
                path = path,
                visualizationData = visualizationData,
                message = createSuccessMessage(voiceCommand, targetNode, path, totalTime, totalClicks),
                estimatedTime = totalTime,
                totalClicks = totalClicks
            )
            
        } catch (e: Exception) {
            return NavigationResult(
                success = false,
                message = "처리 중 예상치 못한 오류가 발생했습니다: ${e.message}",
                error = NavigationError.GENERAL_ERROR
            )
        }
    }
    
    /**
     * 최적 경로 찾기
     */
    private fun findOptimalPath(expectedPath: List<String>): List<WebRelationship>? {
        if (expectedPath.size < 2) return emptyList()
        
        val path = mutableListOf<WebRelationship>()
        
        for (i in 0 until expectedPath.size - 1) {
            val fromId = expectedPath[i]
            val toId = expectedPath[i + 1]
            
            // 직접 관계 찾기
            val relationship = graph.getRelationshipsFrom(fromId)
                .firstOrNull { it.toNodeId == toId }
                ?: return null
            
            path.add(relationship)
        }
        
        return path
    }
    
    /**
     * 접근성 검사
     */
    private fun checkAccessibility(
        targetNode: WebNode,
        path: List<WebRelationship>
    ): AccessibilityCheckResult {
        
        val accessibility = targetNode.accessibility
            ?: return AccessibilityCheckResult(true)
        
        // 접근성 난이도 체크
        when (accessibility.difficulty) {
            AccessibilityDifficulty.HARD -> {
                if (path.sumOf { it.requiredClicks } > 5) {
                    return AccessibilityCheckResult(
                        accessible = false,
                        message = "${targetNode.name}에 접근하기 위해서는 많은 클릭이 필요합니다 (${path.sumOf { it.requiredClicks }}번). 접근성 지원이 제한적일 수 있습니다."
                    )
                }
            }
            else -> { /* 다른 난이도는 접근 허용 */ }
        }
        
        return AccessibilityCheckResult(true)
    }
    
    private data class AccessibilityCheckResult(
        val accessible: Boolean,
        val message: String = ""
    )
    
    /**
     * 성공 메시지 생성
     */
    private fun createSuccessMessage(
        voiceCommand: VoiceCommand,
        targetNode: WebNode,
        path: List<WebRelationship>,
        totalTime: Int,
        totalClicks: Int
    ): String {
        val steps = path.map { "${it.actionType.displayName}: ${it.description}" }
        
        return buildString {
            appendLine("✅ 명령어 인식: \"${voiceCommand.command}\"")
            appendLine("🎯 목적지: ${targetNode.name}")
            if (targetNode.description != null) {
                appendLine("📋 설명: ${targetNode.description}")
            }
            appendLine()
            appendLine("🗺️ 탐색 경로:")
            steps.forEachIndexed { index, step ->
                appendLine("  ${index + 1}. $step")
            }
            appendLine()
            appendLine("⏱️ 예상 소요 시간: ${totalTime}초")
            appendLine("🖱️ 필요한 클릭 수: ${totalClicks}번")
            
            if (voiceCommand.difficulty == CommandDifficulty.EASY) {
                appendLine("✨ 쉬운 접근: 빠르게 도달할 수 있습니다!")
            } else if (voiceCommand.difficulty == CommandDifficulty.HARD) {
                appendLine("⚠️ 복잡한 경로: 시간이 다소 걸릴 수 있습니다.")
            }
        }
    }
    
    /**
     * 도움말 메시지 생성
     */
    private fun getHelpMessage(): String {
        val examples = AccessibilityVoiceCommands.getCommandExamples().take(5)
        return examples.joinToString("\n") { "• $it" }
    }
    
    /**
     * 현재 그래프 상태 반환
     */
    fun getCurrentVisualizationData(): GraphVisualizationData {
        return visualizationEngine.convertToVisualizationData(graph)
    }
    
    /**
     * 사용 가능한 모든 명령어 반환
     */
    fun getAvailableCommands(): List<VoiceCommand> {
        return AccessibilityVoiceCommands.getCommandSet()
    }
    
    /**
     * 특정 노드의 상세 정보 반환
     */
    fun getNodeDetails(nodeId: String): WebNode? {
        return graph.getNode(nodeId)
    }
    
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