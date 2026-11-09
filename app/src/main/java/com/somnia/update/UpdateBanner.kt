package com.somnia.update

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.somnia.ui.theme.SomniaColors

/** Bandeau en haut de l'app quand une nouvelle version est disponible. */
@Composable
fun UpdateBanner(vm: UpdateViewModel) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val state = ui
    val visible = state is UpdateUi.Available || state is UpdateUi.Downloading || state is UpdateUi.Ready ||
        (state is UpdateUi.Error && state.info != null)
    if (!visible) return

    fun installOrAskPermission(s: UpdateUi.Ready) {
        if (UpdateInstaller.canInstall(context)) UpdateInstaller.install(context, s.file)
        else context.startActivity(UpdateInstaller.unknownSourcesSettings(context))
    }

    Column(
        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 6.dp)
            .background(SomniaColors.Lavender, RoundedCornerShape(16.dp))
            .clickable {
                when (state) {
                    is UpdateUi.Available -> vm.download(state.info)
                    is UpdateUi.Ready -> installOrAskPermission(state)
                    is UpdateUi.Error -> state.info?.let { vm.download(it) }
                    else -> Unit
                }
            }
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                val title = when (state) {
                    is UpdateUi.Available -> "Mise à jour disponible (build ${state.info.versionCode})"
                    is UpdateUi.Downloading -> "Téléchargement de la mise à jour…"
                    is UpdateUi.Ready -> "Mise à jour prête"
                    is UpdateUi.Error -> "Échec : ${state.message}"
                    else -> ""
                }
                val subtitle = when (state) {
                    is UpdateUi.Available -> "Touchez pour télécharger et installer"
                    is UpdateUi.Ready -> if (UpdateInstaller.canInstall(context)) "Touchez pour installer"
                        else "Touchez puis autorisez Somnia à installer des applications"
                    is UpdateUi.Error -> "Touchez pour réessayer"
                    else -> null
                }
                Text(title, color = SomniaColors.Night, style = MaterialTheme.typography.titleSmall)
                subtitle?.let { Text(it, color = SomniaColors.Night, style = MaterialTheme.typography.bodySmall) }
            }
            if (state is UpdateUi.Available) {
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = vm::dismiss) { Text("Plus tard", color = SomniaColors.Night) }
            }
        }
        if (state is UpdateUi.Downloading) {
            if (state.progress >= 0f) {
                LinearProgressIndicator(
                    progress = { state.progress }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    color = SomniaColors.Night, trackColor = SomniaColors.Night.copy(alpha = 0.2f),
                )
            } else {
                LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 6.dp), color = SomniaColors.Night)
            }
        }
    }
}
