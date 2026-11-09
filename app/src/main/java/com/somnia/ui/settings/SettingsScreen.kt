package com.somnia.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.somnia.ui.components.SectionLabel
import com.somnia.ui.components.SomniaCard
import com.somnia.ui.components.TimeStepper
import com.somnia.ui.onboarding.ToggleRow
import com.somnia.ui.permissions.PermissionsPanel
import com.somnia.ui.theme.SomniaColors

@Composable
fun SettingsScreen(vm: SettingsViewModel, onBack: () -> Unit) {
    val p by vm.profile.collectAsStateWithLifecycle()
    var confirmErase by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour") }
            Text("Réglages", style = MaterialTheme.typography.headlineMedium)
        }

        SomniaCard {
            SectionLabel("Heure de lever")
            TimeStepper(p.targetWakeMin, { m -> vm.update { it.copy(targetWakeMin = m) } }, step = 5)
            SectionLabel("Réveil intelligent")
            ToggleRow("Sonner en sommeil léger avant l'heure", p.smartAlarmEnabled) { v -> vm.update { it.copy(smartAlarmEnabled = v) } }
            if (p.smartAlarmEnabled) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(10, 20, 30).forEach { w ->
                    FilterChip(p.smartWindowMin == w, { vm.update { it.copy(smartWindowMin = w) } }, label = { Text("$w min") })
                }
            }
        }

        SomniaCard {
            SectionLabel("Capteurs")
            ToggleRow("Analyser le niveau sonore", p.micEnabled) { v -> vm.update { it.copy(micEnabled = v) } }
            Text("Seul un niveau sonore est calculé en local, chaque demi-seconde. Aucun audio n'est enregistré, stocké ni envoyé.",
                style = MaterialTheme.typography.bodySmall, color = SomniaColors.Muted)
        }

        SomniaCard {
            SectionLabel("Profil appris")
            Text("Endormissement : ${p.latencyMin} min · cycle : ${p.cycleMin} min · objectif : ${p.preferredCycles} cycles")
            Text("Ajusté sur ${p.nightsLearned} nuit${if (p.nightsLearned > 1) "s" else ""}.", style = MaterialTheme.typography.bodySmall, color = SomniaColors.Muted)
            OutlinedButton(onClick = vm::redoQuestionnaire) { Text("Refaire le questionnaire") }
        }

        SectionLabel("Autorisations")
        PermissionsPanel(micWanted = p.micEnabled)

        SomniaCard {
            SectionLabel("Confidentialité")
            Text("Toutes vos données sont stockées uniquement sur ce téléphone (base locale). Somnia n'a pas d'accès Internet et n'utilise aucun compte.",
                style = MaterialTheme.typography.bodySmall, color = SomniaColors.Muted)
            TextButton(onClick = { confirmErase = true }) { Text("Effacer toutes mes données", color = SomniaColors.Awake) }
        }
    }

    if (confirmErase) {
        AlertDialog(
            onDismissRequest = { confirmErase = false },
            title = { Text("Tout effacer ?") },
            text = { Text("Nuits, profil, créatures, badges et progression seront définitivement supprimés.") },
            confirmButton = { TextButton(onClick = { confirmErase = false; vm.eraseAll() }) { Text("Effacer") } },
            dismissButton = { TextButton(onClick = { confirmErase = false }) { Text("Annuler") } },
        )
    }
}
