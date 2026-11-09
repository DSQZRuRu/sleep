package com.somnia.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.somnia.data.db.SleepSessionEntity
import com.somnia.data.repository.Clock
import com.somnia.data.repository.hasStages
import com.somnia.data.repository.wakeTime
import com.somnia.domain.sleep.SleepScoreCalculator
import com.somnia.domain.sleep.TimeMath
import com.somnia.ui.components.Hypnogram
import com.somnia.ui.components.SectionLabel
import com.somnia.ui.components.SomniaCard
import com.somnia.ui.components.StageLegend
import com.somnia.ui.components.StatItem
import com.somnia.ui.components.WeeklyBars
import com.somnia.ui.theme.SomniaColors
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateFormat = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH)

fun nightTitle(s: SleepSessionEntity): String =
    "Nuit du " + Instant.ofEpochMilli(s.startTime).atZone(Clock.zone()).format(dateFormat)

@Composable
fun HistoryScreen(vm: HistoryViewModel, onOpen: (Long) -> Unit) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Text("Historique", style = MaterialTheme.typography.headlineMedium) }
        item {
            SomniaCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = vm::previousWeek) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Semaine précédente") }
                    Text(if (ui.weekOffset == 0) "7 dernières nuits" else "Il y a ${ui.weekOffset} semaine${if (ui.weekOffset > 1) "s" else ""}",
                        Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = vm::nextWeek, enabled = ui.weekOffset > 0) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Semaine suivante") }
                }
                WeeklyBars(ui.week)
                Text("Zone verte : 7 à 9 h recommandées. Bleu foncé : sommeil profond estimé.",
                    style = MaterialTheme.typography.bodySmall, color = SomniaColors.Muted)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatItem("Moyenne", if (ui.avgSleep > 0) TimeMath.formatDuration(ui.avgSleep) else "—")
                    StatItem("Score moyen", if (ui.avgScore > 0) "${ui.avgScore}" else "—", color = SomniaColors.Lavender)
                    StatItem("Régularité", ui.regularity?.let { "${it.score}" } ?: "—", color = SomniaColors.Teal)
                }
                ui.regularity?.let { r ->
                    Text(
                        "Coucher moyen ${r.meanBed?.let(TimeMath::format) ?: "—"} (±${r.bedtimeSd} min) · " +
                            "lever moyen ${r.meanWake?.let(TimeMath::format) ?: "—"} (±${r.wakeSd} min)",
                        style = MaterialTheme.typography.bodySmall, color = SomniaColors.Muted,
                    )
                }
            }
        }
        if (ui.sessions.isEmpty()) {
            item { Text("Aucune nuit enregistrée pour l'instant. Lancez votre première nuit ce soir !", color = SomniaColors.Muted) }
        }
        items(ui.sessions, key = { it.id }) { s ->
            SomniaCard(Modifier.clickable { onOpen(s.id) }) {
                Text(nightTitle(s).replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.titleMedium)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatItem("Score", "${s.score}", color = SomniaColors.Lavender)
                    StatItem("Sommeil", TimeMath.formatDuration(s.totalSleepMin))
                    StatItem("Horaires", "${TimeMath.format(Clock.minuteOfDay(s.startTime))}–${TimeMath.format(Clock.minuteOfDay(s.wakeTime))}")
                }
                if (!s.hasStages) Text("Estimée via l'usage du téléphone", style = MaterialTheme.typography.bodySmall, color = SomniaColors.Muted)
            }
        }
    }
}

@Composable
fun NightDetailScreen(vm: NightDetailViewModel, onBack: () -> Unit) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    var confirm by remember { mutableStateOf(false) }
    LaunchedEffect(ui.deleted) { if (ui.deleted) onBack() }
    val s = ui.session

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour") }
            Text(s?.let { nightTitle(it) } ?: "", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
            IconButton(onClick = { confirm = true }) { Icon(Icons.Filled.Delete, "Supprimer", tint = SomniaColors.Muted) }
        }
        if (s == null) return@Column
        SomniaCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatItem("Score", "${s.score}", color = SomniaColors.Lavender)
                StatItem("Sommeil", TimeMath.formatDuration(s.totalSleepMin))
                StatItem("Efficacité", if (s.hasStages) "${(s.efficiency * 100).toInt()} %" else "—")
            }
            Text(SleepScoreCalculator.label(s.score), color = SomniaColors.Muted)
        }
        if (ui.stages.isNotEmpty()) {
            SomniaCard {
                SectionLabel("Phases estimées")
                Hypnogram(ui.stages, startLabel = TimeMath.format(Clock.minuteOfDay(s.startTime)),
                    endLabel = TimeMath.format(Clock.minuteOfDay(s.endTime ?: s.wakeTime)))
                StageLegend()
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatItem("Léger", TimeMath.formatDuration(s.lightMin), color = SomniaColors.Light)
                    StatItem("Profond", TimeMath.formatDuration(s.deepMin), color = SomniaColors.Deep)
                    StatItem("Éveil", TimeMath.formatDuration(s.awakeMin), color = SomniaColors.Awake)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatItem("Endormissement", "${s.latencyMin} min")
                    StatItem("Réveils", "${s.awakenings}")
                    StatItem("Ronflements", if (s.micEnabled) "${s.snoreMinutes} min" else "—")
                }
                Text("Estimation par actigraphie (mouvements du matelas) : utile pour suivre des tendances, pas un examen médical.",
                    style = MaterialTheme.typography.bodySmall, color = SomniaColors.Muted)
            }
        }
        ui.rewards?.takeIf { it.dreamScore > 0 }?.let { r ->
            SomniaCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel("Éclat de rêve", Modifier.weight(1f))
                    Text("${r.dreamScore}", style = MaterialTheme.typography.titleLarge, color = SomniaColors.Gold)
                }
                r.dreamLines.filter { it.active }.forEach {
                    Text("${it.label} : +${it.pct} %", style = MaterialTheme.typography.bodySmall, color = SomniaColors.Muted)
                }
                Text("+${r.stardustGained} ✦ poussière d'étoiles", style = MaterialTheme.typography.bodySmall, color = SomniaColors.Gold)
            }
        }
        s.alarmRangAt?.let {
            SomniaCard {
                SectionLabel("Réveil intelligent")
                val early = s.alarmTargetTime?.let { t -> ((t - it) / 60_000L).toInt() } ?: 0
                Text("A sonné à ${TimeMath.format(Clock.minuteOfDay(it))}" + if (early > 0) ", $early min avant l'heure limite" else "")
            }
        }
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Supprimer cette nuit ?") },
            text = { Text("Les données de la nuit seront effacées. Les récompenses déjà gagnées sont conservées.") },
            confirmButton = { TextButton(onClick = { confirm = false; vm.delete() }) { Text("Supprimer") } },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Annuler") } },
        )
    }
}
