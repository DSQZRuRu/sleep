package com.somnia.ui.game

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.somnia.ui.theme.SomniaColors
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

private class Star(val x: Float, val y: Float, val r: Float, val phase: Float, val speed: Float, val warm: Boolean)

/** Ciel nocturne animé : dégradé, nébuleuses qui dérivent et étoiles qui scintillent. */
@Composable
fun Starfield(modifier: Modifier = Modifier, density: Int = 90) {
    val stars = remember(density) {
        val rnd = Random(7)
        List(density) {
            Star(rnd.nextFloat(), rnd.nextFloat(), 0.6f + rnd.nextFloat() * 1.6f, rnd.nextFloat() * 2f * PI.toFloat(),
                0.6f + rnd.nextFloat() * 1.4f, rnd.nextInt(6) == 0)
        }
    }
    val t by rememberInfiniteTransition(label = "sky").animateFloat(
        0f, 1f, infiniteRepeatable(tween(60_000, easing = LinearEasing), RepeatMode.Restart), label = "t",
    )
    Canvas(modifier) {
        drawRect(Brush.verticalGradient(listOf(Color(0xFF070B1F), SomniaColors.Night, Color(0xFF151036))))
        // Nébuleuses
        val drift = sin(t * 2f * PI.toFloat())
        drawCircle(
            Brush.radialGradient(listOf(Color(0x334656C2), Color.Transparent),
                center = Offset(size.width * (0.2f + 0.05f * drift), size.height * 0.25f), radius = size.width * 0.7f),
            radius = size.width * 0.7f, center = Offset(size.width * (0.2f + 0.05f * drift), size.height * 0.25f),
        )
        drawCircle(
            Brush.radialGradient(listOf(Color(0x2A7B5BD0), Color.Transparent),
                center = Offset(size.width * (0.85f - 0.05f * drift), size.height * 0.7f), radius = size.width * 0.8f),
            radius = size.width * 0.8f, center = Offset(size.width * (0.85f - 0.05f * drift), size.height * 0.7f),
        )
        val time = t * 60f
        for (s in stars) {
            val a = 0.35f + 0.65f * (0.5f + 0.5f * sin(time * s.speed + s.phase))
            val c = if (s.warm) SomniaColors.Gold else Color.White
            val center = Offset(s.x * size.width, s.y * size.height)
            drawCircle(c.copy(alpha = a * 0.22f), s.r * 3f * this.density, center)
            drawCircle(c.copy(alpha = a), s.r * this.density, center)
        }
    }
}
