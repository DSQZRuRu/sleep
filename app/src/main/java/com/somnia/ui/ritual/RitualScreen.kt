package com.somnia.ui.ritual

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.somnia.domain.game.CompanionMood
import com.somnia.domain.game.CompanionStage
import com.somnia.reminders.ReminderPrefs
import com.somnia.sound.SoundscapePicker
import com.somnia.tracking.SleepTrackingService
import com.somnia.ui.components.CompanionView
import com.somnia.ui.components.SectionLabel
import com.somnia.ui.components.SomniaCard
import com.somnia.ui.game.GameButton
import com.somnia.ui.game.GameGauge
import com.somnia.ui.game.SparkleBurst
import com.somnia.ui.theme.SomniaColors

/** Une phase de respiration : libellé et durée en secondes. */
private data class BreathStep(val label: String, val seconds: Int, val target: Float)

private enum class BreathPattern(val label: String, val description: String, val steps: List<BreathStep>) {
    COHERENCE("Cohérence 5-5", "Inspirez 5 s, expirez 5 s : environ 6 respirations par minute.",
        listOf(BreathStep("Inspirez", 5, 1f), BreathStep("Expirez", 5, 0f))),
    RELAX("Relaxation 4-7-8", "Inspirez 4 s, retenez 7 s, expirez lentement 8 s.",
        listOf(BreathStep("Inspirez", 4, 1f), BreathStep("Retenez", 7, 1f), BreathStep("Expirez", 8, 0f))),
}

private val CHECKLIST = listOf(
    "💡 Lumières tamisées, chaudes",
    "📱 Écrans posés ou en mode nuit",
    "🌡 Chambre fraîche et aérée",
    "📝 Demain noté sur un papier, l'esprit libre",
)

/**
 * Rituel du soir guidé (3 à 5 min) : petite liste, respiration lente avec Brumi, puis lancement de la nuit.
 * Terminé, il rapporte un peu d'XP si la nuit suit dans les 2 h.
 */
@Composable
fun RitualScreen(
    stage: CompanionStage,
    prefs: ReminderPrefs,
    profileWakeAlarm: () -> Long?,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val checked = remember { mutableStateListOf<Int>() }
    var pattern by remember { mutableStateOf(BreathPattern.COHERENCE) }
    var running by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(prefs.ritualRecent()) }
    var label by remember { mutableStateOf("Prêt ?") }
    var elapsed by remember { mutableIntStateOf(0) }
    val totalSeconds = 180
    val breath = remember { Animatable(0f) }

    LaunchedEffect(running, pattern) {
        if (!running) return@LaunchedEffect
        elapsed = 0
        while (elapsed < totalSeconds) {
            for (step in pattern.steps) {
                label = step.label
                breath.animateTo(step.target, tween(step.seconds * 1000, easing = LinearEasing))
                elapsed += step.seconds
                if (elapsed >= totalSeconds) break
            }
        }
        running = false
        done = true
        label = "Bravo ✦"
        prefs.lastRitualAt = System.currentTimeMillis()
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour") }
                Text("Rituel du soir", style = MaterialTheme.typography.headlineMedium)
            }

            SomniaCard {
                SectionLabel("Préparer la chambre")
                CHECKLIST.forEachIndexed { i, item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = i in checked, onCheckedChange = { if (it) checked.add(i) else checked.remove(i) })
                        Text(item)
                    }
                }
            }

            SomniaCard {
                SectionLabel("Respirer avec Brumi · 3 minutes")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BreathPattern.entries.forEach { p ->
                        FilterChip(selected = pattern == p, onClick = { if (!running) pattern = p }, label = { Text(p.label) })
                    }
                }
                Text(pattern.description, style = MaterialTheme.typography.bodySmall, color = SomniaColors.Muted)
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CompanionView(
                        stage, if (done) CompanionMood.RADIANT else CompanionMood.CALM,
                        Modifier.size(170.dp).scale(0.8f + breath.value * 0.35f),
                    )
                }
                Text(label, Modifier.fillMaxWidth(), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center,
                    color = SomniaColors.Lavender)
                GameGauge(elapsed / totalSeconds.toFloat(), color = SomniaColors.Teal)
                if (!running && !done) GameButton("Commencer la respiration", onClick = { running = true }, modifier = Modifier.fillMaxWidth())
                if (running) Text("Laissez Brumi guider votre souffle…", Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall, color = SomniaColors.Muted)
            }

            SomniaCard {
                SectionLabel("Son d'endormissement (optionnel)")
                SoundscapePicker()
            }

            if (done) {
                Text("Rituel accompli : +10 XP si vous lancez votre nuit dans les 2 heures.",
                    style = MaterialTheme.typography.bodySmall, color = SomniaColors.Gold)
            }
            GameButton(
                "Lancer ma nuit", leading = "☾", height = 64.dp, modifier = Modifier.fillMaxWidth(),
                onClick = {
                    SleepTrackingService.start(context, profileWakeAlarm())
                    onBack()
                },
            )
        }
        SparkleBurst(if (done) "ritual" else null, Modifier.fillMaxSize())
    }
}
