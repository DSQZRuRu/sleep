package com.somnia.alarm

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.somnia.MainActivity
import com.somnia.ui.components.CompanionView
import com.somnia.domain.game.CompanionMood
import com.somnia.domain.game.CompanionStage
import com.somnia.ui.theme.SomniaColors
import com.somnia.ui.theme.SomniaTheme
import java.text.DateFormat
import java.util.Date

/** Écran plein écran du réveil, affiché par-dessus l'écran de verrouillage. */
class AlarmActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        val time = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date())
        setContent {
            SomniaTheme {
                val pulse by rememberInfiniteTransition(label = "sun").animateFloat(
                    0.95f, 1.08f, infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse",
                )
                Column(
                    Modifier.fillMaxSize()
                        .background(Brush.verticalGradient(listOf(SomniaColors.Night, SomniaColors.Dawn)))
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CompanionView(CompanionStage.YOUNG, CompanionMood.RADIANT, Modifier.size(160.dp).scale(pulse))
                    Spacer(Modifier.height(24.dp))
                    Text(time, style = MaterialTheme.typography.displayLarge, color = SomniaColors.Moon)
                    Text("Vous êtes en sommeil léger : c'est le bon moment.", color = SomniaColors.Muted)
                    Spacer(Modifier.height(48.dp))
                    Button(onClick = { dismiss() }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                        Text("Je me lève")
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = { snooze() }, modifier = Modifier.fillMaxWidth()) {
                        Text("Encore ${SmartAlarmScheduler.SNOOZE_MIN} minutes")
                    }
                }
            }
        }
    }

    private fun dismiss() {
        AlarmController.dismiss(this)
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))
        finish()
    }

    private fun snooze() {
        AlarmController.snooze(this)
        finish()
    }
}
