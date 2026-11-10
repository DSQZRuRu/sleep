package com.somnia.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.action.actionStartService
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.somnia.MainActivity
import com.somnia.SomniaApp
import com.somnia.data.repository.ProfileRepository
import com.somnia.domain.game.CompanionStage
import com.somnia.domain.sleep.TimeMath
import com.somnia.tracking.SleepTrackingService
import com.somnia.tracking.TrackingState
import com.somnia.ui.components.CreatureArt
import kotlinx.coroutines.flow.first

/** Widget d'écran d'accueil : Brumi, l'heure de coucher idéale et un bouton pour lancer la nuit. */
class SomniaWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val container = (context.applicationContext as SomniaApp).container
        val profile = container.profileRepository.get()
        val game = container.gameRepository.state.first()
        val stage = CompanionStage.forCare(game.companionCare)
        val bed = if (profile.onboardingDone) TimeMath.format(ProfileRepository.idealBedtime(profile)) else "--:--"
        val tracking = TrackingState.status.value.running

        provideContent {
            Box(
                GlanceModifier.fillMaxSize().cornerRadius(24.dp).background(Color(0xFF141A36)).padding(12.dp)
                    .clickable(actionStartActivity<MainActivity>()),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.fillMaxWidth()) {
                    Image(
                        ImageProvider(CreatureArt.forCompanion(stage, eyesClosed = true)),
                        contentDescription = "Brumi",
                        modifier = GlanceModifier.size(64.dp),
                    )
                    Spacer(GlanceModifier.width(10.dp))
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text("Coucher idéal", style = TextStyle(color = ColorProvider(Color(0xFFAEB4D6)), fontSize = 12.sp))
                        Text(bed, style = TextStyle(color = ColorProvider(Color(0xFFA9B4FF)), fontSize = 28.sp, fontWeight = FontWeight.Bold))
                        Spacer(GlanceModifier.height(6.dp))
                        Box(
                            GlanceModifier.cornerRadius(18.dp).background(Color(0xFFA9B4FF)).padding(horizontal = 14.dp, vertical = 6.dp)
                                .clickable(
                                    if (tracking) actionStartActivity<MainActivity>()
                                    else actionStartService(SleepTrackingService.startWithDefaultAlarmIntent(context), isForegroundService = true)
                                ),
                        ) {
                            Text(
                                if (tracking) "Nuit en cours ☾" else "☾ Lancer la nuit",
                                style = TextStyle(color = ColorProvider(Color(0xFF0B1026)), fontSize = 14.sp, fontWeight = FontWeight.Bold),
                            )
                        }
                    }
                }
            }
        }
    }

    companion object {
        suspend fun refresh(context: Context) = runCatching { SomniaWidget().updateAll(context) }
    }
}

class SomniaWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SomniaWidget()
}
