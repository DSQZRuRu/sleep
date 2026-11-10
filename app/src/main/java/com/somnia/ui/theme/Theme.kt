package com.somnia.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import com.somnia.ui.game.Starfield
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.somnia.R
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
    val Muted = Color(0xFFAEB4D6)
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
    outline = Color(0xFF5A6496),
)

/** Police ronde et chaleureuse (Fredoka, SIL OFL 1.1 — licences/Fredoka-OFL.txt) pour les titres et chiffres. */
val GameFont = FontFamily(
    Font(R.font.fredoka_500, FontWeight.Medium),
    Font(R.font.fredoka_600, FontWeight.SemiBold),
    Font(R.font.fredoka_700, FontWeight.Bold),
)

private val base = Typography()
private val typography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = GameFont, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp),
    displayMedium = base.displayMedium.copy(fontFamily = GameFont, fontWeight = FontWeight.SemiBold),
    displaySmall = base.displaySmall.copy(fontFamily = GameFont, fontWeight = FontWeight.SemiBold),
    headlineLarge = base.headlineLarge.copy(fontFamily = GameFont, fontWeight = FontWeight.SemiBold),
    headlineMedium = base.headlineMedium.copy(fontFamily = GameFont, fontWeight = FontWeight.SemiBold),
    headlineSmall = base.headlineSmall.copy(fontFamily = GameFont, fontWeight = FontWeight.SemiBold),
    titleLarge = base.titleLarge.copy(fontFamily = GameFont, fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontFamily = GameFont, fontWeight = FontWeight.SemiBold, fontSize = 17.sp),
    titleSmall = base.titleSmall.copy(fontFamily = GameFont, fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
    labelLarge = base.labelLarge.copy(fontFamily = GameFont, fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
    labelMedium = base.labelMedium.copy(fontFamily = GameFont, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontFamily = GameFont, fontSize = 12.sp, letterSpacing = 0.8.sp, fontWeight = FontWeight.Medium),
)

@Composable
fun SomniaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = typography) {
        // Surface racine : fixe la couleur de contenu par défaut (texte clair sur fond nuit).
        // Sans elle, tout Text sans couleur explicite était noir, donc illisible.
        Surface(modifier = Modifier.fillMaxSize(), color = SomniaColors.Night, contentColor = SomniaColors.Moon) {
            Box(Modifier.fillMaxSize()) {
                Starfield(Modifier.fillMaxSize())
                content()
            }
        }
    }
}
