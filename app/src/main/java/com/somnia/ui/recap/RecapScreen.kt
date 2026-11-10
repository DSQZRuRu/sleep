package com.somnia.ui.recap

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.somnia.AppContainer
import com.somnia.data.repository.Clock
import com.somnia.data.repository.toSummary
import com.somnia.domain.game.CompanionLogic
import com.somnia.domain.game.CompanionMood
import com.somnia.domain.game.CompanionStage
import com.somnia.domain.game.WeekKey
import com.somnia.domain.game.WeeklyRecap
import com.somnia.domain.game.WeeklyRecapCalculator
import com.somnia.domain.sleep.TimeMath
import com.somnia.ui.components.CompanionView
import com.somnia.ui.components.SectionLabel
import com.somnia.ui.components.SomniaCard
import com.somnia.ui.components.StatItem
import com.somnia.ui.game.SparkleBurst
import com.somnia.ui.history.nightTitle
import com.somnia.ui.theme.SomniaColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

data class RecapUi(
    val recap: WeeklyRecap? = null,
    val bestTitle: String? = null,
    val range: String = "",
    val stage: CompanionStage = CompanionStage.EGG,
    val mood: CompanionMood = CompanionMood.CALM,
)

/** Bilan d'une semaine (lundi → dimanche). [weeksAgo] = 0 pour la semaine en cours. */
class RecapViewModel(private val c: AppContainer, private val weeksAgo: Int) : ViewModel() {
    private val _ui = MutableStateFlow(RecapUi())
    val ui: StateFlow<RecapUi> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            val monday = WeekKey.mondayOf(Clock.today()) - 7L * weeksAgo
            val from = Clock.startOfDayMillis(monday) + 12 * 3_600_000L
            val to = from + 7 * 86_400_000L
            val sessions = c.sleepRepository.processedBetween(from, to)
            val catches = c.database.gameDao().catchesBetween(from, to + 12 * 3_600_000L)
            val recap = WeeklyRecapCalculator.compute(WeekKey.of(monday), sessions.map { it.toSummary() }, catches.size)
            val best = recap.best?.let { b -> sessions.firstOrNull { it.id == b.sessionId } }
            val game = c.gameRepository.state.first()
            val fmt = DateTimeFormatter.ofPattern("d MMM", Locale.FRENCH)
            _ui.value = RecapUi(
                recap = recap,
                bestTitle = best?.let { nightTitle(it) },
                range = "Du ${LocalDate.ofEpochDay(monday).format(fmt)} au ${LocalDate.ofEpochDay(monday + 6).format(fmt)}",
                stage = CompanionStage.forCare(game.companionCare),
                mood = CompanionLogic.mood(sessions.takeLast(3).map { it.score }),
            )
        }
    }
}

@Composable
fun RecapScreen(vm: RecapViewModel, onBack: () -> Unit) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val r = ui.recap
    var phase by remember { mutableIntStateOf(0) }
    val score = remember { Animatable(0f) }
    LaunchedEffect(r) {
        if (r == null) return@LaunchedEffect
        for (p in 1..4) { delay(350); phase = p }
        score.animateTo(r.avgScore.toFloat(), tween(1000))
    }
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour") }
                Column {
                    Text("Bilan de la semaine", style = MaterialTheme.typography.headlineMedium)
                    Text(ui.range, style = MaterialTheme.typography.bodySmall, color = SomniaColors.Muted)
                }
            }
            if (r == null) return@Column
            CompanionView(ui.stage, ui.mood, Modifier.size(140.dp))
            Text(r.message, textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium)
            AnimatedVisibility(phase >= 1, enter = fadeIn() + slideInVertically { it / 3 }) {
                SomniaCard {
                    SectionLabel("Vos nuits")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        StatItem("Nuits suivies", "${r.nights}")
                        StatItem("Sommeil moyen", if (r.nights > 0) TimeMath.formatDuration(r.avgSleepMin) else "—")
                        StatItem("Score moyen", "${score.value.toInt()}", color = SomniaColors.Lavender)
                    }
                }
            }
            AnimatedVisibility(phase >= 2, enter = fadeIn() + slideInVertically { it / 3 }) {
                SomniaCard {
                    SectionLabel("Habitudes")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        StatItem("Nuits de 7 à 9 h", "${r.healthyNights}", color = SomniaColors.Teal)
                        StatItem("Régularité", r.regularity?.toString() ?: "—", color = SomniaColors.Teal)
                    }
                }
            }
            AnimatedVisibility(phase >= 3, enter = fadeIn() + slideInVertically { it / 3 }) {
                SomniaCard {
                    SectionLabel("Meilleure nuit")
                    Text(ui.bestTitle?.replaceFirstChar { it.uppercase() } ?: "—", style = MaterialTheme.typography.titleMedium)
                    r.best?.let { Text("Score ${it.score} · ${TimeMath.formatDuration(it.totalSleepMin)}", color = SomniaColors.Muted) }
                }
            }
            AnimatedVisibility(phase >= 4, enter = fadeIn() + slideInVertically { it / 3 }) {
                SomniaCard {
                    SectionLabel("Monde des rêves")
                    Text("${r.creaturesMet} créature${if (r.creaturesMet > 1) "s" else ""} du rêve rencontrée${if (r.creaturesMet > 1) "s" else ""}",
                        style = MaterialTheme.typography.titleMedium, color = SomniaColors.Gold)
                }
            }
        }
        SparkleBurst(if (phase >= 4 && (r?.healthyNights ?: 0) >= 3) "recap" else null, Modifier.fillMaxSize())
    }
}
