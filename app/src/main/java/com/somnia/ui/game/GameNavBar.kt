package com.somnia.ui.game

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import com.somnia.ui.theme.SomniaColors

data class NavItem(val label: String, val icon: Painter, val selected: Boolean, val onClick: () -> Unit)

/** Barre d'onglets flottante façon jeu. */
@Composable
fun GameNavBar(items: List<NavItem>, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 14.dp, vertical = 8.dp)
            .background(Brush.verticalGradient(listOf(Color(0xF21D2547), Color(0xF2111633))), RoundedCornerShape(28.dp))
            .border(1.dp, Brush.verticalGradient(listOf(SomniaColors.Lavender.copy(alpha = 0.55f), Color.Transparent)), RoundedCornerShape(28.dp))
            .padding(6.dp),
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        items.forEach { item ->
            val bg by animateColorAsState(if (item.selected) SomniaColors.Lavender.copy(alpha = 0.22f) else Color.Transparent, label = "bg")
            val tint by animateColorAsState(if (item.selected) SomniaColors.Gold else SomniaColors.Muted, label = "tint")
            val scale by animateFloatAsState(if (item.selected) 1.12f else 1f, label = "scale")
            Column(
                Modifier.weight(1f).background(bg, RoundedCornerShape(22.dp))
                    .clickable(remember { MutableInteractionSource() }, indication = null, onClick = item.onClick)
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.size(26.dp).scale(scale), contentAlignment = Alignment.Center) {
                    Icon(item.icon, item.label, tint = tint)
                }
                Text(item.label, style = MaterialTheme.typography.labelSmall, color = tint)
            }
        }
    }
}
