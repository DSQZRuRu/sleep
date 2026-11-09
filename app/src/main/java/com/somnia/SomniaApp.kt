package com.somnia

import android.app.Application
import com.somnia.alarm.Notifications

class SomniaApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Notifications.createChannels(this)
    }
}
