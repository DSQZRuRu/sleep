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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import com.somnia.domain.game.Season
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

enum class Weather { NONE, SNOW, LEAVES, PETALS, FIREFLIES, RAIN }

private class Flake(val x: Float, val y: Float, val speed: Float, val size: Float, val phase: Float)

/** Météo animée derrière la vitre (zone 216–314 × 30–128 de l'illustration 360 × 240). */
@Composable
fun WindowWeather(weather: Weather, modifier: Modifier = Modifier) {
    if (weather == Weather.NONE) return
    val flakes = remember(weather) {
        val rnd = Random(weather.ordinal + 3)
        val count = when (weather) { Weather.RAIN -> 40; Weather.SNOW -> 28; Weather.FIREFLIES -> 10; else -> 12 }
        List(count) { Flake(rnd.nextFloat(), rnd.nextFloat(), 0.6f + rnd.nextFloat() * 0.8f, 0.6f + rnd.nextFloat() * 0.8f, rnd.nextFloat() * 6.28f) }
    }
    val duration = when (weather) { Weather.RAIN -> 1400; Weather.SNOW -> 9000; Weather.FIREFLIES -> 6000; else -> 11000 }
    val t by rememberInfiniteTransition(label = "weather").animateFloat(
        0f, 1f, infiniteRepeatable(tween(duration, easing = LinearEasing), RepeatMode.Restart), label = "t",
    )
    Canvas(modifier) {
        val sx = size.width / 360f
        val sy = size.height / 240f
        val left = 216 * sx; val top = 30 * sy; val w = 98 * sx; val h = 98 * sy
        clipRect(left, top, left + w, top + h) {
            for (f in flakes) {
                val progress = (f.y + t * f.speed) % 1f
                val sway = sin(progress * 2 * PI.toFloat() * 2 + f.phase)
                when (weather) {
                    Weather.SNOW -> drawCircle(Color.White.copy(alpha = 0.85f), 1.6f * f.size * density,
                        Offset(left + (f.x + sway * 0.04f) * w, top + progress * h))
                    Weather.RAIN -> drawLine(Color(0xAAB8DCFF), Offset(left + f.x * w, top + progress * h),
                        Offset(left + f.x * w - 2 * sx, top + progress * h + 9 * sy), strokeWidth = 1.2f * density)
                    Weather.FIREFLIES -> {
                        val glow = 0.4f + 0.6f * (0.5f + 0.5f * sin(t * 2 * PI.toFloat() * 3 + f.phase))
                        val p = Offset(left + (f.x + sway * 0.08f) * w, top + (0.35f + f.y * 0.6f) * h + sway * 4 * sy)
                        drawCircle(Color(0x55F6FF8A).copy(alpha = 0.35f * glow), 5f * density, p)
                        drawCircle(Color(0xFFF6FF8A).copy(alpha = glow), 1.6f * density, p)
                    }
                    Weather.LEAVES, Weather.PETALS -> {
                        val c = if (weather == Weather.LEAVES) listOf(Color(0xFFE8873A), Color(0xFFD9A441), Color(0xFFB85C2E))[(f.phase * 10).toInt() % 3]
                        else Color(0xFFFFC2D9)
                        val p = Offset(left + (f.x + sway * 0.12f) * w, top + progress * h)
                        rotate(sway * 60f, p) {
                            drawOval(c, Offset(p.x - 3 * sx, p.y - 1.6f * sy), Size(6 * sx * f.size, 3.2f * sy * f.size))
                        }
                    }
                    Weather.NONE -> Unit
                }
            }
        }
    }
}

/** Météo du moment : la pluie du son d'endormissement l'emporte, sinon la saison. */
fun weatherFor(season: Season, night: Boolean, rainPlaying: Boolean): Weather = when {
    rainPlaying -> Weather.RAIN
    season == Season.WINTER -> Weather.SNOW
    season == Season.AUTUMN -> Weather.LEAVES
    season == Season.SPRING -> Weather.PETALS
    season == Season.SUMMER && night -> Weather.FIREFLIES
    else -> Weather.NONE
}
