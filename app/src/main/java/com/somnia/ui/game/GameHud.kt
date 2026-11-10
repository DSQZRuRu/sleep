package com.somnia.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.somnia.domain.game.CompanionMood
import com.somnia.domain.game.CompanionStage
import com.somnia.domain.game.XpCalculator
import com.somnia.ui.components.CompanionView
import com.somnia.ui.theme.SomniaColors

/** Barre de statut du joueur : avatar + niveau, jauge d'XP, série et poussière d'étoiles. */
@Composable
fun GameHud(
    level: XpCalculator.LevelInfo,
    streak: Int,
    stardust: Int,
    stage: CompanionStage,
    mood: CompanionMood,
    modifier: Modifier = Modifier,
    onSettings: (() -> Unit)? = null,
) {
    Row(
        modifier.fillMaxWidth()
            .background(Brush.horizontalGradient(listOf(Color(0xE61D2547), Color(0xCC141A36))), RoundedCornerShape(24.dp))
            .border(1.dp, Brush.horizontalGradient(listOf(SomniaColors.Lavender.copy(alpha = 0.5f), Color.Transparent)), RoundedCornerShape(24.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            Box(
                Modifier.size(52.dp).background(Brush.radialGradient(listOf(Color(0xFF3A4590), SomniaColors.Night)), CircleShape)
                    .border(2.dp, SomniaColors.Lavender.copy(alpha = 0.7f), CircleShape),
                contentAlignment = Alignment.Center,
            ) { CompanionView(stage, mood, Modifier.size(46.dp)) }
            LevelBadge(level.level, Modifier.align(Alignment.BottomEnd).offset(x = 6.dp, y = 4.dp), size = 24.dp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Niveau ${level.level}", style = MaterialTheme.typography.titleSmall)
            GameGauge(level.xpIntoLevel / level.xpForLevel.toFloat(), Modifier.padding(vertical = 3.dp), height = 10.dp)
            Text("${level.xpIntoLevel} / ${level.xpForLevel} XP", style = MaterialTheme.typography.labelSmall, color = SomniaColors.Muted)
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ResourceChip("🔥", "$streak", Color(0xFFFF9A5C))
            ResourceChip("✦", "$stardust", SomniaColors.Gold)
        }
        onSettings?.let {
            Box(
                Modifier.padding(start = 6.dp).size(36.dp).background(Color(0x33FFFFFF), CircleShape).clickable(onClick = it),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.Settings, "Réglages", tint = SomniaColors.Moon, modifier = Modifier.size(20.dp)) }
        }
    }
}
