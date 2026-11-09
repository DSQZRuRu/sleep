package com.somnia.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import com.somnia.domain.game.CompanionMood
import com.somnia.domain.game.CompanionStage
import com.somnia.domain.game.CreatureLook
import com.somnia.domain.game.Feature
import kotlin.math.cos
import kotlin.math.sin

private val Ink = Color(0xFF0B1026)

/** Créature du rêve dessinée de façon procédurale (aucune image : univers 100 % original). */
@Composable
fun CreatureView(look: CreatureLook, modifier: Modifier = Modifier, animated: Boolean = true, silhouette: Boolean = false) {
    val float = if (animated) {
        val t by rememberInfiniteTransition(label = "float").animateFloat(
            -1f, 1f, infiniteRepeatable(tween(2200), RepeatMode.Reverse), label = "y",
        )
        t
    } else 0f
    Canvas(modifier) {
        val body = if (silhouette) Color(0xFF2A3157) else Color(look.body)
        val accent = if (silhouette) Color(0xFF2A3157) else Color(look.accent)
        val glow = if (silhouette) Color.Transparent else Color(look.glow)
        drawCreature(body, accent, glow, look.features, look.roundness, if (silhouette) -1 else look.eyeStyle, float * size.minDimension * 0.03f)
    }
}

/** Brumi, le compagnon, dont l'apparence dépend du stade d'évolution et de l'humeur. */
@Composable
fun CompanionView(stage: CompanionStage, mood: CompanionMood, modifier: Modifier = Modifier) {
    val t by rememberInfiniteTransition(label = "brumi").animateFloat(
        -1f, 1f, infiniteRepeatable(tween(2600), RepeatMode.Reverse), label = "bob",
    )
    Canvas(modifier) {
        val bob = t * size.minDimension * 0.025f
        val eyes = when (mood) {
            CompanionMood.RADIANT -> 1
            CompanionMood.CALM -> 0
            CompanionMood.SLEEPY, CompanionMood.TIRED -> 3
        }
        when (stage) {
            CompanionStage.EGG -> drawEgg(bob)
            CompanionStage.SPROUT -> drawCreature(Color(0xFFC9D1FF), Color(0xFFEEF1FF), Color(0xFFA9B4FF), setOf(Feature.ANTENNA), 1.2f, eyes, bob, scale = 0.7f)
            CompanionStage.YOUNG -> drawCreature(Color(0xFFB3BEFF), Color(0xFFE6EBFF), Color(0xFF8FA4FF), setOf(Feature.EARS, Feature.ANTENNA), 1.1f, eyes, bob, scale = 0.85f)
            CompanionStage.GROWN -> drawCreature(Color(0xFF9DA9FF), Color(0xFFDDE2FF), Color(0xFF7B8CFF), setOf(Feature.EARS, Feature.TAIL, Feature.SPOTS), 1.05f, eyes, bob)
            CompanionStage.ASTRAL -> {
                drawStars(t)
                drawCreature(Color(0xFF7F8CF5), Color(0xFFFFE6A8), Color(0xFFFFD98A), setOf(Feature.EARS, Feature.WINGS, Feature.TAIL, Feature.CROWN), 1.05f, eyes, bob)
            }
        }
        if (mood == CompanionMood.TIRED || mood == CompanionMood.SLEEPY) drawZzz(t)
    }
}

private fun DrawScope.drawCreature(
    body: Color, accent: Color, glow: Color, features: Set<Feature>, roundness: Float,
    eyeStyle: Int, dy: Float, scale: Float = 1f,
) {
    val s = size.minDimension * scale
    val c = Offset(size.width / 2, size.height / 2 + dy + size.minDimension * (1 - scale) * 0.25f)
    val w = s * 0.62f
    val h = s * 0.52f / roundness.coerceIn(0.8f, 1.3f) * 1.05f

    // Halo
    drawCircle(Brush.radialGradient(listOf(glow.copy(alpha = 0.45f), Color.Transparent), c, s * 0.55f), s * 0.55f, c)

    if (Feature.WINGS in features) {
        for (dir in listOf(-1f, 1f)) {
            val p = Path().apply {
                moveTo(c.x + dir * w * 0.35f, c.y - h * 0.1f)
                quadraticBezierTo(c.x + dir * w * 0.95f, c.y - h * 0.75f, c.x + dir * w * 0.8f, c.y + h * 0.05f)
                quadraticBezierTo(c.x + dir * w * 0.7f, c.y + h * 0.2f, c.x + dir * w * 0.35f, c.y + h * 0.1f)
                close()
            }
            drawPath(p, accent.copy(alpha = 0.75f))
        }
    }
    if (Feature.TAIL in features) {
        val p = Path().apply {
            moveTo(c.x + w * 0.3f, c.y + h * 0.3f)
            cubicTo(c.x + w * 0.8f, c.y + h * 0.5f, c.x + w * 0.85f, c.y - h * 0.1f, c.x + w * 0.62f, c.y - h * 0.05f)
        }
        drawPath(p, body, style = Stroke(width = s * 0.06f, cap = StrokeCap.Round))
        drawCircle(accent, s * 0.045f, Offset(c.x + w * 0.62f, c.y - h * 0.05f))
    }
    if (Feature.FINS in features) {
        for (dir in listOf(-1f, 1f)) {
            val p = Path().apply {
                moveTo(c.x + dir * w * 0.45f, c.y + h * 0.05f)
                lineTo(c.x + dir * w * 0.72f, c.y + h * 0.3f)
                lineTo(c.x + dir * w * 0.42f, c.y + h * 0.32f)
                close()
            }
            drawPath(p, accent)
        }
    }
    if (Feature.EARS in features) {
        for (dir in listOf(-1f, 1f)) {
            drawOval(body, Offset(c.x + dir * w * 0.28f - s * 0.07f, c.y - h * 0.62f), Size(s * 0.14f, s * 0.2f))
            drawOval(accent, Offset(c.x + dir * w * 0.28f - s * 0.04f, c.y - h * 0.55f), Size(s * 0.08f, s * 0.12f))
        }
    }
    if (Feature.HORNS in features) {
        for (dir in listOf(-1f, 1f)) {
            val p = Path().apply {
                moveTo(c.x + dir * w * 0.2f, c.y - h * 0.38f)
                quadraticBezierTo(c.x + dir * w * 0.55f, c.y - h * 0.95f, c.x + dir * w * 0.45f, c.y - h * 0.3f)
                close()
            }
            drawPath(p, accent)
        }
    }
    if (Feature.ANTENNA in features) {
        val top = Offset(c.x + w * 0.08f, c.y - h * 0.95f)
        drawLine(body, Offset(c.x, c.y - h * 0.4f), top, strokeWidth = s * 0.025f, cap = StrokeCap.Round)
        drawCircle(glow, s * 0.045f, top)
        drawCircle(accent, s * 0.025f, top)
    }

    // Corps
    drawOval(
        Brush.verticalGradient(listOf(accent, body), startY = c.y - h / 2, endY = c.y + h / 2),
        Offset(c.x - w / 2, c.y - h / 2), Size(w, h),
    )
    // Ventre
    drawOval(accent.copy(alpha = 0.55f), Offset(c.x - w * 0.25f, c.y + h * 0.02f), Size(w * 0.5f, h * 0.38f))

    if (Feature.SPOTS in features) {
        drawCircle(accent.copy(alpha = 0.7f), s * 0.03f, Offset(c.x - w * 0.32f, c.y - h * 0.05f))
        drawCircle(accent.copy(alpha = 0.7f), s * 0.022f, Offset(c.x - w * 0.36f, c.y + h * 0.12f))
        drawCircle(accent.copy(alpha = 0.7f), s * 0.026f, Offset(c.x + w * 0.34f, c.y + h * 0.04f))
    }
    if (Feature.CROWN in features) {
        val cx = c.x; val cy = c.y - h * 0.55f
        val p = Path().apply {
            moveTo(cx - s * 0.1f, cy); quadraticBezierTo(cx - s * 0.02f, cy - s * 0.12f, cx + s * 0.1f, cy - s * 0.08f)
            quadraticBezierTo(cx - s * 0.01f, cy - s * 0.06f, cx - s * 0.1f, cy); close()
        }
        drawPath(p, Color(0xFFFFE6A8))
    }

    // Yeux
    val eyeY = c.y - h * 0.08f
    val dx = w * 0.17f
    val r = s * 0.035f
    for (dir in listOf(-1f, 1f)) {
        val e = Offset(c.x + dir * dx, eyeY)
        when (eyeStyle) {
            -1 -> Unit
            1 -> drawArc(Ink, 200f, 140f, false, Offset(e.x - r, e.y - r * 0.4f), Size(r * 2, r * 1.6f), style = Stroke(s * 0.018f, cap = StrokeCap.Round))
            2 -> { drawCircle(Ink, r * 1.1f, e); drawCircle(Color.White, r * 0.4f, Offset(e.x + r * 0.35f, e.y - r * 0.35f)) }
            3 -> drawArc(Ink, 20f, 140f, false, Offset(e.x - r, e.y - r), Size(r * 2, r * 1.4f), style = Stroke(s * 0.018f, cap = StrokeCap.Round))
            else -> { drawCircle(Ink, r * 0.9f, e); drawCircle(Color.White, r * 0.3f, Offset(e.x + r * 0.3f, e.y - r * 0.3f)) }
        }
    }
    if (eyeStyle != -1) {
        // Joues
        drawCircle(Color(0xFFFF9EB5).copy(alpha = 0.35f), r * 0.9f, Offset(c.x - dx * 1.6f, eyeY + r * 1.6f))
        drawCircle(Color(0xFFFF9EB5).copy(alpha = 0.35f), r * 0.9f, Offset(c.x + dx * 1.6f, eyeY + r * 1.6f))
    }
}

private fun DrawScope.drawEgg(dy: Float) {
    val s = size.minDimension
    val c = Offset(size.width / 2, size.height / 2 + dy)
    drawCircle(Brush.radialGradient(listOf(Color(0x66A9B4FF), Color.Transparent), c, s * 0.5f), s * 0.5f, c)
    drawOval(
        Brush.verticalGradient(listOf(Color(0xFFEEF1FF), Color(0xFFA9B4FF)), startY = c.y - s * 0.3f, endY = c.y + s * 0.3f),
        Offset(c.x - s * 0.22f, c.y - s * 0.3f), Size(s * 0.44f, s * 0.58f),
    )
    drawCircle(Color(0xFFFFD98A).copy(alpha = 0.8f), s * 0.03f, Offset(c.x - s * 0.08f, c.y - s * 0.08f))
    drawCircle(Color(0xFF7FD6C7).copy(alpha = 0.8f), s * 0.025f, Offset(c.x + s * 0.09f, c.y + s * 0.05f))
    drawCircle(Color(0xFFFFD98A).copy(alpha = 0.6f), s * 0.02f, Offset(c.x - s * 0.02f, c.y + s * 0.15f))
}

private fun DrawScope.drawStars(t: Float) {
    val s = size.minDimension
    val c = Offset(size.width / 2, size.height / 2)
    for (i in 0 until 6) {
        val a = (i * 60f + t * 12f) * (Math.PI / 180f).toFloat()
        val p = Offset(c.x + cos(a) * s * 0.46f, c.y + sin(a) * s * 0.42f)
        drawCircle(Color(0xFFFFE6A8).copy(alpha = 0.5f + 0.3f * ((i % 2) * t)), s * 0.012f, p)
    }
}

private fun DrawScope.drawZzz(t: Float) {
    val s = size.minDimension
    val base = Offset(size.width * 0.78f, size.height * 0.22f - t * s * 0.02f)
    for (i in 0 until 2) {
        val o = Offset(base.x + i * s * 0.06f, base.y - i * s * 0.08f)
        val l = s * (0.04f + i * 0.015f)
        val p = Path().apply {
            moveTo(o.x, o.y); lineTo(o.x + l, o.y); lineTo(o.x, o.y + l); lineTo(o.x + l, o.y + l)
        }
        drawPath(p, Color(0xFFE8EAF6).copy(alpha = 0.6f), style = Stroke(s * 0.01f, cap = StrokeCap.Round))
    }
}
