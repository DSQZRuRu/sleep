package com.somnia.ui.tracking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.somnia.data.repository.Clock
import com.somnia.domain.game.CompanionMood
import com.somnia.domain.game.CompanionStage
import com.somnia.domain.sleep.TimeMath
import com.somnia.tracking.SleepTrackingService
import com.somnia.tracking.TrackingStatus
import com.somnia.ui.components.CompanionView
import com.somnia.ui.theme.SomniaColors
import kotlinx.coroutines.delay

/**
 * Écran affiché pendant la nuit. Très sombre et statique : il n'est PAS nécessaire
 * de le garder allumé — le suivi continue écran éteint.
 */
@Composable
fun TrackingScreen(status: TrackingStatus, stage: CompanionStage) {
    val context = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var confirm by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) { now = System.currentTimeMillis(); delay(30_000) }
    }

    Column(
        Modifier.fillMaxSize().background(Color.Black).statusBarsPadding().navigationBarsPadding().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (status.finishing) {
            CircularProgressIndicator(color = SomniaColors.Lavender)
            Spacer(Modifier.height(16.dp))
            Text("Analyse de votre nuit…", color = SomniaColors.Muted)
            return@Column
        }
        CompanionView(stage, CompanionMood.SLEEPY, Modifier.size(120.dp))
        Spacer(Modifier.height(24.dp))
        Text(TimeMath.format(Clock.minuteOfDay(now)), style = MaterialTheme.typography.displayLarge, color = SomniaColors.Muted)
        status.startedAt?.let {
            Text("Nuit commencée il y a ${TimeMath.formatDuration(((now - it) / 60_000L).toInt())}", color = Color(0xFF8A90B4))
        }
        status.alarmAt?.let {
            Text("Réveil au plus tard à ${TimeMath.format(Clock.minuteOfDay(it))}", color = Color(0xFF8A90B4))
        }
        if (status.micActive) Text("Niveau sonore analysé localement, sans enregistrement", style = MaterialTheme.typography.bodySmall, color = Color(0xFF6E7499))
        Spacer(Modifier.height(32.dp))
        Text("Vous pouvez éteindre l'écran.\nBrumi veille avec vous. Bonne nuit ✦",
            color = Color(0xFF8A90B4), style = MaterialTheme.typography.bodyMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        com.somnia.sound.SoundscapePicker(dim = true)
        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = { confirm = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Terminer la nuit", color = SomniaColors.Muted)
        }
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Terminer la nuit ?") },
            text = { Text("Votre nuit sera analysée et les récompenses révélées. Moins de 20 minutes de suivi ne sont pas conservées.") },
            confirmButton = {
                TextButton(onClick = {
                    confirm = false
                    context.startService(SleepTrackingService.stopIntent(context))
                }) { Text("Terminer") }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Continuer à dormir") } },
        )
    }
}
