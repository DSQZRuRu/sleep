package com.somnia.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.somnia.domain.game.CompanionMood
import com.somnia.domain.game.CompanionStage
import com.somnia.domain.sleep.ChronotypeQuestionnaire
import com.somnia.domain.sleep.TimeMath
import com.somnia.ui.components.CompanionView
import com.somnia.ui.components.ProgressBar
import com.somnia.ui.components.SectionLabel
import com.somnia.ui.components.SomniaCard
import com.somnia.ui.components.TimeStepper
import com.somnia.ui.permissions.PermissionsPanel
import com.somnia.ui.theme.SomniaColors

private const val STEPS = 5

@Composable
fun OnboardingScreen(vm: OnboardingViewModel, onDone: () -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    BackHandler(enabled = s.step > 0) { vm.back() }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp)) {
        ProgressBar((s.step + 1f) / STEPS)
        Spacer(Modifier.height(24.dp))
        AnimatedContent(s.step, Modifier.weight(1f), transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "step") { step ->
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                when (step) {
                    0 -> Welcome(vm)
                    1 -> WakeStep(s, vm)
                    2 -> ChronoStep(s, vm)
                    3 -> ResultStep(s)
                    else -> PermissionStep(s, vm)
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            if (s.step > 0) TextButton(onClick = vm::back) { Text("Retour") } else Spacer(Modifier.size(1.dp))
            val enabled = s.step != 2 || s.chronoComplete
            Button(
                onClick = {
                    if (s.step == STEPS - 1) { vm.finish(); onDone() } else vm.next()
                },
                enabled = enabled,
            ) { Text(if (s.step == STEPS - 1) "Commencer" else "Continuer") }
        }
    }
}

@Composable
private fun ColumnScope.Welcome(vm: OnboardingViewModel) {
    val restoring by vm.restoring.collectAsStateWithLifecycle()
    val error by vm.restoreError.collectAsStateWithLifecycle()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(vm::restore) }
    CompanionView(CompanionStage.EGG, CompanionMood.CALM, Modifier.fillMaxWidth().height(200.dp))
    Text("Bienvenue dans Somnia", style = MaterialTheme.typography.headlineMedium)
    Text(
        "Somnia suit votre sommeil avec votre téléphone seul, posé sur le matelas. " +
            "Toutes vos données restent sur votre appareil.",
        color = SomniaColors.Muted,
    )
    Text(
        "Cet œuf de brume, c'est Brumi. Il éclora et grandira au rythme de vos nuits régulières.",
        color = SomniaColors.Muted,
    )
    Text("Quelques questions pour créer votre profil de sommeil.", style = MaterialTheme.typography.titleMedium)
    SomniaCard {
        SectionLabel("Vous avez déjà utilisé Somnia ?")
        Text("Restaurez votre sauvegarde : elle se trouve dans Téléchargements › Somnia (somnia-sauvegarde-….zip).",
            style = MaterialTheme.typography.bodySmall, color = SomniaColors.Muted)
        OutlinedButton(onClick = { picker.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) }, enabled = !restoring) {
            Text(if (restoring) "Restauration…" else "Restaurer une sauvegarde")
        }
        error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = SomniaColors.Awake) }
    }
}

@Composable
private fun ColumnScope.WakeStep(s: OnboardingState, vm: OnboardingViewModel) {
    Text("À quelle heure souhaitez-vous vous lever ?", style = MaterialTheme.typography.headlineMedium)
    TimeStepper(s.wakeMin, { m -> vm.update { it.copy(wakeMin = m) } }, Modifier.align(Alignment.CenterHorizontally))

    SectionLabel("Temps habituel pour vous endormir")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(5, 15, 25, 40).forEach { m ->
            FilterChip(selected = s.latencyMin == m, onClick = { vm.update { it.copy(latencyMin = m) } },
                label = { Text(if (m == 40) "40+ min" else "$m min") })
        }
    }

    SectionLabel("Contraintes")
    SomniaCard {
        ToggleRow("Je ne peux pas me coucher avant…", s.hasEarliest) { v -> vm.update { it.copy(hasEarliest = v) } }
        if (s.hasEarliest) TimeStepper(s.earliestBedMin, { m -> vm.update { it.copy(earliestBedMin = m) } })
        ToggleRow("Je veux me coucher au plus tard à…", s.hasLatest) { v -> vm.update { it.copy(hasLatest = v) } }
        if (s.hasLatest) TimeStepper(s.latestBedMin, { m -> vm.update { it.copy(latestBedMin = m) } })
        ToggleRow("Mes horaires changent le week-end", s.weekendDifferent) { v -> vm.update { it.copy(weekendDifferent = v) } }
    }
}

@Composable
private fun ColumnScope.ChronoStep(s: OnboardingState, vm: OnboardingViewModel) {
    Text("Votre chronotype", style = MaterialTheme.typography.headlineMedium)
    Text("Inspiré du questionnaire matin/soir de Horne & Östberg (1976). Répondez selon vos préférences naturelles.",
        style = MaterialTheme.typography.bodySmall, color = SomniaColors.Muted)
    ChronotypeQuestionnaire.questions.forEach { q ->
        SomniaCard {
            Text(q.text, style = MaterialTheme.typography.titleMedium)
            q.options.forEach { (label, value) ->
                FilterChip(
                    selected = s.answers[q.id] == value,
                    onClick = { vm.answer(q.id, value) },
                    label = { Text(label) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.ResultStep(s: OnboardingState) {
    val type = s.chronotype
    Text("Votre profil", style = MaterialTheme.typography.headlineMedium)
    SomniaCard {
        SectionLabel("Chronotype")
        Text(type.label, style = MaterialTheme.typography.titleLarge, color = SomniaColors.Gold)
        Text(type.description, color = SomniaColors.Muted)
    }
    SomniaCard {
        SectionLabel("Heures de coucher pour un lever à ${TimeMath.format(s.wakeMin)}")
        Text("Basées sur des cycles d'environ 90 min et ${s.latencyMin} min d'endormissement. " +
            "Les adultes ont besoin de 7 à 9 h de sommeil.", style = MaterialTheme.typography.bodySmall, color = SomniaColors.Muted)
        s.options().forEach { o ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(TimeMath.format(o.bedtimeMin), style = MaterialTheme.typography.titleLarge,
                    color = if (o.ideal) SomniaColors.Lavender else SomniaColors.Moon)
                Spacer(Modifier.size(16.dp))
                Column(Modifier.weight(1f)) {
                    Text("${o.cycles} cycles · ${TimeMath.formatDuration(o.sleepDurationMin)}")
                    val note = when {
                        o.ideal -> "Recommandé pour vous"
                        !o.recommended -> "Hors de la plage 7–9 h"
                        !o.fitsConstraints -> "Hors de vos contraintes"
                        else -> "Possible"
                    }
                    Text(note, style = MaterialTheme.typography.bodySmall,
                        color = if (o.ideal) SomniaColors.Teal else SomniaColors.Muted)
                }
            }
        }
    }
    if (type.bedtimeShiftMin > 0) {
        Text("Profil du soir : si l'heure recommandée vous semble tôt, avancez-la progressivement de 15 min tous les quelques jours, en prenant la lumière du matin.",
            style = MaterialTheme.typography.bodySmall, color = SomniaColors.Muted)
    }
    Text("Le profil s'affinera nuit après nuit : temps d'endormissement réel, durée de vos cycles et ressenti au réveil.",
        style = MaterialTheme.typography.bodySmall, color = SomniaColors.Muted)
}

@Composable
private fun ColumnScope.PermissionStep(s: OnboardingState, vm: OnboardingViewModel) {
    Text("Quelques autorisations", style = MaterialTheme.typography.headlineMedium)
    SomniaCard {
        ToggleRow("Analyser aussi le son (ronflements, mouvements)", s.micEnabled) { v -> vm.update { it.copy(micEnabled = v) } }
        Text("Le micro mesure seulement un niveau sonore, calculé et oublié chaque demi-seconde. Aucun audio n'est enregistré ni envoyé.",
            style = MaterialTheme.typography.bodySmall, color = SomniaColors.Muted)
    }
    PermissionsPanel(micWanted = s.micEnabled)
}

@Composable
fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
