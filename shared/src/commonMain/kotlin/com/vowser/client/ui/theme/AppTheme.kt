package com.vowser.client.ui.theme

import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Vowser 앱의  테마 시스템
 */
object AppTheme {

    object Colors {
        val Contribution = Color(0xFF41B59B)
        val Warning = Color(0xFFC77A7A)
        val Info = Color(0xFF69A19D)
        val Success = Color(0xFF3C7F59)
        val Error = Color(0xFFC44040)

        val ButtonSecondary = Color(0xFF8576A2)
        val GraphControl = Color(0xFF66A8C6)
        val StatusBackground = Color(0xFF1A1A1A)
        val OverlayBackground = Color(0x80000000)
        val DisabledBackground = Color(0xFF2A2A2A)
        
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
        val buttonHeight = 48.dp
        val buttonHeightSmall = 32.dp
        val buttonHeightLarge = 56.dp
        
        // 아이콘 크기
        val iconSizeSmall = 16.dp
        val iconSizeMedium = 24.dp
        val iconSizeLarge = 48.dp
        val iconSizeXLarge = 80.dp
        
        // 카드 elevation
        val cardElevationLow = 2.dp
        val cardElevation = 4.dp
        val cardElevationHigh = 8.dp
        val cardElevationXHigh = 12.dp
        val cardElevationMax = 16.dp
        
        // 테두리 radius
        val borderRadius = 8.dp
        val borderRadiusSmall = 4.dp
        val borderRadiusLarge = 12.dp
        val borderRadiusXLarge = 16.dp
        val borderRadiusCircle = 50.dp
        
        // 간격
        val spacingXSmall = 4.dp
        val spacingSmall = 8.dp
        val spacingMedium = 12.dp
        val spacingLarge = 16.dp
    }
    
    // 애니메이션 duration
    object Animation {
        const val DurationFast = 150
        const val DurationMedium = 300
        const val DurationSlow = 500
    }
    
    // 타이포그래피 체계 (중복 제거)
    object Typography {
        // 제목 크기들
        val titleLarge = 24.sp
        val titleMedium = 20.sp
        val titleSmall = 18.sp
        
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
    val DarkTheme = darkColors(
        primary = Color(0xFF3C7F9B),
        secondary = Color(0xFF3C7F59),
        background = Color(0xFF161B22),
        surface = Color(0xFF21262D),
        error = Color(0xFFC44040),
        onPrimary = Color.White,
        onSecondary = Color.White,
        onBackground = Color(0xFFE6E6E6),
        onSurface = Color(0xFFE6E6E6),
        onError = Color.White
    )

    /**
     * 라이트 테마
     */
    val LightTheme = lightColors(
        primary = Color(0xFF3C7F9B),
        secondary = Color(0xFF3C7F59),
        background = Color(234, 234, 234),
        surface = Color.White,
        error = Color(0xFFC44040),
        onPrimary = Color.White,
        onSecondary = Color.White,
        onBackground = Color(30, 30, 30),
        onSurface = Color(30, 30, 30),
        onError = Color.White
    )
}