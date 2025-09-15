package com.vowser.client.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vowser.client.ui.theme.AppTheme

/**
 * STT 모드 정보
 */
data class SttMode(
    val id: String,
    val name: String,
    val description: String,
    val iconEmoji: String,
    val isDefault: Boolean = false
)

/**
 * STT 모드 선택 UI 컴포넌트
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SttModeSelector(
    selectedModes: Set<String>,
    onModeToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
    isVisible: Boolean = true
) {
    val availableModes = listOf(
        SttMode(
            id = "general",
            name = "기본",
            description = "일반 음성 인식",
            iconEmoji = "\uD83D\uDD35",
            isDefault = true
        ),
        SttMode(
            id = "number",
            name = "숫자",
            description = "숫자 인식 최적화",
            iconEmoji = "\uD83D\uDFE2",
        ),
        SttMode(
            id = "alphabet",
            name = "알파벳",
            description = "알파벳 인식 최적화",
            iconEmoji = "\uD83D\uDFE1",
        ),
        SttMode(
            id = "snippet",
            name = "스니펫",
            description = "코드/명령어 인식",
            iconEmoji = "\uD83D\uDFE3",
        )
    )

    if (isVisible) {
        Card(
            modifier = modifier.fillMaxWidth(),
            elevation = AppTheme.Dimensions.cardElevationLow,
            backgroundColor = MaterialTheme.colors.surface,
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppTheme.Dimensions.paddingMedium, vertical = AppTheme.Dimensions.paddingSmall),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppTheme.Dimensions.paddingSmall)
            ) {
                Text(
                    text = "STT:",
                    style = MaterialTheme.typography.body2.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    ),
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    availableModes.forEach { mode ->
                        SttModeChip(
                            mode = mode,
                            isSelected = selectedModes.contains(mode.id),
                            onToggle = { onModeToggle(mode.id) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * STT 모드 선택 칩
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun SttModeChip(
    mode: SttMode,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    val contentColor = if (isSelected) {
        MaterialTheme.colors.primary
    } else {
        MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
    }

    Row(
        modifier = Modifier
            .clickable { onToggle() }
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = mode.iconEmoji,
            fontSize = 14.sp,
            color = contentColor
        )

        Text(
            text = mode.name,
            style = MaterialTheme.typography.caption.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 13.sp
            ),
            color = contentColor
        )
    }
}