package com.somnia.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.somnia.domain.sleep.SleepStage
import com.somnia.ui.theme.SomniaColors

/** Hypnogramme : une marche par minute, éveil en haut, profond en bas. */
@Composable
fun Hypnogram(stages: List<SleepStage>, modifier: Modifier = Modifier, startLabel: String = "", endLabel: String = "") {
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().height(140.dp)) {
            if (stages.isEmpty()) return@Canvas
            val rows = mapOf(SleepStage.AWAKE to 0, SleepStage.LIGHT to 1, SleepStage.DEEP to 2)
            val rowH = size.height / 3f
            for (r in 0..2) {
                drawLine(Color(0x22FFFFFF), Offset(0f, rowH * r + rowH / 2), Offset(size.width, rowH * r + rowH / 2),
                    strokeWidth = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 8f)))
            }
            val step = size.width / stages.size
            var i = 0
            while (i < stages.size) {
                val st = stages[i]
                var j = i
                while (j + 1 < stages.size && stages[j + 1] == st) j++
                val row = rows.getValue(st)
                drawRoundRect(
                    SomniaColors.stage(st),
                    topLeft = Offset(i * step, rowH * row + rowH * 0.2f),
                    size = Size(((j - i + 1) * step).coerceAtLeast(1.5f), rowH * 0.6f),
                    cornerRadius = CornerRadius(4f, 4f),
                )
                i = j + 1
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(startLabel, style = MaterialTheme.typography.labelSmall, color = SomniaColors.Muted)
            Text(endLabel, style = MaterialTheme.typography.labelSmall, color = SomniaColors.Muted)
        }
    }
}

@Composable
fun StageLegend() {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        SleepStage.entries.forEach { st ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).background(SomniaColors.stage(st), CircleShape))
                Spacer(Modifier.width(6.dp))
                Text(st.label, style = MaterialTheme.typography.bodySmall, color = SomniaColors.Muted)
            }
        }
    }
}

data class BarDatum(val label: String, val sleepMin: Int, val deepMin: Int, val score: Int?)

/** Barres hebdomadaires de sommeil, avec la zone recommandée 7–9 h en surbrillance. */
@Composable
fun WeeklyBars(data: List<BarDatum>, modifier: Modifier = Modifier) {
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().height(160.dp)) {
            val maxMin = 11 * 60f
            fun y(min: Float) = size.height - (min / maxMin) * size.height
            drawRect(SomniaColors.Teal.copy(alpha = 0.08f), Offset(0f, y(540f)), Size(size.width, y(420f) - y(540f)))
            drawLine(SomniaColors.Teal.copy(alpha = 0.4f), Offset(0f, y(420f)), Offset(size.width, y(420f)), 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 8f)))
            drawLine(SomniaColors.Teal.copy(alpha = 0.4f), Offset(0f, y(540f)), Offset(size.width, y(540f)), 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 8f)))
            if (data.isEmpty()) return@Canvas
            val slot = size.width / data.size
            val bw = slot * 0.5f
            data.forEachIndexed { i, d ->
                val x = i * slot + (slot - bw) / 2
                if (d.sleepMin > 0) {
                    val top = y(d.sleepMin.toFloat().coerceAtMost(maxMin))
                    drawRoundRect(SomniaColors.Light, Offset(x, top), Size(bw, size.height - top), CornerRadius(8f, 8f))
                    val deepTop = y(d.deepMin.toFloat())
                    drawRoundRect(SomniaColors.Deep, Offset(x, deepTop), Size(bw, size.height - deepTop), CornerRadius(8f, 8f))
                } else {
                    drawRoundRect(Color(0x22FFFFFF), Offset(x, size.height - 6f), Size(bw, 6f), CornerRadius(3f, 3f))
                }
            }
        }
        Row(Modifier.fillMaxWidth()) {
            data.forEach {
                Text(it.label, Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = SomniaColors.Muted,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
    }
}

/** Anneau de score animable. */
@Composable
fun ScoreRing(score: Int, modifier: Modifier = Modifier, progress: Float = score / 100f, color: Color = SomniaColors.Lavender) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val stroke = size.minDimension * 0.08f
            val d = size.minDimension - stroke
            val tl = Offset((size.width - d) / 2, (size.height - d) / 2)
            drawArc(Color(0x22FFFFFF), 135f, 270f, false, tl, Size(d, d), style = Stroke(stroke, cap = StrokeCap.Round))
            drawArc(color, 135f, 270f * progress.coerceIn(0f, 1f), false, tl, Size(d, d), style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$score", style = MaterialTheme.typography.displayMedium, color = SomniaColors.Moon)
            Text("SCORE", style = MaterialTheme.typography.labelSmall, color = SomniaColors.Muted)
        }
    }
}
