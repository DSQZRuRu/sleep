package com.somnia.ui.game

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.somnia.ui.theme.SomniaColors

/** Bouton de jeu « 3D » : une tranche plus sombre sous la face, qui s'enfonce à l'appui. */
@Composable
fun GameButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = SomniaColors.Lavender,
    textColor: Color = SomniaColors.Night,
    enabled: Boolean = true,
    height: Dp = 56.dp,
    leading: String? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val depth = 6.dp
    val press by animateDpAsState(if (pressed && enabled) depth - 1.dp else 0.dp, spring(stiffness = 2000f), label = "press")
    val shape = RoundedCornerShape(height / 2)
    val face = if (enabled) color else Color(0xFF3B4470)
    Box(
        modifier.height(height + depth).alpha(if (enabled) 1f else 0.7f)
            .clickable(interaction, indication = null, enabled = enabled, onClick = onClick),
    ) {
        Box(
            Modifier.fillMaxWidth().height(height).offset(y = depth)
                .background(lerp(face, Color.Black, 0.45f), shape),
        )
        Box(
            Modifier.fillMaxWidth().height(height).offset(y = press)
                .background(Brush.verticalGradient(listOf(lerp(face, Color.White, 0.35f), face)), shape)
                .border(1.5.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.6f), Color.Transparent)), shape),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                leading?.let { Text(it, color = textColor, style = MaterialTheme.typography.titleMedium); Spacer(Modifier.width(8.dp)) }
                Text(text, color = if (enabled) textColor else SomniaColors.Muted, style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center)
            }
        }
    }
}

/** Pastille de ressource (poussière, série…). */
@Composable
fun ResourceChip(symbol: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Row(
        modifier.background(Color(0xCC141A36), RoundedCornerShape(50))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(symbol, color = color, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.width(5.dp))
        Text(value, color = SomniaColors.Moon, style = MaterialTheme.typography.labelLarge)
    }
}

/** Barre de progression épaisse avec reflet, façon jauge de jeu. */
@Composable
fun GameGauge(progress: Float, modifier: Modifier = Modifier, color: Color = SomniaColors.Gold, height: Dp = 12.dp) {
    val shape = RoundedCornerShape(height / 2)
    Box(modifier.fillMaxWidth().height(height).clip(shape).background(Color(0x55000000)).border(1.dp, Color(0x33FFFFFF), shape)) {
        Box(
            Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(height).clip(shape)
                .background(Brush.verticalGradient(listOf(lerp(color, Color.White, 0.4f), color, lerp(color, Color.Black, 0.2f)))),
        )
        Box(Modifier.fillMaxWidth().height(height / 3).padding(horizontal = 6.dp).offset(y = 2.dp)
            .background(Color.White.copy(alpha = 0.18f), shape))
    }
}

/** Médaillon de niveau. */
@Composable
fun LevelBadge(level: Int, modifier: Modifier = Modifier, size: Dp = 30.dp) {
    Box(
        modifier.size(size).background(Brush.verticalGradient(listOf(Color(0xFFFFE6A8), SomniaColors.Gold, Color(0xFFE0A33A))), CircleShape)
            .border(2.dp, SomniaColors.Night, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text("$level", color = SomniaColors.Night, style = MaterialTheme.typography.labelLarge)
    }
}

/** En-tête de section façon ruban. */
@Composable
fun SectionBanner(title: String, modifier: Modifier = Modifier, trailing: @Composable (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(6.dp).background(SomniaColors.Gold, CircleShape))
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

