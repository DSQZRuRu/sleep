package com.somnia.ui.room

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.somnia.domain.game.BonusKind
import com.somnia.domain.game.DecorCatalog
import com.somnia.domain.game.DecorItem
import com.somnia.domain.game.DecorSlot
import com.somnia.domain.sleep.MoonPhase
import com.somnia.ui.components.SectionLabel
import com.somnia.ui.components.SomniaCard
import com.somnia.ui.game.GameButton
import com.somnia.ui.game.ResourceChip
import com.somnia.ui.game.RoomScene
import com.somnia.ui.theme.SomniaColors

fun decorBonusText(item: DecorItem): String? {
    val b = item.bonus ?: return null
    return if (b == BonusKind.STARDUST) "${b.symbol} +${item.bonusPct} % de poussière d'étoiles"
    else "${b.symbol} +${item.bonusPct} % d'Éclat ${b.condition}"
}

/** « Ma chambre » : décorer la chambre de Brumi avec la poussière d'étoiles. */
@Composable
fun RoomScreen(vm: RoomViewModel, onBack: () -> Unit, onManageBed: () -> Unit) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val slot by vm.slot.collectAsStateWithLifecycle()
    val preview by vm.preview.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val layout = ui.room.layout + (preview?.let { mapOf(it.slot to it) } ?: emptyMap())
    val now = System.currentTimeMillis()
    val moon = MoonPhase.fraction(now)

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour") }
            Text("Ma chambre", Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium)
            ResourceChip("✦", "${ui.stardust}", SomniaColors.Gold)
        }
        RoomScene(ui.stage, ui.mood, ui.bed, ui.slotsUnlocked, layout = layout, wakeMin = ui.wakeMin, bedMin = ui.bedMin)
        val phase = com.somnia.domain.sleep.DayCycle.phase(com.somnia.data.repository.Clock.minuteOfDay(now), ui.wakeMin, ui.bedMin)
        Text(
            "${phase.label} dans la chambre · ${MoonPhase.name(moon)} ce soir" + (MoonPhase.daysToFullMoon(now).let { d ->
                if (d == 0) " · Opaline pourrait vous rendre visite !" else " · pleine lune dans $d jour${if (d > 1) "s" else ""}"
            }),
            style = MaterialTheme.typography.bodySmall, color = SomniaColors.Muted,
        )
        preview?.let { Text("Aperçu : ${it.name}", style = MaterialTheme.typography.titleSmall, color = SomniaColors.Lavender) }
        OutlinedButton(onClick = onManageBed, modifier = Modifier.fillMaxWidth()) { Text("Gérer les créatures du lit") }

        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DecorSlot.entries.forEach { s -> FilterChip(selected = slot == s, onClick = { vm.selectSlot(s) }, label = { Text(s.label) }) }
        }
        message?.let { Text(it, color = SomniaColors.Teal, style = MaterialTheme.typography.bodySmall) }

        DecorCatalog.ofSlot(slot).forEach { item ->
            val owned = ui.room.owns(item)
            val installed = ui.room.layout[item.slot]?.id == item.id
            val selected = preview?.id == item.id
            val shape = RoundedCornerShape(22.dp)
            Column(
                Modifier.fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color(0xEB1D2547), Color(0xEB121734))), shape)
                    .border(if (selected || installed) 2.dp else 1.dp,
                        if (installed) SomniaColors.Teal else if (selected) SomniaColors.Lavender else Color(0x33FFFFFF), shape)
                    .clickable { vm.preview(item) }
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    Text(
                        when {
                            installed -> "Installé"
                            owned -> "Possédé"
                            else -> "✦ ${item.price}"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = if (installed) SomniaColors.Teal else if (owned) SomniaColors.Muted else SomniaColors.Gold,
                    )
                }
                Text(item.description, style = MaterialTheme.typography.bodySmall, color = SomniaColors.Muted)
                decorBonusText(item)?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = SomniaColors.Gold) }
                if (selected && !installed) {
                    GameButton(
                        if (owned) "Installer" else "Acheter · ✦ ${item.price}",
                        onClick = { vm.place(item) },
                        enabled = owned || ui.stardust >= item.price,
                        color = if (owned) SomniaColors.Lavender else SomniaColors.Gold,
                        height = 48.dp,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        SomniaCard {
            SectionLabel("Bon à savoir")
            Text("Les bonus du décor sont modestes et, comme ceux des créatures, ne comptent que si la nuit remplit leur condition.",
                style = MaterialTheme.typography.bodySmall, color = SomniaColors.Muted)
        }
    }
}
