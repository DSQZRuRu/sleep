package com.somnia.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.somnia.domain.sleep.TimeMath
import com.somnia.ui.theme.SomniaColors

/** Sélecteur d'heure minimaliste par pas de 15 minutes. */
@Composable
fun TimeStepper(minutes: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier, step: Int = 15) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        FilledTonalIconButton(onClick = { onChange(TimeMath.normalize(minutes - step)) }) { Text("−") }
        Text(
            TimeMath.format(minutes),
            Modifier.padding(horizontal = 20.dp),
            style = MaterialTheme.typography.displayMedium,
            color = SomniaColors.Moon,
        )
        FilledTonalIconButton(onClick = { onChange(TimeMath.normalize(minutes + step)) }) { Text("+") }
    }
}
