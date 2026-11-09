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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.platform.LocalContext
import com.somnia.BuildConfig
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import com.somnia.domain.game.EventCalendar
import com.somnia.alarm.Notifications
import com.somnia.alarm.SmartAlarmScheduler
import com.somnia.update.UpdateUi
import com.somnia.update.UpdateViewModel
import com.somnia.ui.components.SectionLabel
import com.somnia.ui.components.SomniaCard
import com.somnia.ui.components.TimeStepper
import com.somnia.ui.onboarding.ToggleRow
import com.somnia.ui.permissions.PermissionsPanel
import com.somnia.ui.theme.SomniaColors

@Composable
fun SettingsScreen(vm: SettingsViewModel, updates: UpdateViewModel, onBack: () -> Unit) {
    val p by vm.profile.collectAsStateWithLifecycle()
    val r by vm.reminders.collectAsStateWithLifecycle()
    val update by updates.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var devMessage by remember { mutableStateOf<String?>(null) }
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

        SomniaCard {
            SectionLabel("Rappels du soir")
            ToggleRow("Rappel à l'heure du coucher", r.bedtimeEnabled) { v -> vm.updateReminders { it.copy(bedtimeEnabled = v) } }
            if (r.bedtimeEnabled) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0 to "À l'heure", 15 to "15 min avant", 30 to "30 min avant").forEach { (m, label) ->
                    FilterChip(r.offsetMin == m, { vm.updateReminders { it.copy(offsetMin = m) } }, label = { Text(label) })
                }
            }
            ToggleRow("Rappel si j'éteins l'écran sans lancer le suivi", r.smartEnabled) { v -> vm.updateReminders { it.copy(smartEnabled = v) } }
            if (r.smartEnabled) Text(
                "De 30 min avant l'heure de coucher jusqu'à 2 h après : si l'écran reste éteint 2 minutes sans suivi en cours, " +
                    "Somnia vous envoie un rappel (une fois par nuit). Plus précis avec l'accès aux données d'utilisation.",
                style = MaterialTheme.typography.bodySmall, color = SomniaColors.Muted,
            )
            ToggleRow("Sonner même en mode vibreur", r.loud) { v -> vm.updateReminders { it.copy(loud = v) } }
            Text("Le rappel utilise alors le volume des alarmes au lieu de celui des notifications.",
                style = MaterialTheme.typography.bodySmall, color = SomniaColors.Muted)
        }

        SectionLabel("Autorisations")
        PermissionsPanel(micWanted = p.micEnabled)

        SomniaCard {
            SectionLabel("Développeur")
            Text("Version ${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})", style = MaterialTheme.typography.bodySmall, color = SomniaColors.Muted)
            OutlinedButton(onClick = {
                devMessage = if (Notifications.post(context, Notifications.ID_TEST, Notifications.test(context)))
                    "Notification envoyée ✓" else "Bloquée : autorisez les notifications ci-dessus."
            }, modifier = Modifier.fillMaxWidth()) { Text("Envoyer une notification de test") }
            OutlinedButton(onClick = {
                devMessage = if (Notifications.post(context, Notifications.ID_REMINDER, Notifications.bedtimeReminder(context, smart = true, loud = r.loud)))
                    "Rappel envoyé ✓" else "Bloquée : autorisez les notifications ci-dessus."
            }, modifier = Modifier.fillMaxWidth()) { Text("Tester le rappel du coucher") }
            OutlinedButton(onClick = {
                SmartAlarmScheduler(context).schedule(System.currentTimeMillis() + 10_000L)
                devMessage = "Le réveil va sonner dans 10 secondes. Vous pouvez verrouiller l'écran."
            }, modifier = Modifier.fillMaxWidth()) { Text("Tester le réveil dans 10 s") }
            SectionLabel("Événement forcé (test)")
            val forced by vm.eventOverride.collectAsStateWithLifecycle()
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(forced == null, { vm.setEventOverride(null) }, label = { Text("Calendrier") })
                EventCalendar.all.forEach { e ->
                    FilterChip(forced == e.id, { vm.setEventOverride(e.id) }, label = { Text("${e.symbol} ${e.name}") })
                }
            }
            OutlinedButton(onClick = { vm.addStardust(); devMessage = "+500 ✦ ajoutés" }, modifier = Modifier.fillMaxWidth()) {
                Text("Ajouter 500 poussières d'étoiles")
            }
            OutlinedButton(onClick = { updates.check(manual = true) }, modifier = Modifier.fillMaxWidth()) {
                Text("Vérifier les mises à jour")
            }
            val updateText = when (val u = update) {
                UpdateUi.Checking -> "Vérification…"
                is UpdateUi.UpToDate -> if (u.manual) "Vous avez la dernière version ✓" else null
                is UpdateUi.Available -> "Build ${u.info.versionCode} disponible : touchez le bandeau en haut."
                is UpdateUi.Error -> if (u.manual) "Mises à jour : ${u.message}" else null
                else -> null
            }
            devMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = SomniaColors.Teal) }
            updateText?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = SomniaColors.Teal) }
        }

        SomniaCard {
            SectionLabel("Sauvegarde")
            val backupMsg by vm.backupMessage.collectAsStateWithLifecycle()
            val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
                uri?.let(vm::exportTo)
            }
            var pendingRestore by remember { mutableStateOf<android.net.Uri?>(null) }
            val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> pendingRestore = uri }
            Text(
                "Une copie de vos données est enregistrée chaque jour et après chaque nuit dans Téléchargements › Somnia. " +
                    "Ce dossier est conservé si vous désinstallez l'app : au prochain lancement, choisissez « Restaurer une sauvegarde ».",
                style = MaterialTheme.typography.bodySmall, color = SomniaColors.Muted,
            )
            val last = vm.lastBackupAt
            if (last > 0) Text(
                "Dernière sauvegarde : " + java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT, java.text.DateFormat.SHORT).format(java.util.Date(last)),
                style = MaterialTheme.typography.bodySmall, color = SomniaColors.Muted,
            )
            OutlinedButton(onClick = vm::backupNow, modifier = Modifier.fillMaxWidth()) { Text("Sauvegarder maintenant") }
            OutlinedButton(onClick = { exporter.launch("somnia-sauvegarde.zip") }, modifier = Modifier.fillMaxWidth()) { Text("Exporter vers…") }
            OutlinedButton(onClick = { importer.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) }, modifier = Modifier.fillMaxWidth()) {
                Text("Restaurer depuis un fichier…")
            }
            backupMsg?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = SomniaColors.Teal) }
            pendingRestore?.let { uri ->
                AlertDialog(
                    onDismissRequest = { pendingRestore = null },
                    title = { Text("Restaurer cette sauvegarde ?") },
                    text = { Text("Vos données actuelles seront remplacées par celles de la sauvegarde, puis l'app redémarrera.") },
                    confirmButton = { TextButton(onClick = { pendingRestore = null; vm.restoreFrom(uri) }) { Text("Restaurer") } },
                    dismissButton = { TextButton(onClick = { pendingRestore = null }) { Text("Annuler") } },
                )
            }
        }

        SomniaCard {
            SectionLabel("Confidentialité")
            Text("Toutes vos données sont stockées sur ce téléphone (base locale), sans compte Somnia. La sauvegarde automatique d'Android peut aussi les copier, chiffrées, sur votre compte Google. Internet ne sert qu'à vérifier et télécharger les mises à jour depuis GitHub : aucune donnée n'est envoyée.",
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
