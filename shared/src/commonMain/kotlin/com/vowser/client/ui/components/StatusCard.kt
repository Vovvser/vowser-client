package com.vowser.client.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.vowser.client.ui.theme.AppTheme

@Composable
fun StatusCard(
    statusMessage: String,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    val status = parseStatus(statusMessage)
    
    // 기본 메시지 표시 로직
    val shouldShowExpandButton = status.details.isNotEmpty() && 
                               status.details != status.friendlyMessage &&
                               (status.statusType == StatusType.SUCCESS || status.statusType == StatusType.ERROR)
    
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.widthIn(max = 350.dp)  // 특수 최대 너비
        ) {
            // 메인 상태 메시지 행
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier.fillMaxWidth()
            ) {
            // 상태 메시지
            Text(
                text = status.friendlyMessage,
                fontSize = AppTheme.Typography.bodyMedium,
                color = getStatusTextColor(status.statusType),
                modifier = Modifier.weight(1f)
            )
            
            // 표시할 메시지 있을 때 확장 버튼 표시
            if (shouldShowExpandButton) {
                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(AppTheme.Dimensions.iconSizeMedium)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "상세 정보 숨기기" else "상세 정보 보기",
                        tint = getStatusTextColor(status.statusType),
                        modifier = Modifier.size(AppTheme.Dimensions.iconSizeSmall)
                    )
                }
            }
        }
        
        // 상세 정보 (펼치기)
        AnimatedVisibility(
            visible = isExpanded && status.details.isNotEmpty(),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            ScrollableDetailView(
                details = status.details,
                modifier = Modifier.padding(top = AppTheme.Dimensions.paddingSmall)
            )
        }
        }
    }
}

@Composable
private fun ScrollableDetailView(
    details: String,
    modifier: Modifier = Modifier
) {
    Card(
        elevation = AppTheme.Dimensions.cardElevationLow,
        backgroundColor = MaterialTheme.colors.surface.copy(alpha = 0.3f),
        modifier = modifier
    ) {
        LazyColumn(
            modifier = Modifier
                .heightIn(max = 120.dp)  // 최대 높이
                .fillMaxWidth()
                .padding(AppTheme.Dimensions.paddingSmall)
        ) {
            item {
                SelectionContainer {
                    Text(
                        text = details,
                        fontSize = AppTheme.Typography.overline,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                        lineHeight = AppTheme.Typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun getStatusTextColor(statusType: StatusType): Color {
    return when (statusType) {
        StatusType.SUCCESS -> AppTheme.Colors.Success.copy(alpha = 0.8f)
        StatusType.ERROR -> AppTheme.Colors.Error.copy(alpha = 0.8f)
        StatusType.PROCESSING -> MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
        StatusType.INFO -> MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
    }
}