package com.somnia.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.somnia.domain.game.CompanionLogic
import com.somnia.domain.sleep.SleepScoreCalculator
import com.somnia.domain.sleep.TimeMath
import com.somnia.tracking.SleepTrackingService
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.somnia.ui.components.CompanionView
import com.somnia.ui.components.CreatureView
import com.somnia.ui.components.ProgressBar
import com.somnia.ui.components.SectionLabel
import com.somnia.ui.components.SomniaCard
import com.somnia.ui.components.StatItem
import com.somnia.ui.components.TimeStepper
import com.somnia.ui.permissions.PermissionStatus
import com.somnia.ui.theme.SomniaColors
import java.time.LocalTime

@Composable
fun HomeScreen(vm: HomeViewModel, onSettings: () -> Unit, onOpenTip: () -> Unit, onOpenJournal: () -> Unit) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val bed by vm.bed.collectAsStateWithLifecycle()
    val event by vm.event.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refreshUsageEstimate() }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(greeting(), style = MaterialTheme.typography.headlineMedium)
                Text("Niveau ${ui.level.level} · série de ${ui.game.streak} nuit${if (ui.game.streak > 1) "s" else ""}",
                    color = SomniaColors.Muted)
            }
            IconButton(onClick = onSettings) { Icon(Icons.Filled.Settings, "Réglages", tint = SomniaColors.Muted) }
        }

        // Compagnon
        SomniaCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CompanionView(ui.stage, ui.mood, Modifier.size(110.dp))
                Spacer(Modifier.size(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("${ui.stage.label} · ${ui.mood.label}", style = MaterialTheme.typography.titleMedium)
                    Text(CompanionLogic.message(ui.mood), style = MaterialTheme.typography.bodySmall, color = SomniaColors.Muted)
                    Spacer(Modifier.height(8.dp))
                    ProgressBar(ui.level.xpIntoLevel / ui.level.xpForLevel.toFloat(), color = SomniaColors.Gold)
                    Text("${ui.level.xpIntoLevel} / ${ui.level.xpForLevel} XP", style = MaterialTheme.typography.labelSmall, color = SomniaColors.Muted)
                }
            }
        }

        event?.let { e ->
            val accent = Color(e.accent)
            Column(
                Modifier.fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(accent.copy(alpha = 0.35f), SomniaColors.Surface)), RoundedCornerShape(24.dp))
                    .clickable(onClick = onOpenJournal)
                    .padding(16.dp),
            ) {
                Text("${e.symbol} ${e.name}", style = MaterialTheme.typography.titleMedium, color = accent)
                Text("Créatures de saison et poussière d'étoiles ×${e.stardustMultiplier}".replace(".0", ""),
                    style = MaterialTheme.typography.bodySmall)
            }
        }

        // Le lit de ce soir
        SomniaCard(Modifier.clickable(onClick = onOpenJournal)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    SectionLabel("Ce soir dans votre lit")
                    Text(
                        if (bed.isEmpty()) "Personne pour l'instant : installez une créature depuis le Carnet pour gagner des bonus d'Éclat."
                        else bed.joinToString(" · ") { "${it.creature.name} ${it.creature.bonus.symbol}+${it.bonusPct} %" },
                        style = MaterialTheme.typography.bodySmall, color = SomniaColors.Muted,
                    )
                }
                Text("✦ ${ui.game.stardust}", color = SomniaColors.Gold, style = MaterialTheme.typography.titleSmall)
            }
            if (bed.isNotEmpty()) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                bed.forEach { CreatureView(it.creature, Modifier.size(56.dp)) }
            }
        }

        // Estimation de secours
        ui.usageEstimate?.let { e ->
            SomniaCard {
                SectionLabel("Nuit non suivie ?")
                Text("D'après l'usage du téléphone, vous auriez dormi de ${TimeMath.format(com.somnia.data.repository.Clock.minuteOfDay(e.sleepStart))} " +
                    "à ${TimeMath.format(com.somnia.data.repository.Clock.minuteOfDay(e.wakeTime))}.")
                Text("Estimation sans phases de sommeil.", style = MaterialTheme.typography.bodySmall, color = SomniaColors.Muted)
                OutlinedButton(onClick = vm::acceptUsageEstimate) { Text("Enregistrer cette nuit") }
            }
        }

        // Ce soir
        SomniaCard {
            SectionLabel("Ce soir")
            Text("Coucher idéal", color = SomniaColors.Muted)
            Text(TimeMath.format(ui.idealBed), style = MaterialTheme.typography.displayLarge, color = SomniaColors.Lavender)
            Text(
                "Pour ${ui.options.firstOrNull { it.ideal }?.cycles ?: 5} cycles de ~${ui.profile.cycleMin} min, " +
                    "en comptant ${ui.profile.latencyMin} min pour vous endormir.",
                style = MaterialTheme.typography.bodySmall, color = SomniaColors.Muted,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ui.options.filter { !it.ideal && it.recommended }.forEach {
                    Text("ou ${TimeMath.format(it.bedtimeMin)} (${it.cycles} cycles)", style = MaterialTheme.typography.bodySmall, color = SomniaColors.Muted)
                }
            }
        }

        SomniaCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    SectionLabel("Réveil intelligent")
                    Text(
                        if (ui.profile.smartAlarmEnabled) "Sonne en sommeil léger, dans les ${ui.profile.smartWindowMin} min avant"
                        else "Sonne à l'heure exacte",
                        style = MaterialTheme.typography.bodySmall, color = SomniaColors.Muted,
                    )
                }
                Switch(ui.alarmTonight, vm::setAlarmTonight)
            }
            if (ui.alarmTonight) {
                TimeStepper(ui.profile.targetWakeMin, { vm.setWakeTime(it) }, Modifier.align(Alignment.CenterHorizontally), step = 5)
                if (!PermissionStatus.notifications(context) || !PermissionStatus.exactAlarms(context)) {
                    Text("⚠ Autorisez les notifications et les alarmes exactes dans les réglages pour que le réveil sonne.",
                        style = MaterialTheme.typography.bodySmall, color = SomniaColors.Awake)
                }
            }
        }

        Button(
            onClick = { SleepTrackingService.start(context, vm.nextAlarmAt()) },
            modifier = Modifier.fillMaxWidth().height(64.dp),
        ) { Text("Commencer ma nuit", style = MaterialTheme.typography.titleMedium) }
        Text("Posez le téléphone sur le matelas, près de l'oreiller, puis éteignez l'écran. Branchez-le si possible.",
            style = MaterialTheme.typography.bodySmall, color = SomniaColors.Muted)

        ui.lastNight?.let { n ->
            SomniaCard {
                SectionLabel("Dernière nuit")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatItem("Score", "${n.score}", color = SomniaColors.Lavender)
                    StatItem("Sommeil", TimeMath.formatDuration(n.totalSleepMin))
                    StatItem("Profond", TimeMath.formatDuration(n.deepMin))
                }
                Text(SleepScoreCalculator.label(n.score), color = SomniaColors.Muted)
            }
        }

        SomniaCard(Modifier.clickable(onClick = onOpenTip)) {
            SectionLabel("Conseil du jour · ${ui.tip.category}")
            Text(ui.tip.title, style = MaterialTheme.typography.titleMedium)
            Text(ui.tip.body, style = MaterialTheme.typography.bodySmall, color = SomniaColors.Muted, maxLines = 3)
        }
        Spacer(Modifier.height(8.dp))
    }
}

private fun greeting(): String = when (LocalTime.now().hour) {
    in 5..11 -> "Bonjour"
    in 12..17 -> "Bon après-midi"
    else -> "Bonsoir"
}
