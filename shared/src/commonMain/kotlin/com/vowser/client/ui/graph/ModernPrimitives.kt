package com.vowser.client.ui.graph

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.*
import com.vowser.client.ui.theme.AppTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.text.style.TextOverflow

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

@Composable
fun SearchBadge(query: String?, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val bg = cs.surface.copy(alpha = 0.85f)
    val borderBrush = Brush.linearGradient(
        colors = listOf(
            AppTheme.Colors.Purple.copy(alpha = 0.40f),
            AppTheme.Colors.Purple.copy(alpha = 0.18f)
        )
    )
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .border(1.dp, borderBrush, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = "검색",
            tint = AppTheme.Colors.Purple.copy(alpha = 0.85f),
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "검색",
            fontSize = 12.sp,
            color = cs.onSurface.copy(alpha = 0.75f)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = query ?: "-",
            fontSize = 13.sp,
            color = cs.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun StepProgressBar(
    totalSteps: Int,
    currentStep: Int,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
    corner: Dp = 999.dp,
) {
    val cs = MaterialTheme.colorScheme
    val safeTotal = if (totalSteps <= 0) 1 else totalSteps
    val clampedStep = currentStep.coerceIn(0, safeTotal)
    val frac = clampedStep.toFloat() / safeTotal.toFloat()

    val trackBrush = Brush.verticalGradient(
        colors = listOf(
            AppTheme.Colors.Purple.copy(alpha = 0.08f),
            Color.White.copy(alpha = 0.03f)
        )
    )
    val fillGradient = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF151225), // subtle purple tint
                Color(0xFF0F131C), // subtle blue tint
                Color(0xFF0B0B0F)  // near black
            )
        )
    }

    Canvas(
        modifier = modifier
            .height(height)
            .fillMaxWidth()
            .clip(RoundedCornerShape(corner))
    ) {
        drawRoundRect(
            brush = trackBrush,
            size = size,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f)
        )

        val fillWidth = size.width * frac
        if (fillWidth > 0f) {
            drawRoundRect(
                brush = fillGradient,
                size = androidx.compose.ui.geometry.Size(fillWidth, size.height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f)
            )
            if (fillWidth < size.width) {
                drawLine(
                    color = AppTheme.Colors.Purple.copy(alpha = 0.16f),
                    start = androidx.compose.ui.geometry.Offset(fillWidth, size.height * 0.20f),
                    end = androidx.compose.ui.geometry.Offset(fillWidth, size.height * 0.80f),
                    strokeWidth = 1f,
                    cap = StrokeCap.Round
                )
            }
        }

        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.White.copy(alpha = 0.06f), Color.Transparent)
            ),
            size = androidx.compose.ui.geometry.Size(size.width, size.height * 0.55f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f)
        )

        val stepWidth = size.width / safeTotal
        for (i in 1 until safeTotal) {
            val x = stepWidth * i
            drawLine(
                color = cs.outline.copy(alpha = 0.28f),
                start = androidx.compose.ui.geometry.Offset(x, size.height * 0.15f),
                end = androidx.compose.ui.geometry.Offset(x, size.height * 0.85f),
                strokeWidth = 1.0f,
                cap = StrokeCap.Round
            )
        }

        if (clampedStep > 0) {
            val markerX = stepWidth * (clampedStep - 0.5f)
            val rOuter = size.height * 0.40f
            val rInner = size.height * 0.24f
            drawCircle(
                color = Color.Black.copy(alpha = 0.60f),
                radius = rOuter,
                center = androidx.compose.ui.geometry.Offset(markerX, size.height / 2f),
                style = Stroke(width = 2.5f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.12f),
                radius = rInner,
                center = androidx.compose.ui.geometry.Offset(markerX, size.height / 2f)
            )
        }
    }
}
