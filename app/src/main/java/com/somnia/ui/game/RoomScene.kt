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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.somnia.domain.game.BrumiTalk
import com.somnia.domain.game.Season
import com.somnia.domain.sleep.TimeMath
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.alpha
import com.somnia.data.repository.Clock
import com.somnia.domain.sleep.DayCycle
import com.somnia.domain.sleep.DayPhase
import kotlinx.coroutines.delay
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
    wakeMin: Int = 7 * 60,
    bedMin: Int = 23 * 60,
    debtMin: Int = 0,
    onClick: (() -> Unit)? = null,
) {
    val sound by com.somnia.sound.Soundscape.state.collectAsStateWithLifecycle()
    var bubble by remember { mutableStateOf<String?>(null) }
    var bubbleKey by remember { mutableIntStateOf(0) }
    val jump = remember { Animatable(0f) }
    LaunchedEffect(bubbleKey) {
        if (bubbleKey == 0) return@LaunchedEffect
        jump.snapTo(0f)
        jump.animateTo(1f, tween(180))
        jump.animateTo(0f, spring(dampingRatio = 0.35f, stiffness = 500f))
        delay(3200)
        bubble = null
    }
    val shape = RoundedCornerShape(28.dp)
    // Horloge de la scène, rafraîchie chaque minute pour faire glisser le ciel.
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) { delay(60_000L - now % 60_000L); now = System.currentTimeMillis() }
    }
    val moon = MoonPhase.fraction(now)
    val w8 = DayCycle.weights(Clock.minuteOfDay(now), wakeMin, bedMin)
    val day = w8.getValue(DayPhase.DAY)
    val dawn = w8.getValue(DayPhase.DAWN)
    val dusk = w8.getValue(DayPhase.DUSK)
    val night = w8.getValue(DayPhase.NIGHT)
    // Brumi vit au même rythme : endormi la nuit, éveillé le reste du temps (sauf s'il est fatigué).
    val sceneMood = if (night >= 0.5f) CompanionMood.SLEEPY else mood
    BoxWithConstraints(
        modifier.fillMaxWidth().aspectRatio(360f / 240f).clip(shape)
            .border(1.5.dp, Brush.verticalGradient(listOf(SomniaColors.Lavender.copy(alpha = 0.7f), Color.Transparent)), shape)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it },
    ) {
        val w = maxWidth
        val h = maxHeight
        RoomLayer(R.drawable.art_room_back)
        val moonSize = w * 0.16f
        if (night + dusk * 0.4f > 0.01f) {
            MoonView(moon, Modifier.size(moonSize).offset(x = w * (290f / 360f) - moonSize / 2, y = h * (58f / 240f) - moonSize / 2)
                .alpha((night + dusk * 0.4f).coerceIn(0f, 1f)))
        }
        RoomLayer(R.drawable.art_room_back_dusk, dusk)
        RoomLayer(R.drawable.art_room_back_dawn, dawn)
        RoomLayer(R.drawable.art_room_back_day, day)
        val today = java.time.LocalDate.now()
        WindowWeather(
            weatherFor(Season.of(today.monthValue, today.dayOfMonth), night >= 0.5f, sound.playing == com.somnia.sound.SoundKind.RAIN),
            Modifier.fillMaxSize(),
        )
        RoomLayer(R.drawable.art_room_window)
        for (slot in listOf(DecorSlot.WALL, DecorSlot.CURTAINS, DecorSlot.RUG, DecorSlot.LAMP, DecorSlot.PLANT)) {
            RoomLayer(layout[slot]?.let { DecorArt.layer(it.id) })
        }
        RoomLayer(R.drawable.art_room_bed)
        RoomLayer(layout[DecorSlot.QUILT]?.let { DecorArt.layer(it.id) })
        RoomLayer(layout[DecorSlot.MOBILE]?.let { DecorArt.layer(it.id) })
        RoomLayer(R.drawable.art_room_light_day, day)
        RoomLayer(R.drawable.art_room_light_warm, dawn + dusk)

        // L'illustration pose ses personnages à ~87 % de sa hauteur : on aligne ce point sur l'oreiller.
        val brumiSize = w * 0.19f
        val brumiX = w * 0.255f - brumiSize / 2
        val brumiY = h * 0.67f - brumiSize * 0.87f
        Box(
            Modifier.size(brumiSize).offset(x = brumiX, y = brumiY - brumiSize * 0.18f * jump.value)
                .clickable(remember { MutableInteractionSource() }, indication = null) {
                    val nowMin = Clock.minuteOfDay(System.currentTimeMillis())
                    val toBed = TimeMath.normalize(bedMin - nowMin).takeIf { it in 1..180 }
                    bubble = BrumiTalk.line(DayCycle.phase(nowMin, wakeMin, bedMin), mood, debtMin, toBed)
                    bubbleKey++
                },
        ) { CompanionView(stage, sceneMood, Modifier.fillMaxSize()) }
        bubble?.let { text ->
            Box(
                Modifier.offset(x = w * 0.04f, y = h * 0.04f).widthIn(max = w * 0.62f)
                    .background(Color(0xF2FFFFFF), RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(text, style = MaterialTheme.typography.bodySmall, color = SomniaColors.Night)
            }
        }

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
private fun RoomLayer(@DrawableRes res: Int?, alpha: Float = 1f) {
    if (res != null && alpha > 0.01f) {
        Image(painterResource(res), null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds, alpha = alpha.coerceIn(0f, 1f))
    }
}
