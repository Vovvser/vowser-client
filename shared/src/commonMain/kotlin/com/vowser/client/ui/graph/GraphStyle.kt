package com.vowser.client.ui.graph

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

data class GraphStyle(
    val background: Color,
    val gridMinor: Color,
    val gridMajor: Color,

    // edges
    val edgeSoft: Color,
    val edge: Color,
    val edgeHighlight: Color,
    val edgeError: Color,
    val edgeActive: Color,

    // labels
    val labelBg: Color,
    val labelText: Color,

    // node icon/outline
    val nodeIcon: Color,

    // node fills
    val nodeNavigate: Color,
    val nodeClick: Color,
    val nodeInput: Color,
    val nodeWait: Color,

    val nodeStart: Color,
    val nodeWebsite: Color,
    val nodeAction: Color,
)

@Composable
fun rememberGraphStyle(isContributionMode: Boolean): GraphStyle {
    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.luminance() < 0.5f

    val baseBg    = if (isDark) Color(0xFF0F1218) else Color(0xFFF6F7FB)
    val gridMinor = if (isDark) Color(0xFF8A93A6).copy(alpha = 0.08f) else Color(0xFF4B5568).copy(alpha = 0.06f)
    val gridMajor = if (isDark) Color(0xFFA9B3C7).copy(alpha = 0.12f) else Color(0xFF2C3648).copy(alpha = 0.10f)

    val edgeSoft  = if (isDark) Color(0xFF9FB0C7).copy(alpha = 0.18f) else Color(0xFF2E3A4C).copy(alpha = 0.12f)
    val edgeMain  = if (isDark) Color(0xFFC9D2E3).copy(alpha = 0.55f) else Color(0xFF3C4D63).copy(alpha = 0.55f)
    val edgeHi    = Color(0xFF6A4CFF) //진한 보라
    val edgeErr   = Color(0xFFEF5350)
    val edgeAct   = Color(0xFF2BCB77) //밝은 초록

    val labelBg  = if (isDark) Color(0xFF0D1117).copy(alpha = 0.80f) else Color.White.copy(alpha = 0.90f)
    val labelTxt = cs.onSurface

    val navigate = Color(0xFF5B8CFF)
    val click    = Color(0xFFB9A6FF).copy(alpha=0.9f)
    val input    = Color(0xFFFFB85C).copy(alpha=0.9f)
    val waitCol  = Color(0xFF6CA8FF).copy(alpha=0.9f)

    val start    = Color(0xFFE48C6B)
    val website  = Color(0xFF5A74D6)
    val action   = Color(0xFF8B6BD6)

    return remember(isContributionMode, cs, isDark) {
        GraphStyle(
            background = baseBg,
            gridMinor = gridMinor,
            gridMajor = gridMajor,

            edgeSoft = edgeSoft,
            edge = edgeMain,
            edgeHighlight = edgeHi,
            edgeError = edgeErr,
            edgeActive = edgeAct,

            labelBg = labelBg,
            labelText = labelTxt,

            nodeIcon = cs.onSurface.copy(alpha = 0.92f),

            nodeNavigate = navigate,
            nodeClick = click,
            nodeInput = input,
            nodeWait = waitCol,

            nodeStart = start,
            nodeWebsite = website,
            nodeAction = action,
        )
    }
}