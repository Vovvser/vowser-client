package com.vowser.client.ui.graph

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.*

@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    corner: Dp = 14.dp,
    borderAlpha: Float = 0.18f,
    content: @Composable ColumnScope.() -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(corner))
            .background(cs.surface.copy(alpha = 0.70f))
            .border(1.dp, cs.outline.copy(alpha = borderAlpha), RoundedCornerShape(corner))
            .padding(12.dp),
        content = content
    )
}

@Composable
fun PillChip(text: String, leading: String? = null, bg: Color, fg: Color = Color.White) {
    Row(modifier = Modifier
        .clip(RoundedCornerShape(999.dp))
        .background(bg)
        .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically) {
        leading?.let { Text(it, fontSize = 13.sp, modifier = Modifier.padding(end = 6.dp)) }
        Text(text, fontSize = 13.sp, color = fg)
    }
}