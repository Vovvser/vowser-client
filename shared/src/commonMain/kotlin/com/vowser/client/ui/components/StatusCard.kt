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
import androidx.compose.ui.unit.sp

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
            modifier = Modifier.widthIn(max = 350.dp)
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
                fontSize = 14.sp,
                color = getStatusTextColor(status.statusType),
                modifier = Modifier.weight(1f)
            )
            
            // 표시할 메시지 있을 때 확장 버튼 표시
            if (shouldShowExpandButton) {
                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "상세 정보 숨기기" else "상세 정보 보기",
                        tint = getStatusTextColor(status.statusType),
                        modifier = Modifier.size(16.dp)
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
                modifier = Modifier.padding(top = 8.dp)
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
        elevation = 1.dp,
        backgroundColor = Color.Gray.copy(alpha = 0.1f),
        modifier = modifier
    ) {
        LazyColumn(
            modifier = Modifier
                .heightIn(max = 120.dp)
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            item {
                SelectionContainer {
                    Text(
                        text = details,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.Black.copy(alpha = 0.7f),
                        lineHeight = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun getStatusTextColor(statusType: StatusType): Color {
    return when (statusType) {
        StatusType.SUCCESS -> Color.Green.copy(alpha = 0.8f)
        StatusType.ERROR -> Color.Red.copy(alpha = 0.8f)
        StatusType.PROCESSING -> MaterialTheme.colors.onSurface.copy(alpha = 0.5f) // 기본 색상
        StatusType.INFO -> MaterialTheme.colors.onSurface.copy(alpha = 0.5f) // 기본 색상
    }
}