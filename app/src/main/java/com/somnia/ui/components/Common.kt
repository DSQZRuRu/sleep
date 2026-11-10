package com.somnia.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.somnia.ui.theme.SomniaColors

/** Panneau de jeu : verre sombre, liseré lumineux et reflet en haut. */
@Composable
fun SomniaCard(modifier: Modifier = Modifier, accent: Color = SomniaColors.Lavender, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(26.dp)
    Column(
        modifier.fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color(0xEB1D2547), Color(0xEB121734))), shape)
            .border(1.5.dp, Brush.verticalGradient(listOf(accent.copy(alpha = 0.55f), accent.copy(alpha = 0.08f))), shape)
            .clip(shape)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), modifier, style = MaterialTheme.typography.labelSmall, color = SomniaColors.Muted)
}

@Composable
fun StatItem(label: String, value: String, modifier: Modifier = Modifier, color: Color = SomniaColors.Moon) {
    Column(modifier, horizontalAlignment = Alignment.Start) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = color)
        Text(label, style = MaterialTheme.typography.bodySmall, color = SomniaColors.Muted)
    }
}

@Composable
fun ProgressBar(progress: Float, modifier: Modifier = Modifier, color: Color = SomniaColors.Lavender) {
    com.somnia.ui.game.GameGauge(progress, modifier, color, height = 10.dp)
}

@Composable
fun StatRow(content: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { content() }
}
