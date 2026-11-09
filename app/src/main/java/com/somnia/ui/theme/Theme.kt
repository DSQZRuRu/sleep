package com.somnia.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.somnia.domain.sleep.SleepStage

object SomniaColors {
    val Night = Color(0xFF0B1026)
    val Surface = Color(0xFF141A36)
    val SurfaceHigh = Color(0xFF1D2547)
    val Lavender = Color(0xFFA9B4FF)
    val Teal = Color(0xFF7FD6C7)
    val Gold = Color(0xFFFFD98A)
    val Moon = Color(0xFFE8EAF6)
    val Muted = Color(0xFF8C93B8)
    val Dawn = Color(0xFF3A2F5B)
    val Awake = Color(0xFFF2A07B)
    val Light = Color(0xFF8FA4FF)
    val Deep = Color(0xFF4656C2)

    fun stage(stage: SleepStage) = when (stage) {
        SleepStage.AWAKE -> Awake
        SleepStage.LIGHT -> Light
        SleepStage.DEEP -> Deep
    }
}

private val scheme = darkColorScheme(
    primary = SomniaColors.Lavender,
    onPrimary = SomniaColors.Night,
    secondary = SomniaColors.Teal,
    onSecondary = SomniaColors.Night,
    tertiary = SomniaColors.Gold,
    background = SomniaColors.Night,
    onBackground = SomniaColors.Moon,
    surface = SomniaColors.Surface,
    onSurface = SomniaColors.Moon,
    surfaceVariant = SomniaColors.SurfaceHigh,
    onSurfaceVariant = SomniaColors.Muted,
    surfaceContainer = SomniaColors.Surface,
    surfaceContainerHigh = SomniaColors.SurfaceHigh,
    outline = Color(0xFF3B4470),
)

private val base = Typography()
private val typography = Typography(
    displayLarge = base.displayLarge.copy(fontWeight = FontWeight.Light, letterSpacing = (-1).sp),
    displayMedium = base.displayMedium.copy(fontWeight = FontWeight.Light),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Light),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Normal),
    labelSmall = TextStyle(fontFamily = FontFamily.Default, fontSize = 11.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.Medium),
)

@Composable
fun SomniaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = typography, content = content)
}
