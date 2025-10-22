package com.vowser.client.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
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
    val icon: String,
    val description: String,
    val isDefault: Boolean = false
)

/**
 * STT 모드 선택 UI 컴포넌트
 */
@Composable
fun SttModeSelector(
    selectedMode: String?,
    onModeSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    isVisible: Boolean = true
) {
    val availableModes = listOf(
        SttMode(
            id = "general",
            name = "기본",
            icon = "가",
            description = "일반 음성 인식",
            isDefault = true
        ),
        SttMode(
            id = "number",
            name = "숫자",
            icon = "123",
            description = "숫자 인식 최적화",
        ),
        SttMode(
            id = "alphabet",
            name = "알파벳",
            icon = "ABC",
            description = "알파벳 인식 최적화",
        )
    )

    if (isVisible) {
        Card(
            modifier = modifier
                .background(
                    MaterialTheme.colorScheme.background,
                    RoundedCornerShape(AppTheme.Dimensions.borderRadiusXLarge)
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(AppTheme.Dimensions.borderRadiusXLarge)
                )
                .padding(2.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = AppTheme.Dimensions.cardElevationLow),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
        ) {
            Row(
                modifier = Modifier
                    .border(
                        width = 0.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(AppTheme.Dimensions.borderRadiusSmall)
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppTheme.Dimensions.paddingSmall)
            ) {
                availableModes.forEach { mode ->
                    SttModeChip(
                        mode = mode,
                        isSelected = selectedMode == mode.id,
                        onToggle = { onModeSelect(mode.id) }
                    )
                }
            }
        }
    }
}

/**
 * STT 모드 선택
 */
@Composable
private fun SttModeChip(
    mode: SttMode,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.onBackground
    } else {
        MaterialTheme.colorScheme.background
    }
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.background
    } else {
        MaterialTheme.colorScheme.onBackground
    }

    Button(
        onClick = onToggle,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = MaterialTheme.colorScheme.surface,
            disabledContentColor =  MaterialTheme.colorScheme.onSurface,
        ),
        modifier = Modifier.width(120.dp)
    ) {
        Text(
            text = mode.icon,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 14.sp
            ),
            color = contentColor,
            modifier = Modifier.padding(end = 4.dp)
        )
        Text(
            text = mode.name,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 13.sp
            ),
            color = contentColor
        )
    }
}