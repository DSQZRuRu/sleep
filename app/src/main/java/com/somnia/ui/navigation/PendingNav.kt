package com.somnia.ui.navigation

import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow

/** Destination demandée par une notification (rituel, bilan), consommée par la navigation. */
object PendingNav {
    const val EXTRA_OPEN = "com.somnia.OPEN"
    const val RITUAL = "ritual"
    const val RECAP = "recap"

    val target = MutableStateFlow<String?>(null)

    fun handle(intent: Intent?) {
        intent?.getStringExtra(EXTRA_OPEN)?.let { target.value = it }
    }
}
