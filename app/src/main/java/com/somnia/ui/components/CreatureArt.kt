package com.somnia.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import com.somnia.R
import com.somnia.domain.game.CompanionMood
import com.somnia.domain.game.CompanionStage
import com.somnia.domain.game.DreamCreature
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Illustrations vectorielles (art/svg → res/drawable/art_*.xml, générées par tools/art/build_art.py).
 * Toute créature sans illustration (nouvel ajout) retombe sur le rendu procédural.
 */
object CreatureArt {
    private val creatures = mapOf(
        "plumeline" to R.drawable.art_plumeline,
        "frissonnette" to R.drawable.art_frissonnette,
        "veilleuse" to R.drawable.art_veilleuse,
        "brumelot" to R.drawable.art_brumelot,
        "songelune" to R.drawable.art_songelune,
        "dormouflon" to R.drawable.art_dormouflon,
        "abyssin" to R.drawable.art_abyssin,
        "mousselin" to R.drawable.art_mousselin,
        "lentorne" to R.drawable.art_lentorne,
        "onirion" to R.drawable.art_onirion,
        "tourbillot" to R.drawable.art_tourbillot,
        "grignoton" to R.drawable.art_grignoton,
        "cliquetis" to R.drawable.art_cliquetis,
        "orageon" to R.drawable.art_orageon,
        "givrelin" to R.drawable.art_givrelin,
        "flocondor" to R.drawable.art_flocondor,
        "lanternelle" to R.drawable.art_lanternelle,
        "citrouillon" to R.drawable.art_citrouillon,
        "brumoire" to R.drawable.art_brumoire,
        "bourgeonnet" to R.drawable.art_bourgeonnet,
        "petalune" to R.drawable.art_petalune,
        "solsticelle" to R.drawable.art_solsticelle,
        "meduzephyr" to R.drawable.art_meduzephyr,
        "opaline" to R.drawable.art_opaline,
    )

    @DrawableRes fun forCreature(id: String): Int? = creatures[id]

    @DrawableRes fun forCompanion(stage: CompanionStage, eyesClosed: Boolean): Int = when (stage) {
        CompanionStage.EGG -> R.drawable.art_brumi_egg
        CompanionStage.SPROUT -> if (eyesClosed) R.drawable.art_brumi_sprout_sleep else R.drawable.art_brumi_sprout
        CompanionStage.YOUNG -> if (eyesClosed) R.drawable.art_brumi_young_sleep else R.drawable.art_brumi_young
        CompanionStage.GROWN -> if (eyesClosed) R.drawable.art_brumi_grown_sleep else R.drawable.art_brumi_grown
        CompanionStage.ASTRAL -> if (eyesClosed) R.drawable.art_brumi_astral_sleep else R.drawable.art_brumi_astral
    }
}

private val SilhouetteFilter = ColorFilter.tint(Color(0xFF2A3157), BlendMode.SrcIn)

/** Créature du rêve : illustration si elle existe, sinon dessin procédural. */
@Composable
fun CreatureView(
    creature: DreamCreature,
    modifier: Modifier = Modifier,
    animated: Boolean = true,
    silhouette: Boolean = false,
    level: Int = 1,
) {
    val art = CreatureArt.forCreature(creature.id)
    if (art == null) {
        CreatureView(creature.look, modifier, animated, silhouette)
        return
    }
    if (!silhouette && level >= 5) {
        // Évolution : aura (niveau 5), puis couronne d'étoiles en orbite (niveau 10).
        Box(modifier) {
            EvolutionAura(creature, level, Modifier.fillMaxSize())
            CreatureImage(art, creature, Modifier.fillMaxSize(), animated, false)
        }
        return
    }
    CreatureImage(art, creature, modifier, animated, silhouette)
}

@Composable
private fun CreatureImage(@DrawableRes art: Int, creature: DreamCreature, modifier: Modifier, animated: Boolean, silhouette: Boolean) {
    val breath = if (animated) {
        val t by rememberInfiniteTransition(label = "breath").animateFloat(
            0f, 1f, infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "t",
        )
        t
    } else 0f
    Image(
        painterResource(art), contentDescription = if (silhouette) null else creature.name,
        modifier = modifier.graphicsLayer {
            transformOrigin = TransformOrigin(0.5f, 0.9f)
            scaleY = 1f + breath * 0.035f
            scaleX = 1f - breath * 0.015f
            translationY = -breath * size.height * 0.02f
        },
        colorFilter = if (silhouette) SilhouetteFilter else null,
    )
}

/** Halo d'évolution : couleur de la rareté, étoiles en orbite au niveau maximum. */
@Composable
private fun EvolutionAura(creature: DreamCreature, level: Int, modifier: Modifier) {
    val spin by rememberInfiniteTransition(label = "aura").animateFloat(
        0f, 360f, infiniteRepeatable(tween(9000, easing = androidx.compose.animation.core.LinearEasing)), label = "spin",
    )
    val color = Color(creature.look.glow)
    val gold = Color(0xFFFFD98A)
    val astral = level >= com.somnia.domain.game.CreatureProgression.MAX_LEVEL
    androidx.compose.foundation.Canvas(modifier) {
        val c = center
        val r = size.minDimension * 0.42f
        drawCircle(
            androidx.compose.ui.graphics.Brush.radialGradient(
                listOf((if (astral) gold else color).copy(alpha = 0.55f), Color.Transparent), c, r * 1.15f,
            ), r * 1.15f, c,
        )
        drawCircle(
            (if (astral) gold else color).copy(alpha = 0.7f), r, c,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = size.minDimension * 0.012f),
        )
        val n = if (astral) 5 else 3
        for (i in 0 until n) {
            val a = Math.toRadians((spin + i * 360f / n).toDouble())
            val p = androidx.compose.ui.geometry.Offset(c.x + (r * kotlin.math.cos(a)).toFloat(), c.y + (r * kotlin.math.sin(a)).toFloat())
            drawCircle(if (astral) gold else Color.White, size.minDimension * (if (astral) 0.03f else 0.022f), p)
        }
    }
}

/** Brumi illustré : respiration, clignement des yeux, yeux fermés quand il est fatigué. */
@Composable
fun CompanionView(stage: CompanionStage, mood: CompanionMood, modifier: Modifier = Modifier) {
    val sleepy = mood == CompanionMood.SLEEPY || mood == CompanionMood.TIRED
    var blink by remember { mutableStateOf(false) }
    LaunchedEffect(sleepy, stage) {
        if (sleepy || stage == CompanionStage.EGG) return@LaunchedEffect
        while (true) {
            delay(2500L + Random.nextLong(3000))
            blink = true; delay(140); blink = false
        }
    }
    val t by rememberInfiniteTransition(label = "brumi").animateFloat(
        0f, 1f, infiniteRepeatable(tween(if (sleepy) 3400 else 2600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "breath",
    )
    Box(modifier) {
        Image(
            painterResource(CompanionArtRes(stage, sleepy || blink)), contentDescription = stage.label,
            modifier = Modifier.fillMaxSize().graphicsLayer {
                transformOrigin = TransformOrigin(0.5f, 0.9f)
                if (stage == CompanionStage.EGG) {
                    rotationZ = (t - 0.5f) * 4f
                } else {
                    scaleY = 1f + t * 0.04f
                    scaleX = 1f - t * 0.015f
                    translationY = -t * size.height * 0.025f
                }
            },
        )
        if (sleepy) ZzzOverlay(Modifier.fillMaxSize(), t)
    }
}

@DrawableRes private fun CompanionArtRes(stage: CompanionStage, closed: Boolean) = CreatureArt.forCompanion(stage, closed)
