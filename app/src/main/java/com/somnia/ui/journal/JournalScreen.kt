package com.somnia.ui.journal

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.somnia.domain.game.CompanionLogic
import com.somnia.ui.components.CompanionView
import com.somnia.ui.components.CreatureView
import com.somnia.ui.components.ProgressBar
import com.somnia.ui.components.SectionLabel
import com.somnia.ui.components.SomniaCard
import com.somnia.ui.morning.rarityColor
import com.somnia.ui.theme.SomniaColors

@Composable
fun JournalScreen(vm: JournalViewModel) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    var selected by remember { mutableStateOf<CreatureEntry?>(null) }
    val full = GridItemSpan(3)

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { full }) { Text("Carnet des rêves", style = MaterialTheme.typography.headlineMedium) }

        item(span = { full }) {
            SomniaCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CompanionView(ui.stage, ui.mood, Modifier.size(120.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(ui.stage.label, style = MaterialTheme.typography.titleLarge)
                        Text(ui.stage.description, style = MaterialTheme.typography.bodySmall, color = SomniaColors.Muted)
                        Spacer(Modifier.height(8.dp))
                        val next = ui.stage.next
                        if (next != null) {
                            val span = next.careNeeded - ui.stage.careNeeded
                            ProgressBar((ui.game.companionCare - ui.stage.careNeeded) / span.toFloat(), color = SomniaColors.Teal)
                            Text("Prochaine forme : ${next.label}", style = MaterialTheme.typography.labelSmall, color = SomniaColors.Muted)
                        } else {
                            Text("Forme finale atteinte ✧", color = SomniaColors.Gold)
                        }
                    }
                }
                Text(CompanionLogic.message(ui.mood), style = MaterialTheme.typography.bodySmall, color = SomniaColors.Muted)
                Text("${CompanionLogic.NAME} grandit grâce à la qualité et à la régularité de vos nuits. Il ne régresse jamais.",
                    style = MaterialTheme.typography.bodySmall, color = SomniaColors.Muted)
            }
        }

        item(span = { full }) {
            SomniaCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        SectionLabel("Niveau")
                        Text("${ui.level.level}", style = MaterialTheme.typography.displayMedium, color = SomniaColors.Gold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        SectionLabel("Série")
                        Text("🔥 ${ui.game.streak}", style = MaterialTheme.typography.displayMedium)
                        Text("Record : ${ui.game.bestStreak}", style = MaterialTheme.typography.bodySmall, color = SomniaColors.Muted)
                    }
                }
                ProgressBar(ui.level.xpIntoLevel / ui.level.xpForLevel.toFloat(), color = SomniaColors.Gold)
                Text("${ui.level.xpIntoLevel} / ${ui.level.xpForLevel} XP", style = MaterialTheme.typography.labelSmall, color = SomniaColors.Muted)
                Text(
                    if (ui.freezeAvailable) "🛡 Protection disponible : une nuit ratée cette semaine ne cassera pas votre série."
                    else "🛡 Protection utilisée cette semaine. Elle revient lundi.",
                    style = MaterialTheme.typography.bodySmall, color = SomniaColors.Muted,
                )
            }
        }

        item(span = { full }) {
            SomniaCard {
                SectionLabel("Défis de la semaine")
                ui.challenges.forEach { cp ->
                    Column(Modifier.padding(vertical = 4.dp)) {
                        Row(Modifier.fillMaxWidth()) {
                            Text((if (cp.completed) "✓ " else "") + cp.challenge.title, Modifier.weight(1f),
                                color = if (cp.completed) SomniaColors.Teal else SomniaColors.Moon)
                            Text("${cp.progress}/${cp.challenge.target}", color = SomniaColors.Muted)
                        }
                        Text(cp.challenge.description + " · +${cp.challenge.xpReward} XP", style = MaterialTheme.typography.bodySmall, color = SomniaColors.Muted)
                        ProgressBar(cp.progress / cp.challenge.target.toFloat(), color = SomniaColors.Teal)
                    }
                }
            }
        }

        item(span = { full }) {
            SectionLabel("Créatures du rêve · ${ui.discovered}/${ui.creatures.size} découvertes")
        }
        items(ui.creatures, key = { it.creature.id }) { e ->
            val found = e.count > 0
            Column(
                Modifier.background(SomniaColors.Surface, RoundedCornerShape(20.dp)).clickable(enabled = found) { selected = e }.padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CreatureView(e.creature.look, Modifier.fillMaxWidth().aspectRatio(1f), animated = false, silhouette = !found)
                Text(if (found) e.creature.name else "???", style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center)
                Text(if (found) "×${e.count}" else e.creature.type.label, style = MaterialTheme.typography.labelSmall,
                    color = if (found) rarityColor(e.creature.rarity) else SomniaColors.Muted, textAlign = TextAlign.Center)
            }
        }

        item(span = { full }) { SectionLabel("Badges") }
        items(ui.badges, key = { it.id }) { b ->
            Column(Modifier.alpha(if (b.unlocked) 1f else 0.35f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(56.dp).background(SomniaColors.SurfaceHigh, CircleShape), contentAlignment = Alignment.Center) {
                    Text(b.symbol, style = MaterialTheme.typography.titleLarge, color = if (b.unlocked) SomniaColors.Gold else SomniaColors.Muted)
                }
                Text(b.name, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                Text(b.description, style = MaterialTheme.typography.bodySmall, color = SomniaColors.Muted, textAlign = TextAlign.Center, maxLines = 2)
            }
        }
    }

    selected?.let { e ->
        AlertDialog(
            onDismissRequest = { selected = null },
            icon = { CreatureView(e.creature.look, Modifier.size(120.dp)) },
            title = { Text(e.creature.name) },
            text = {
                Column {
                    Text("${e.creature.type.label} · ${e.creature.rarity.label}", color = rarityColor(e.creature.rarity))
                    Spacer(Modifier.height(8.dp))
                    Text(e.creature.lore)
                    Text("Rencontrée ${e.count} fois", style = MaterialTheme.typography.bodySmall, color = SomniaColors.Muted)
                }
            },
            confirmButton = { TextButton(onClick = { selected = null }) { Text("Fermer") } },
        )
    }
}

