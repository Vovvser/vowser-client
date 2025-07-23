package com.vowser.client.ui.theme

import androidx.compose.material.darkColors
import androidx.compose.ui.graphics.Color

/**
 * Vowser 앱의 테마 관리
 */
object AppTheme {
    
    // 색상 팔레트
    object Colors {
        val Primary = Color(0xFF0969DA)          // GitHub Blue
        val Success = Color(0xFF238636)          // GitHub Green
        val Contribution = Color(0xFF00D4AA)     // Mint Green
        val Background = Color(0xFF0D1117)       // GitHub Dark
        val Error = Color(0xFFFF4444)            // Red
        val Warning = Color(0xFFFF6B6B)          // Orange Red
        val Info = Color(0xFF4ECDC4)             // Cyan
    }
    
    /**
     * 일반 모드 테마
     */
    val NormalTheme = darkColors(
        primary = Colors.Primary,
        secondary = Colors.Success,
        background = Colors.Background
    )
    
    /**
     * 기여 모드 테마
     */
    val ContributionTheme = darkColors(
        primary = Colors.Contribution,
        secondary = Colors.Contribution,
        background = Colors.Background
    )
}