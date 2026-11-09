package com.somnia.ui.tips

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.somnia.data.db.ProfileEntity
import com.somnia.data.repository.ProfileRepository
import com.somnia.domain.tips.SleepTipsCatalog
import com.somnia.ui.components.SectionLabel
import com.somnia.ui.components.SomniaCard
import com.somnia.ui.theme.SomniaColors
import kotlinx.coroutines.flow.Flow

@Composable
fun TipsScreen(profile: Flow<ProfileEntity>) {
    val p by profile.collectAsStateWithLifecycle(ProfileEntity())
    val chrono = ProfileRepository.chronotype(p)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("Conseils", style = MaterialTheme.typography.headlineMedium) }
        item {
            SomniaCard {
                SectionLabel("Pour vous")
                Text("Chronotype : ${chrono.label}", style = MaterialTheme.typography.titleMedium, color = SomniaColors.Gold)
                Text(
                    if (chrono.bedtimeShiftMin > 0)
                        "Votre horloge tend vers le soir. Une exposition à la lumière dès le lever et des écrans tamisés le soir aident à avancer l'endormissement en douceur."
                    else if (chrono.bedtimeShiftMin < 0)
                        "Votre horloge tend vers le matin. Gardez une heure de lever stable le week-end pour ne pas vous décaler."
                    else "Votre horloge est flexible : la régularité des horaires reste votre meilleure alliée.",
                    color = SomniaColors.Muted,
                )
                if (p.latencyMin >= 30) Text(
                    "Vous mettez en moyenne ${p.latencyMin} min à vous endormir. Si cela dure, essayez de ne vous coucher qu'à l'arrivée de la somnolence (voir « Le lit, c'est pour dormir »).",
                    color = SomniaColors.Muted,
                )
            }
        }
        items(SleepTipsCatalog.all, key = { it.id }) { tip ->
            SomniaCard {
                SectionLabel(tip.category)
                Text(tip.title, style = MaterialTheme.typography.titleMedium)
                Text(tip.body, color = SomniaColors.Muted)
                Text("Source : ${tip.source}", style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic, color = SomniaColors.Muted)
            }
        }
        item {
            Text("Somnia n'est pas un dispositif médical. En cas de troubles du sommeil persistants (insomnie, ronflements importants, somnolence diurne), consultez un professionnel de santé.",
                style = MaterialTheme.typography.bodySmall, color = SomniaColors.Muted)
        }
    }
}
