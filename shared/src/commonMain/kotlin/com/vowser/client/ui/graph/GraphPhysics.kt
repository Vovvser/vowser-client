package com.vowser.client.ui.graph

import androidx.compose.ui.geometry.Size
import kotlin.math.*

/**
 * 그래프 물리학 기반 레이아웃 함수들
 */

// 물리학 기반 노드 레이아웃
fun layoutNodesWithPhysics(
    nodes: List<GraphNode>,
    edges: List<GraphEdge>,
    canvasSize: Size
): List<GraphNode> {
    if (nodes.isEmpty()) return nodes
    
    // 개선된 계층형 레이아웃 (물리학 시뮬레이션 적용)
    val nodesByType = nodes.groupBy { it.type }
    val layers = listOf(
        NodeType.START,
        NodeType.WEBSITE,
        NodeType.PAGE,
        NodeType.ACTION
    )
    
    val layerHeight = canvasSize.height / (layers.size + 1.5f)
    val result = mutableListOf<GraphNode>()
    
    val parentChildMap = mutableMapOf<String, MutableList<String>>()
    edges.forEach { edge ->
        parentChildMap.getOrPut(edge.from) { mutableListOf() }.add(edge.to)
    }
    
    layers.forEachIndexed { layerIndex, nodeType ->
        val nodesInLayer = nodesByType[nodeType] ?: emptyList()
        if (nodesInLayer.isNotEmpty()) {
            val y = layerHeight * (layerIndex + 1)
            
            when (layerIndex) {
                0 -> {
                    // ROOT 노드 - 중앙 상단
                    val x = canvasSize.width / 2f
                    result.addAll(nodesInLayer.map { it.copy(x = x, y = y) })
                }
                else -> {
                    // 자식 노드들을 부모 중심으로 배치 (물리적 거리 고려)
                    val parentNodes = result.filter { parentNode ->
                        parentChildMap[parentNode.id]?.any { childId ->
                            nodesInLayer.any { it.id == childId }
                        } == true
                    }
                    
                    if (parentNodes.isNotEmpty()) {
                        parentNodes.forEach { parentNode ->
                            val childrenOfParent = nodesInLayer.filter { child ->
                                parentChildMap[parentNode.id]?.contains(child.id) == true
                            }
                            
                            childrenOfParent.forEachIndexed { index, child ->
                                val angle = (2 * PI * index / childrenOfParent.size) - PI / 2
                                val radius = 120f + (layerIndex * 20f)
                                
                                val x = (parentNode.x + cos(angle).toFloat() * radius)
                                    .coerceIn(60f, canvasSize.width - 60f)
                                
                                result.add(child.copy(x = x.toFloat(), y = y))
                            }
                        }
                    } else {
                        // 부모가 없는 노드들 균등 분산
                        val spacing = (canvasSize.width - 120f) / maxOf(1, nodesInLayer.size - 1)
                        nodesInLayer.forEachIndexed { index, node ->
                            val x = 60f + index * spacing
                            result.add(node.copy(x = x, y = y))
                        }
                    }
                }
            }
        }
    }
    
    return result
}

/**
 * 포스 디렉티드 레이아웃 (Force-Directed Layout)
 * 노드 간의 물리적 힘을 계산하여 자연스러운 배치를 생성
 */
fun layoutNodesWithForces(
    nodes: List<GraphNode>,
    edges: List<GraphEdge>,
    canvasSize: Size,
    iterations: Int = 50
): List<GraphNode> {
    if (nodes.isEmpty()) return nodes
    
    var currentNodes = nodes.map { it.copy() }
    val nodeMap = currentNodes.associateBy { it.id }.toMutableMap()
    
    // 물리 상수들
    val repulsionStrength = 1000f
    val attractionStrength = 0.1f
    val dampingFactor = 0.9f
    val minDistance = 50f
    
    repeat(iterations) { iteration ->
        val forces = mutableMapOf<String, Pair<Float, Float>>()
        
        // 모든 노드에 대해 힘 계산
        currentNodes.forEach { node ->
            var totalForceX = 0f
            var totalForceY = 0f
            
            // 1. 다른 모든 노드로부터의 척력 (Repulsion)
            currentNodes.forEach { otherNode ->
                if (node.id != otherNode.id) {
                    val dx = node.x - otherNode.x
                    val dy = node.y - otherNode.y
                    val distance = sqrt(dx * dx + dy * dy).coerceAtLeast(minDistance)
                    
                    val force = repulsionStrength / (distance * distance)
                    totalForceX += (dx / distance) * force
                    totalForceY += (dy / distance) * force
                }
            }
            
            // 2. 연결된 노드로의 인력 (Attraction)
            edges.forEach { edge ->
                val connectedNodeId = when (node.id) {
                    edge.from -> edge.to
                    edge.to -> edge.from
                    else -> null
                }
                
                connectedNodeId?.let { connectedId ->
                    val connectedNode = nodeMap[connectedId]
                    if (connectedNode != null) {
                        val dx = connectedNode.x - node.x
                        val dy = connectedNode.y - node.y
                        val distance = sqrt(dx * dx + dy * dy)
                        
                        val force = attractionStrength * distance
                        totalForceX += (dx / distance) * force
                        totalForceY += (dy / distance) * force
                    }
                }
            }
            
            // 3. 중앙으로의 약한 인력 (Centering force)
            val centerX = canvasSize.width / 2f
            val centerY = canvasSize.height / 2f
            val centerForce = 0.01f
            
            totalForceX += (centerX - node.x) * centerForce
            totalForceY += (centerY - node.y) * centerForce
            
            forces[node.id] = Pair(totalForceX, totalForceY)
        }
        
        // 힘을 적용하여 노드 위치 업데이트
        currentNodes = currentNodes.map { node ->
            val (forceX, forceY) = forces[node.id] ?: Pair(0f, 0f)
            
            // 감쇠 적용 및 속도 제한
            val dampedForceX = forceX * dampingFactor * (1f - iteration.toFloat() / iterations)
            val dampedForceY = forceY * dampingFactor * (1f - iteration.toFloat() / iterations)
            
            // 새로운 위치 계산 (경계 내에 유지)
            val newX = (node.x + dampedForceX).coerceIn(60f, canvasSize.width - 60f)
            val newY = (node.y + dampedForceY).coerceIn(60f, canvasSize.height - 60f)
            
            val updatedNode = node.copy(x = newX, y = newY)
            nodeMap[node.id] = updatedNode
            updatedNode
        }
    }
    
    return currentNodes
}

/**
 * 원형 레이아웃 (Circular Layout)
 * 노드들을 원형으로 배치
 */
fun layoutNodesInCircle(
    nodes: List<GraphNode>,
    canvasSize: Size,
    radius: Float = minOf(canvasSize.width, canvasSize.height) * 0.3f
): List<GraphNode> {
    if (nodes.isEmpty()) return nodes
    
    val centerX = canvasSize.width / 2f
    val centerY = canvasSize.height / 2f
    
    return nodes.mapIndexed { index, node ->
        val angle = 2 * PI * index / nodes.size
        val x = centerX + radius * cos(angle).toFloat()
        val y = centerY + radius * sin(angle).toFloat()
        
        node.copy(x = x, y = y)
    }
}

/**
 * 그리드 레이아웃 (Grid Layout)
 * 노드들을 격자 형태로 배치
 */
fun layoutNodesInGrid(
    nodes: List<GraphNode>,
    canvasSize: Size
): List<GraphNode> {
    if (nodes.isEmpty()) return nodes
    
    val cols = ceil(sqrt(nodes.size.toDouble())).toInt()
    val rows = ceil(nodes.size.toDouble() / cols).toInt()
    
    val cellWidth = canvasSize.width / cols
    val cellHeight = canvasSize.height / rows
    
    return nodes.mapIndexed { index, node ->
        val col = index % cols
        val row = index / cols
        
        val x = cellWidth * col + cellWidth / 2f
        val y = cellHeight * row + cellHeight / 2f
        
        node.copy(x = x, y = y)
    }
}

/**
 * 계층적 레이아웃 (Hierarchical Layout)
 * 노드 타입에 따른 계층 구조로 배치
 */
fun layoutNodesHierarchically(
    nodes: List<GraphNode>,
    edges: List<GraphEdge>,
    canvasSize: Size
): List<GraphNode> {
    if (nodes.isEmpty()) return nodes
    
    // 노드 타입별 우선순위 정의
    val typeOrder = mapOf(
        NodeType.START to 0,
        NodeType.WEBSITE to 1,
        NodeType.PAGE to 2,
        NodeType.ACTION to 3,
        NodeType.DEFAULT to 4
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