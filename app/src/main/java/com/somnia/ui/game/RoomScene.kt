package com.somnia.ui.game

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.somnia.R
import com.somnia.data.repository.OwnedCreature
import com.somnia.domain.game.CompanionMood
import com.somnia.domain.game.CompanionStage
import com.somnia.domain.game.DecorCatalog
import com.somnia.domain.game.DecorItem
import com.somnia.domain.game.DecorSlot
import com.somnia.domain.sleep.MoonPhase
import com.somnia.ui.components.CompanionView
import com.somnia.ui.components.CreatureView
import com.somnia.ui.theme.SomniaColors
import kotlin.math.abs
import kotlin.math.cos

/** Calques de décoration (art/svg → res/drawable/art_decor_*.xml). Les objets « aucun » n'ont pas de calque. */
object DecorArt {
    @DrawableRes
    fun layer(id: String): Int? = when (id) {
        "quilt_moons" -> R.drawable.art_decor_quilt_moons
        "quilt_dawn" -> R.drawable.art_decor_quilt_dawn
        "quilt_forest" -> R.drawable.art_decor_quilt_forest
        "quilt_stars" -> R.drawable.art_decor_quilt_stars
        "curtains_lilac" -> R.drawable.art_decor_curtains_lilac
        "curtains_velvet" -> R.drawable.art_decor_curtains_velvet
        "curtains_peach" -> R.drawable.art_decor_curtains_peach
        "lamp_classic" -> R.drawable.art_decor_lamp_classic
        "lamp_paper" -> R.drawable.art_decor_lamp_paper
        "lamp_fireflies" -> R.drawable.art_decor_lamp_fireflies
        "plant_sprout" -> R.drawable.art_decor_plant_sprout
        "plant_fern" -> R.drawable.art_decor_plant_fern
        "plant_mushrooms" -> R.drawable.art_decor_plant_mushrooms
        "rug_moon" -> R.drawable.art_decor_rug_moon
        "rug_cloud" -> R.drawable.art_decor_rug_cloud
        "mobile_stars" -> R.drawable.art_decor_mobile_stars
        "mobile_dreamcatcher" -> R.drawable.art_decor_mobile_dreamcatcher
        "wall_garland" -> R.drawable.art_decor_wall_garland
        "wall_constellations" -> R.drawable.art_decor_wall_constellations
        else -> null
    }
}

/** Lune dessinée selon sa phase réelle (fraction 0 = nouvelle, 0,5 = pleine). */
@Composable
fun MoonView(fraction: Double, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val r = size.minDimension / 2 * 0.62f
        val c = Offset(size.width / 2, size.height / 2)
        val moonColor = Color(0xFFFFE6A8)
        drawCircle(Brush.radialGradient(listOf(Color(0x55FFF4D6), Color.Transparent), c, r * 2.2f), r * 2.2f, c)
        drawCircle(Color(0xFF232A5E), r, c) // face non éclairée
        val illum = MoonPhase.illumination(fraction)
        if (illum < 0.02) return@Canvas
        val waxing = fraction < 0.5
        val k = cos(2 * Math.PI * fraction).toFloat() // 1 → nouvelle, -1 → pleine
        val half = Path().apply {
            // Demi-disque éclairé : à droite quand la lune croît (hémisphère nord).
            addArc(Rect(c, r), if (waxing) -90f else 90f, 180f)
            close()
        }
        val rx = r * abs(k)
        val ellipse = Path().apply { addOval(Rect(c.x - rx, c.y - r, c.x + rx, c.y + r)) }
        val lit = Path().apply {
            op(half, ellipse, if (k > 0) PathOperation.Difference else PathOperation.Union)
        }
        drawPath(lit, moonColor)
    }
}

/**
 * La chambre : calques (fond, lune, fenêtre, décor, lit), Brumi sur l'oreiller et les créatures
 * sur la couette. Positions en fractions de l'illustration (360 × 240).
 */
@Composable
fun RoomScene(
    stage: CompanionStage,
    mood: CompanionMood,
    bed: List<OwnedCreature>,
    slotsUnlocked: Int,
    modifier: Modifier = Modifier,
    layout: Map<DecorSlot, DecorItem> = DecorCatalog.layout(emptyMap()),
    onClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(28.dp)
    val moon = remember { MoonPhase.fraction(System.currentTimeMillis()) }
    BoxWithConstraints(
        modifier.fillMaxWidth().aspectRatio(360f / 240f).clip(shape)
            .border(1.5.dp, Brush.verticalGradient(listOf(SomniaColors.Lavender.copy(alpha = 0.7f), Color.Transparent)), shape)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it },
    ) {
        val w = maxWidth
        val h = maxHeight
        RoomLayer(R.drawable.art_room_back)
        val moonSize = w * 0.16f
        MoonView(moon, Modifier.size(moonSize).offset(x = w * (290f / 360f) - moonSize / 2, y = h * (58f / 240f) - moonSize / 2))
        RoomLayer(R.drawable.art_room_window)
        for (slot in listOf(DecorSlot.WALL, DecorSlot.CURTAINS, DecorSlot.RUG, DecorSlot.LAMP, DecorSlot.PLANT)) {
            RoomLayer(layout[slot]?.let { DecorArt.layer(it.id) })
        }
        RoomLayer(R.drawable.art_room_bed)
        RoomLayer(layout[DecorSlot.QUILT]?.let { DecorArt.layer(it.id) })
        RoomLayer(layout[DecorSlot.MOBILE]?.let { DecorArt.layer(it.id) })

        // L'illustration pose ses personnages à ~87 % de sa hauteur : on aligne ce point sur l'oreiller.
        val brumiSize = w * 0.19f
        CompanionView(stage, mood, Modifier.size(brumiSize).offset(x = w * 0.255f - brumiSize / 2, y = h * 0.67f - brumiSize * 0.87f))

        val slotX = listOf(0.47f, 0.60f, 0.73f)
        val cSize = w * 0.14f
        for (i in 0 until 3) {
            val x = w * slotX[i] - cSize / 2
            val y = h * 0.80f - cSize * 0.87f
            val owned = bed.firstOrNull { it.slot == i }
            when {
                owned != null -> CreatureView(owned.creature, Modifier.size(cSize).offset(x, y), level = owned.level)
                i < slotsUnlocked -> Box(Modifier.size(cSize).offset(x, y), contentAlignment = Alignment.Center) {
                    Text("+", style = MaterialTheme.typography.headlineMedium, color = Color.White.copy(alpha = 0.45f))
                }
            }
        }
    }
}

@Composable
private fun RoomLayer(@DrawableRes res: Int?) {
    if (res != null) Image(painterResource(res), null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
}
