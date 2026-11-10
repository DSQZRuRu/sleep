package com.somnia.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import com.somnia.ui.theme.SomniaColors
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private class Particle(val angle: Float, val speed: Float, val size: Float, val color: Color, val spin: Float)

/** Gerbe d'étoiles qui jaillit du centre quand [trigger] change (récompense, niveau…). */
@Composable
fun SparkleBurst(trigger: Any?, modifier: Modifier = Modifier, count: Int = 36) {
    val progress = remember { Animatable(1f) }
    val particles = remember(trigger) {
        val rnd = Random(trigger.hashCode())
        val palette = listOf(SomniaColors.Gold, SomniaColors.Lavender, SomniaColors.Teal, Color.White, Color(0xFFFFB3D9))
        List(count) {
            Particle(rnd.nextFloat() * 6.283f, 0.35f + rnd.nextFloat() * 0.65f, 4f + rnd.nextFloat() * 8f, palette[rnd.nextInt(palette.size)], rnd.nextFloat() * 4f)
        }
    }
    LaunchedEffect(trigger) {
        if (trigger == null) return@LaunchedEffect
        progress.snapTo(0f)
        progress.animateTo(1f, tween(1400, easing = FastOutSlowInEasing))
    }
    Canvas(modifier) {
        val p = progress.value
        if (p >= 1f) return@Canvas
        val c = Offset(size.width / 2, size.height / 2)
        val reach = size.minDimension * 0.55f
        for (pt in particles) {
            val d = reach * pt.speed * p
            val pos = Offset(c.x + cos(pt.angle) * d, c.y + sin(pt.angle) * d + 60f * p * p)
            val alpha = (1f - p).coerceIn(0f, 1f)
            val r = pt.size * (1f - p * 0.5f) * density / 2
            val k = r * 0.25f
            val path = Path().apply {
                moveTo(pos.x, pos.y - r)
                quadraticBezierTo(pos.x + k, pos.y - k, pos.x + r, pos.y)
                quadraticBezierTo(pos.x + k, pos.y + k, pos.x, pos.y + r)
                quadraticBezierTo(pos.x - k, pos.y + k, pos.x - r, pos.y)
                quadraticBezierTo(pos.x - k, pos.y - k, pos.x, pos.y - r)
                close()
            }
            drawPath(path, pt.color.copy(alpha = alpha))
        }
    }
}
