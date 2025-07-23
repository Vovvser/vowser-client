package com.vowser.client.ui.graph

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 플로팅 그래프 컨트롤 패널 컴포넌트
 */
@Composable
fun FloatingControlPanel(
    scale: Float,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onReset: () -> Unit,
    onCenterView: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(16.dp)
            .background(
                color = Color.Black.copy(alpha = 0.7f),
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        elevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 줌 인
            FloatingActionButton(
                onClick = onZoomIn,
                modifier = Modifier.size(40.dp),
                backgroundColor = Color(0xFF238636),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Zoom In", modifier = Modifier.size(20.dp))
            }
            
            // 줌 아웃
            FloatingActionButton(
                onClick = onZoomOut,
                modifier = Modifier.size(40.dp),
                backgroundColor = Color(0xFF238636),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Zoom Out", modifier = Modifier.size(20.dp))
            }
            
            // 리셋
            FloatingActionButton(
                onClick = onReset,
                modifier = Modifier.size(40.dp),
                backgroundColor = Color(0xFF8250DF),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Reset", modifier = Modifier.size(20.dp))
            }
            
            // 중앙 정렬
            FloatingActionButton(
                onClick = onCenterView,
                modifier = Modifier.size(40.dp),
                backgroundColor = Color(0xFF0969DA),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = "Center", modifier = Modifier.size(20.dp))
            }
            
            // 확대/축소 레벨 표시
            Text(
                text = "${(scale * 100).toInt()}%",
                color = Color.White,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}