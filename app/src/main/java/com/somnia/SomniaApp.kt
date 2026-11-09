package com.somnia

import android.app.Application
import com.somnia.alarm.Notifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class SomniaApp : Application() {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Notifications.createChannels(this)
        // Reprogramme les rappels dès que le profil (heure de coucher) ou leurs réglages changent.
        appScope.launch {
            combine(container.profileRepository.profile, container.reminderPrefs.settings) { p, s -> p to s }
                .collectLatest { container.reminderScheduler.reschedule() }
        }
    }
}
