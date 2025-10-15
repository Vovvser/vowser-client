package com.vowser.client.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vowser.client.ui.theme.AppTheme.Colors.GraphControl
import com.vowser.client.ui.theme.AppTheme.Colors.Green
import com.vowser.client.ui.theme.AppTheme.Colors.Info

/**
 * Vowser 앱의  테마 시스템
 */
object AppTheme {

    object Colors {
        val Green = Color(79, 197, 96)

        val Contribution = Color(79, 197, 96)
        val Warning = Color(0xFFC77A7A)
        val Info = Color(0xFF69A19D)
        val Success = Color(0xFF3C7F59)
        val Loading = Color(243, 217, 137)
        val Error = Color(231, 67, 65)

        val GraphControl = Color(0xFF66A8C6)
        val StatusBackground = Color(0xFF1A1A1A)
    }
    
    // 크기 체계
    object Dimensions {
        // 패딩/마진
        val paddingXSmall = 4.dp
        val paddingSmall = 8.dp
        val paddingMedium = 16.dp
        val paddingLarge = 24.dp
        val paddingXLarge = 32.dp
        
        // 컴포넌트 크기
        val buttonHeight = 44.dp
        val buttonHeightSmall = 28.dp
        val buttonHeightLarge = 44.dp

        // 아이콘 크기
        val iconSizeSmall = 12.dp
        val iconSizeMedium = 20.dp
        val iconSizeLarge = 44.dp
        val iconSizeXLarge = 76.dp
        
        // 카드 elevation
        val cardElevationLow = 2.dp
        val cardElevation = 4.dp
        val cardElevationHigh = 8.dp
        val cardElevationXHigh = 12.dp
        val cardElevationMax = 16.dp
        
        // 테두리 radius
        val borderRadiusSmall = 4.dp
        val borderRadius = 8.dp
        val borderRadiusLarge = 12.dp
        val borderRadiusXLarge = 16.dp
        val borderRadiusXXLarge = 32.dp
        val borderRadiusCircle = 50.dp
        
        // 간격
        val spacingXSmall = 4.dp
        val spacingSmall = 8.dp
        val spacingMedium = 12.dp
        val spacingLarge = 16.dp
    }

    // 타이포그래피 체계
    object Typography {
        // 제목 크기들
        val titleLarge = 24.sp
        val titleMedium = 20.sp
        
        // 본문 크기들
        val bodyLarge = 16.sp
        val bodyMedium = 14.sp
        val bodySmall = 12.sp
        
        // 특수 크기들
        val overline = 10.sp
    }

    /**
     * 다크 테마
     */
    val DarkTheme = darkColorScheme(
        primary = Green,
        onPrimary = Color(255, 255, 255),
        secondary = GraphControl,
        onSecondary = Color(255, 255, 255),
        tertiary = Info,
        onTertiary = Color(255, 255, 255),
        background = Color(10, 10, 10),
        onBackground = Color(255, 255, 255),
        surface = Color(18, 18, 18),
        onSurface = Color(134, 134, 134),
        error = Color(76, 24, 23),
        onError = Color(255, 255, 255),
        outline = Color(38, 38, 38),
    )

    /**
     * 라이트 테마
     */
    val LightTheme = lightColorScheme(
        primary = Green,
        onPrimary = Color(255, 255, 255),
        secondary = GraphControl,
        onSecondary = Color(255, 255, 255),
        tertiary = Info,
        onTertiary = Color(255, 255, 255),
        background = Color(255, 255, 255),
        onBackground = Color(10, 10, 10),
        surface = Color(243, 243, 243),
        onSurface = Color(112, 112, 128),
        error = Color(194, 49, 66),
        onError = Color(255, 255, 255),
        outline = Color(226, 226, 226)
    )
}