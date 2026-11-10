package com.somnia.ui.dreams

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.somnia.data.db.SleepSessionEntity
import com.somnia.data.repository.Clock
import com.somnia.data.repository.toSummary
import com.somnia.domain.game.DreamFragments
import com.somnia.domain.game.RewardEngine
import com.somnia.domain.game.WeekKey
import com.somnia.ui.components.SectionLabel
import com.somnia.ui.components.SomniaCard
import com.somnia.ui.history.nightTitle
import com.somnia.ui.theme.SomniaColors
import kotlinx.coroutines.flow.Flow

private const val WEEKS = 18

/** Couleur d'une case du calendrier selon le score de la nuit. */
fun scoreColor(score: Int?): Color = when {
    score == null -> Color(0x1AFFFFFF)
    score >= 85 -> SomniaColors.Gold
    else -> lerp(Color(0xFF3B4580), SomniaColors.Lavender, ((score - 30) / 55f).coerceIn(0f, 1f))
}

@Composable
fun DreamJournalScreen(history: Flow<List<SleepSessionEntity>>, onBack: () -> Unit, onOpenNight: (Long) -> Unit) {
    val nights by history.collectAsStateWithLifecycle(emptyList())
    val byDay = remember(nights) { nights.groupBy { Clock.nightDay(it.startTime) }.mapValues { e -> e.value.maxOf { it.score } } }
    val today = Clock.today()
    val firstMonday = WeekKey.mondayOf(today) - (WEEKS - 1) * 7L
    val tracked = (firstMonday..today).count { it in byDay }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour") }
                Text("Journal des rêves", style = MaterialTheme.typography.headlineMedium)
            }
        }
        item {
            SomniaCard {
                SectionLabel("Calendrier des nuits · $WEEKS semaines")
                // Grille : une colonne par semaine, une ligne par jour (lundi en haut).
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    for (w in 0 until WEEKS) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            for (d in 0 until 7) {
                                val day = firstMonday + w * 7L + d
                                val future = day > today
                                Box(
                                    Modifier.fillMaxWidth().aspectRatio(1f)
                                        .background(if (future) Color.Transparent else scoreColor(byDay[day]), RoundedCornerShape(3.dp)),
                                )
                            }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Moins bien", style = MaterialTheme.typography.labelSmall, color = SomniaColors.Muted)
                    Spacer(Modifier.width(6.dp))
                    listOf(40, 60, 75, 90).forEach {
                        Box(Modifier.size(12.dp).background(scoreColor(it), RoundedCornerShape(3.dp)))
                        Spacer(Modifier.width(3.dp))
                    }
                    Spacer(Modifier.width(3.dp))
                    Text("Excellente", style = MaterialTheme.typography.labelSmall, color = SomniaColors.Muted)
                }
                Text("$tracked nuit${if (tracked > 1) "s" else ""} suivie${if (tracked > 1) "s" else ""} sur cette période. " +
                    "Une grille bien remplie et régulière, c'est le meilleur des scores.",
                    style = MaterialTheme.typography.bodySmall, color = SomniaColors.Muted)
            }
        }
        if (nights.isEmpty()) item { Text("Vos rêves apparaîtront ici après votre première nuit.", color = SomniaColors.Muted) }
        items(nights, key = { it.id }) { n ->
            val fragment = if (n.totalSleepMin >= 180) DreamFragments.forNight(n.id, RewardEngine.sleepType(n.toSummary())) else null
            SomniaCard(Modifier.clickable { onOpenNight(n.id) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(12.dp).background(scoreColor(n.score), RoundedCornerShape(3.dp)))
                    Spacer(Modifier.width(8.dp))
                    Text(nightTitle(n).replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.titleSmall)
                }
                if (n.dreamNote.isNotBlank()) Text(n.dreamNote, style = MaterialTheme.typography.bodyMedium)
                fragment?.let {
                    Text("« $it »", style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic, color = SomniaColors.Lavender)
                }
            }
        }
    }
}
