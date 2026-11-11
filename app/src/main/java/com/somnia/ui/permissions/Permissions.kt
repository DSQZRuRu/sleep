package com.somnia.ui.permissions

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.somnia.alarm.SmartAlarmScheduler
import com.somnia.tracking.PhoneUsageDetector
import com.somnia.ui.components.SomniaCard
import com.somnia.ui.theme.SomniaColors

/** État des autorisations utiles à Somnia. */
object PermissionStatus {
    fun notifications(c: Context) = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun microphone(c: Context) =
        ContextCompat.checkSelfPermission(c, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    fun exactAlarms(c: Context) = SmartAlarmScheduler(c).canScheduleExact()

    fun fullScreen(c: Context): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
        c.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()

    fun batteryUnrestricted(c: Context): Boolean =
        (c.getSystemService(Context.POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(c.packageName)

    fun usageAccess(c: Context) = PhoneUsageDetector(c).hasAccess()
}

object PermissionIntents {
    private fun pkg(c: Context) = Uri.parse("package:${c.packageName}")

    fun exactAlarms(c: Context): Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, pkg(c)) else null

    fun fullScreen(c: Context): Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, pkg(c)) else null

    @SuppressLint("BatteryLife")
    fun battery(c: Context) = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, pkg(c))

    fun usage() = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)

    fun appDetails(c: Context) = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkg(c))
}

private fun Context.launch(intent: Intent?) {
    if (intent == null) return
    runCatching { startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        .onFailure { runCatching { startActivity(PermissionIntents.appDetails(this).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
}

/**
 * Panneau d'autorisations : chaque ligne explique POURQUOI la permission est utile.
 * L'état est rafraîchi à chaque retour sur l'écran (après un passage dans les réglages).
 */
@Composable
fun PermissionsPanel(micWanted: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refresh++ }

    val runtimeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh++ }

    // `key(refresh)` recalcule les statuts après chaque retour des réglages.
    key(refresh) { Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PermissionRow(
                "Notifications", "Indispensables pour la sonnerie du réveil et la notification de suivi.",
                PermissionStatus.notifications(context),
            ) { runtimeLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
        }
        PermissionRow(
            "Alarmes exactes", "Pour que le réveil sonne à l'heure, même en veille profonde.",
            PermissionStatus.exactAlarms(context),
        ) { context.launch(PermissionIntents.exactAlarms(context)) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            PermissionRow(
                "Réveil en plein écran", "Affiche le réveil par-dessus l'écran de verrouillage.",
                PermissionStatus.fullScreen(context),
            ) { context.launch(PermissionIntents.fullScreen(context)) }
        }
        PermissionRow(
            "Batterie sans restriction", "Évite que le système interrompe le suivi au milieu de la nuit.",
            PermissionStatus.batteryUnrestricted(context),
        ) { context.launch(PermissionIntents.battery(context)) }
        if (micWanted) {
            PermissionRow(
                "Micro (optionnel)", "Mesure uniquement le niveau sonore, en local. Aucun son n'est enregistré.",
                PermissionStatus.microphone(context),
            ) { runtimeLauncher.launch(Manifest.permission.RECORD_AUDIO) }
        }
        PermissionRow(
            "Données d'utilisation (optionnel)", "Estime votre nuit si vous oubliez de lancer le suivi, et repère un sommeil commencé avant son lancement.",
            PermissionStatus.usageAccess(context),
        ) { context.launch(PermissionIntents.usage()) }
    } }
}

@Composable
private fun PermissionRow(title: String, why: String, granted: Boolean, onRequest: () -> Unit) {
    SomniaCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(why, style = MaterialTheme.typography.bodySmall, color = SomniaColors.Muted)
            }
            Spacer(Modifier.width(12.dp))
            if (granted) {
                Icon(Icons.Filled.CheckCircle, contentDescription = "Accordée", tint = SomniaColors.Teal)
            } else {
                FilledTonalButton(onClick = onRequest) { Text("Autoriser") }
            }
        }
    }
}
