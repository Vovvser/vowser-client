package com.vowser.client.ui.graph

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.vowser.client.api.dto.MatchedPathDetail
import com.vowser.client.visualization.SearchInfo

@Composable
fun PathDetailDialog(
    searchInfo: SearchInfo,
    allMatchedPaths: List<MatchedPathDetail>,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // 헤더
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🔍 검색 결과 상세",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Text("✕", fontSize = 24.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 검색 정보 요약
                SearchSummaryCard(searchInfo)

                Spacer(modifier = Modifier.height(16.dp))

                // 경로 목록
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    allMatchedPaths.forEachIndexed { index, path ->
                        PathDetailCard(
                            index = index + 1,
                            path = path,
                            isTopResult = index == 0
                        )
                        if (index < allMatchedPaths.size - 1) {
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchSummaryCard(searchInfo: SearchInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🎯 검색어:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(
                    text = "\"${searchInfo.query}\"",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                InfoChip("📊 총 경로", "${searchInfo.totalPaths}개")
                InfoChip("⏱️ 검색 시간", "${searchInfo.searchTimeMs}ms")
                searchInfo.topRelevance?.let {
                    InfoChip("🎖️ 최고 관련도", "${(it * 100).toInt()}%")
                }
            }
        }
    }
}

@Composable
private fun InfoChip(label: String, value: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun PathDetailCard(
    index: Int,
    path: MatchedPathDetail,
    isTopResult: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isTopResult) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 경로 헤더
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isTopResult) {
                        Text("🏆", fontSize = 16.sp)
                    }
                    Text(
                        text = "#$index ${path.taskIntent}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = when {
                                    path.relevanceScore >= 0.7 -> MaterialTheme.colorScheme.primary
                                    path.relevanceScore >= 0.4 -> MaterialTheme.colorScheme.tertiary
                                    else -> MaterialTheme.colorScheme.error
                                },
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${(path.relevanceScore * 100).toInt()}%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            // 도메인
            Text(
                text = "🌐 ${path.domain}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            Divider()

            // 단계 목록
            Text(
                text = "📝 실행 단계 (${path.steps.size}개)",
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                path.steps.take(5).forEachIndexed { stepIndex, step ->
                    StepItem(stepIndex + 1, step)
                }

                if (path.steps.size > 5) {
                    Text(
                        text = "... 외 ${path.steps.size - 5}개 단계",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(start = 24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StepItem(order: Int, step: com.vowser.client.api.dto.PathStepDetail) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        // 순서 번호
        Text(
            text = "$order.",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(24.dp)
        )

        // 액션 아이콘
        Text(
            text = when (step.action) {
                "navigate" -> "🧭"
                "click" -> "👆"
                "input", "type" -> "⌨️"
                "wait" -> "⏳"
                else -> "⚡"
            },
            fontSize = 12.sp
        )

        // 설명
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = step.description,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            // URL (짧게)
            Text(
                text = step.url.take(50) + if (step.url.length > 50) "..." else "",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}