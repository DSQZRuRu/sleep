package com.somnia.sound

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.somnia.data.repository.Clock
import com.somnia.domain.sleep.TimeMath
import com.somnia.ui.theme.SomniaColors

/** Choix d'un son d'endormissement et de sa durée. */
@Composable
fun SoundscapePicker(modifier: Modifier = Modifier, dim: Boolean = false) {
    val state by Soundscape.state.collectAsStateWithLifecycle()
    var minutes by remember { mutableIntStateOf(30) }
    val muted = if (dim) SomniaColors.Muted.copy(alpha = 0.6f) else SomniaColors.Muted
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SoundKind.entries.forEach { k ->
                FilterChip(
                    selected = state.playing == k,
                    onClick = { if (state.playing == k) Soundscape.stop() else Soundscape.play(k, minutes) },
                    label = { Text("${k.emoji} ${k.label}") },
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Durée", style = MaterialTheme.typography.bodySmall, color = muted)
            listOf(15, 30, 45, 60).forEach { m ->
                FilterChip(selected = minutes == m, onClick = {
                    minutes = m
                    state.playing?.let { Soundscape.play(it, m) }
                }, label = { Text("$m min") })
            }
        }
        state.endsAt?.let {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("S'arrête en douceur vers ${TimeMath.format(Clock.minuteOfDay(it))}", style = MaterialTheme.typography.bodySmall, color = muted)
                TextButton(onClick = { Soundscape.stop() }) { Text("Arrêter") }
            }
        }
    }
}
