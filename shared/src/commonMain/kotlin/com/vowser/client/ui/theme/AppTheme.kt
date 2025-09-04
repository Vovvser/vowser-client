package com.vowser.client.ui.theme

import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.ui.graphics.Color

/**
 * Vowser 앱의 테마 관리
 */
object AppTheme {

    // 색상 팔레트
    object Colors {
        val Primary = Color(0xFF3C7F9B)
        val Success = Color(0xFF3C7F59)
        val Contribution = Color(0xFF41B59B)
        val Background = Color(0xFF161B22)
        val Error = Color(0xFFC44040)
        val Warning = Color(0xFFC77A7A)
        val Info = Color(0xFF69A19D)
        val Text = Color(30, 30, 30)
        val LightBackground = Color(234, 234, 234)
        val DarkText = Color(0xFFE6E6E6)
    }

    /**
     * 일반 모드 테마 (Dark)
     */
    val NormalThemeDark = darkColors(
        primary = Colors.Primary,
        secondary = Colors.Success,
        background = Colors.Background,
        onPrimary = Color.White,
        onSecondary = Color.White,
        onBackground = Colors.DarkText,
        onSurface = Colors.DarkText
    )

    /**
     * 기여 모드 테마 (Dark)
     */
    val ContributionThemeDark = darkColors(
        primary = Colors.Contribution,
        secondary = Colors.Contribution,
        background = Colors.Background,
        onPrimary = Color.White,
        onSecondary = Color.White,
        onBackground = Colors.DarkText,
        onSurface = Colors.DarkText
    )

    /**
     * 일반 모드 테마 (Light)
     */
    val NormalThemeLight = lightColors(
        primary = Colors.Primary,
        secondary = Colors.Success,
        background = Colors.LightBackground,
        surface = Colors.LightBackground,
        onPrimary = Colors.Text,
        onSecondary = Colors.Text,
        onBackground = Colors.Text,
        onSurface = Colors.Text
    )

    /**
     * 기여 모드 테마 (Light)
     */
    val ContributionThemeLight = lightColors(
        primary = Colors.Contribution,
        secondary = Colors.Contribution,
        background = Colors.LightBackground,
        surface = Colors.LightBackground,
        onPrimary = Colors.Text,
        onSecondary = Colors.Text,
        onBackground = Colors.Text,
        onSurface = Colors.Text
    )
}