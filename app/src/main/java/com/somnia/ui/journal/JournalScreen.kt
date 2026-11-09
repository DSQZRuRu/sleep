package com.somnia.ui.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.somnia.data.repository.OwnedCreature
import com.somnia.domain.game.BonusKind
import com.somnia.domain.game.CompanionLogic
import com.somnia.domain.game.CreatureProgression
import com.somnia.domain.game.DreamCreature
import com.somnia.domain.game.DreamScoreCalculator
import com.somnia.domain.game.EquippedCreature
import com.somnia.ui.components.CompanionView
import com.somnia.ui.components.CreatureView
import com.somnia.ui.components.ProgressBar
import com.somnia.ui.components.SectionLabel
import com.somnia.ui.components.SomniaCard
import com.somnia.ui.morning.rarityColor
import com.somnia.ui.theme.SomniaColors
import java.time.format.DateTimeFormatter
import java.util.Locale

fun bonusText(creature: DreamCreature, pct: Int): String =
    if (creature.bonus == BonusKind.STARDUST) "${creature.bonus.symbol} +$pct % de poussière d'étoiles chaque nuit"
    else "${creature.bonus.symbol} ${creature.bonus.label} : +$pct % d'Éclat ${creature.bonus.condition}"

@Composable
fun StardustChip(amount: Int, modifier: Modifier = Modifier) {
    Row(
        modifier.background(SomniaColors.SurfaceHigh, RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("✦", color = SomniaColors.Gold)
        Spacer(Modifier.width(6.dp))
        Text("$amount", style = MaterialTheme.typography.titleSmall, color = SomniaColors.Gold)
    }
}

@Composable
fun JournalScreen(vm: JournalViewModel) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    var detailId by remember { mutableStateOf<String?>(null) }
    var pickSlot by remember { mutableStateOf<Int?>(null) }
    var replaceCandidate by remember { mutableStateOf<String?>(null) }
    val full = GridItemSpan(3)

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { full }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Carnet des rêves", Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium)
                StardustChip(ui.game.stardust)
            }
        }

        item(span = { full }) { EventCard(ui) }

        // — Le lit —
        item(span = { full }) {
            SomniaCard {
                val equipped = ui.bed.filterNotNull().map { EquippedCreature(it.creature, it.level) }
                val brumi = CreatureProgression.companionPct(ui.stage)
                val synergy = DreamScoreCalculator.synergyPct(equipped)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        SectionLabel("Mon lit")
                        Text("Les créatures installées ajoutent leur bonus à votre Éclat de rêve, si la nuit remplit leur condition.",
                            style = MaterialTheme.typography.bodySmall, color = SomniaColors.Muted)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    (0 until 3).forEach { slot ->
                        BedSlot(
                            owned = ui.bed[slot],
                            unlocked = slot < ui.slotsUnlocked,
                            lockLevel = CreatureProgression.levelForSlot(slot),
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val o = ui.bed[slot]
                                when {
                                    o != null -> detailId = o.creature.id
                                    slot < ui.slotsUnlocked -> pickSlot = slot
                                }
                            },
                        )
                    }
                }
                if (brumi > 0) Text("☾ ${CompanionLogic.NAME} dort avec vous : +$brumi % d'Éclat", style = MaterialTheme.typography.bodySmall, color = SomniaColors.Lavender)
                synergy?.let { (label, pct) -> Text("✧ $label : +$pct % d'Éclat", style = MaterialTheme.typography.bodySmall, color = SomniaColors.Teal) }
                if (ui.game.bestDream > 0) Text("Meilleur Éclat : ${ui.game.bestDream}", style = MaterialTheme.typography.bodySmall, color = SomniaColors.Muted)
            }
        }

        item(span = { full }) {
            SomniaCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CompanionView(ui.stage, ui.mood, Modifier.size(110.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(ui.stage.label, style = MaterialTheme.typography.titleLarge)
                        Text(ui.stage.description, style = MaterialTheme.typography.bodySmall, color = SomniaColors.Muted)
                        Spacer(Modifier.height(8.dp))
                        val next = ui.stage.next
                        if (next != null) {
                            val span = next.careNeeded - ui.stage.careNeeded
                            ProgressBar((ui.game.companionCare - ui.stage.careNeeded) / span.toFloat(), color = SomniaColors.Teal)
                            Text("Prochaine forme : ${next.label} (bonus +${CreatureProgression.companionPct(next)} %)",
                                style = MaterialTheme.typography.labelSmall, color = SomniaColors.Muted)
                        } else {
                            Text("Forme finale atteinte ✧", color = SomniaColors.Gold)
                        }
                    }
                }
                Text(CompanionLogic.message(ui.mood), style = MaterialTheme.typography.bodySmall, color = SomniaColors.Muted)
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
                Text("${ui.level.xpIntoLevel} / ${ui.level.xpForLevel} XP · place de lit suivante au niveau ${
                    if (ui.slotsUnlocked < 3) CreatureProgression.levelForSlot(ui.slotsUnlocked) else "—"}",
                    style = MaterialTheme.typography.labelSmall, color = SomniaColors.Muted)
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

        item(span = { full }) { SectionLabel("Créatures du rêve · ${ui.discovered}/${ui.creatures.size} découvertes") }
        items(ui.creatures, key = { it.creature.id }) { e -> CreatureTile(e) { detailId = e.creature.id } }

        item(span = { full }) { SectionLabel("Créatures des événements") }
        items(ui.eventCreatures, key = { it.creature.id }) { e -> CreatureTile(e) { detailId = e.creature.id } }

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

    // Fiche d'une créature
    detailId?.let { id ->
        val entry = (ui.creatures + ui.eventCreatures).firstOrNull { it.creature.id == id }
        if (entry != null) CreatureDialog(
            entry = entry,
            stardust = ui.game.stardust,
            canEquip = ui.slotsUnlocked > 0,
            onDismiss = { detailId = null; vm.clearMessage() },
            onLevelUp = { vm.levelUp(id) },
            onEquip = {
                val free = (0 until ui.slotsUnlocked).firstOrNull { ui.bed[it] == null }
                if (free != null) vm.equip(id, free) else { replaceCandidate = id; pickSlot = REPLACE }
                detailId = null
            },
            onUnequip = { vm.unequip(id); detailId = null },
            message = message,
        )
    }

    // Choix d'une créature pour une place libre, ou de la place à remplacer
    pickSlot?.let { slot ->
        if (slot == REPLACE) {
            val candidate = replaceCandidate
            AlertDialog(
                onDismissRequest = { pickSlot = null },
                title = { Text("Le lit est plein") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Quelle créature doit retourner au carnet ?")
                        ui.bed.forEachIndexed { i, o ->
                            if (o != null && i < ui.slotsUnlocked) OutlinedButton(
                                onClick = { candidate?.let { vm.equip(it, i) }; pickSlot = null },
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text("Remplacer ${o.creature.name}") }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { pickSlot = null }) { Text("Annuler") } },
            )
        } else {
            val candidates = ui.owned.filter { it.slot == null }.sortedByDescending { it.bonusPct }
            AlertDialog(
                onDismissRequest = { pickSlot = null },
                title = { Text("Installer dans le lit") },
                text = {
                    if (candidates.isEmpty()) Text("Aucune créature disponible. Dormez bien : elles viendront vous rendre visite !")
                    else Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        candidates.forEach { o ->
                            Row(
                                Modifier.fillMaxWidth().background(SomniaColors.SurfaceHigh, RoundedCornerShape(16.dp))
                                    .clickable { vm.equip(o.creature.id, slot); pickSlot = null }.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                CreatureView(o.creature, Modifier.size(48.dp), animated = false)
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text("${o.creature.name} · niv. ${o.level}", style = MaterialTheme.typography.titleSmall)
                                    Text(bonusText(o.creature, o.bonusPct), style = MaterialTheme.typography.bodySmall, color = SomniaColors.Muted)
                                }
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { pickSlot = null }) { Text("Fermer") } },
            )
        }
    }
}

private const val REPLACE = -100

@Composable
private fun BedSlot(owned: OwnedCreature?, unlocked: Boolean, lockLevel: Int, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .background(Brush.verticalGradient(listOf(SomniaColors.SurfaceHigh, SomniaColors.Night)), RoundedCornerShape(20.dp))
            .border(1.dp, if (owned != null) SomniaColors.Lavender.copy(alpha = 0.6f) else Color(0x33FFFFFF), RoundedCornerShape(20.dp))
            .clickable(enabled = unlocked, onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when {
            owned != null -> {
                CreatureView(owned.creature, Modifier.fillMaxWidth().aspectRatio(1f))
                Text(owned.creature.name, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                Text("niv. ${owned.level} · ${owned.creature.bonus.symbol} +${owned.bonusPct} %",
                    style = MaterialTheme.typography.labelSmall, color = SomniaColors.Gold)
            }
            unlocked -> {
                Box(Modifier.fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
                    Text("+", style = MaterialTheme.typography.displayMedium, color = SomniaColors.Muted)
                }
                Text("Installer", style = MaterialTheme.typography.labelSmall, color = SomniaColors.Muted)
                Text(" ", style = MaterialTheme.typography.labelSmall)
            }
            else -> {
                Box(Modifier.fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
                    Text("🔒", style = MaterialTheme.typography.titleLarge)
                }
                Text("Niveau $lockLevel", style = MaterialTheme.typography.labelSmall, color = SomniaColors.Muted)
                Text(" ", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun CreatureTile(e: CreatureEntry, onClick: () -> Unit) {
    val found = e.count > 0
    Column(
        Modifier.background(SomniaColors.Surface, RoundedCornerShape(20.dp)).clickable(enabled = found, onClick = onClick).padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            CreatureView(e.creature, Modifier.fillMaxWidth().aspectRatio(1f), animated = false, silhouette = !found)
            if (e.owned?.slot != null) Text("☾", Modifier.align(Alignment.TopEnd), color = SomniaColors.Lavender)
        }
        Text(if (found) e.creature.name else "???", style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center, maxLines = 1)
        Text(
            when {
                e.owned != null -> "niv. ${e.owned.level} · ×${e.count}"
                e.creature.eventId != null -> "Événement"
                else -> e.creature.type.label
            },
            style = MaterialTheme.typography.labelSmall,
            color = if (found) rarityColor(e.creature.rarity) else SomniaColors.Muted, textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun CreatureDialog(
    entry: CreatureEntry, stardust: Int, canEquip: Boolean, message: String?,
    onDismiss: () -> Unit, onLevelUp: () -> Unit, onEquip: () -> Unit, onUnequip: () -> Unit,
) {
    val c = entry.creature
    val o = entry.owned
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { CreatureView(c, Modifier.size(120.dp)) },
        title = { Text(if (o != null) "${c.name} · niv. ${o.level}" else c.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("${c.type.label} · ${c.rarity.label}", color = rarityColor(c.rarity))
                Text(c.lore)
                if (o != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(bonusText(c, o.bonusPct), color = SomniaColors.Gold)
                    if (!o.maxed) Text("Niveau ${o.level + 1} : +${CreatureProgression.bonusPct(c, o.level + 1)} %",
                        style = MaterialTheme.typography.bodySmall, color = SomniaColors.Muted)
                    ProgressBar(o.level / CreatureProgression.MAX_LEVEL.toFloat(), color = SomniaColors.Gold)
                    Text("Rencontrée ${o.count} fois", style = MaterialTheme.typography.bodySmall, color = SomniaColors.Muted)
                    if (!o.maxed) Button(onClick = onLevelUp, enabled = stardust >= o.upgradeCost, modifier = Modifier.fillMaxWidth()) {
                        Text("Améliorer · ✦ ${o.upgradeCost}")
                    } else Text("Niveau maximum ✧", color = SomniaColors.Gold)
                    if (o.slot != null) OutlinedButton(onClick = onUnequip, modifier = Modifier.fillMaxWidth()) { Text("Retirer du lit") }
                    else if (canEquip) OutlinedButton(onClick = onEquip, modifier = Modifier.fillMaxWidth()) { Text("Installer dans le lit") }
                    message?.let { Text(it, color = SomniaColors.Teal, style = MaterialTheme.typography.bodySmall) }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fermer") } },
    )
}

private val dayFormat = DateTimeFormatter.ofPattern("d MMMM", Locale.FRENCH)

@Composable
private fun EventCard(ui: JournalUi) {
    val active = ui.activeEvent
    if (active != null) {
        val accent = Color(active.accent)
        Column(
            Modifier.fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(accent.copy(alpha = 0.35f), SomniaColors.Surface)), RoundedCornerShape(24.dp))
                .border(1.dp, accent.copy(alpha = 0.7f), RoundedCornerShape(24.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            SectionLabel("Événement en cours")
            Text("${active.symbol} ${active.name}", style = MaterialTheme.typography.titleLarge, color = accent)
            Text(active.description, style = MaterialTheme.typography.bodySmall)
            Text("Jusqu'au ${active.endDate(java.time.LocalDate.now()).format(dayFormat)} · créatures exclusives et bonus ×2 dans le lit",
                style = MaterialTheme.typography.bodySmall, color = SomniaColors.Muted)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ui.eventCreatures.filter { it.creature.eventId == active.id }.forEach {
                    CreatureView(it.creature, Modifier.size(56.dp), silhouette = it.count == 0)
                }
            }
        }
    } else ui.nextEvent?.let { next ->
        SomniaCard {
            SectionLabel("Prochain événement")
            Text("${next.symbol} ${next.name} · dans ${ui.daysToNextEvent} jour${if (ui.daysToNextEvent > 1) "s" else ""}",
                style = MaterialTheme.typography.titleMedium, color = Color(next.accent))
            Text(next.description, style = MaterialTheme.typography.bodySmall, color = SomniaColors.Muted)
        }
    }
}
