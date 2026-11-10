package com.somnia.ui.game

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.somnia.R
import com.somnia.data.repository.OwnedCreature
import com.somnia.domain.game.CompanionMood
import com.somnia.domain.game.CompanionStage
import com.somnia.ui.components.CompanionView
import com.somnia.ui.components.CreatureView
import com.somnia.ui.theme.SomniaColors

/**
 * La chambre : Brumi sur l'oreiller, les créatures installées sur la couette.
 * Les positions sont exprimées en fractions de l'illustration (360 × 240).
 */
@Composable
fun RoomScene(
    stage: CompanionStage,
    mood: CompanionMood,
    bed: List<OwnedCreature>,
    slotsUnlocked: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(28.dp)
    BoxWithConstraints(
        modifier.fillMaxWidth().aspectRatio(360f / 240f).clip(shape)
            .border(1.5.dp, Brush.verticalGradient(listOf(SomniaColors.Lavender.copy(alpha = 0.7f), Color.Transparent)), shape)
            .clickable(onClick = onClick),
    ) {
        val w = maxWidth
        val h = maxHeight
        Image(painterResource(R.drawable.art_room), null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)

        // Brumi sur l'oreiller
        val brumiSize = w * 0.26f
        CompanionView(stage, mood, Modifier.size(brumiSize).offset(x = w * 0.255f - brumiSize / 2, y = h * 0.6f - brumiSize * 0.85f))

        // Créatures sur la couette
        val slotX = listOf(0.40f, 0.55f, 0.70f)
        val cSize = w * 0.16f
        for (i in 0 until 3) {
            val x = w * slotX[i] - cSize / 2
            val y = h * 0.84f - cSize * 0.92f
            val owned = bed.firstOrNull { it.slot == i }
            when {
                owned != null -> CreatureView(owned.creature, Modifier.size(cSize).offset(x, y))
                i < slotsUnlocked -> Box(Modifier.size(cSize).offset(x, y), contentAlignment = Alignment.Center) {
                    Text("+", style = MaterialTheme.typography.headlineMedium, color = Color.White.copy(alpha = 0.45f))
                }
            }
        }
    }
}
