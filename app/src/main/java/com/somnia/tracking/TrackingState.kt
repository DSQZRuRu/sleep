package com.somnia.tracking

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class TrackingStatus(
    val running: Boolean = false,
    val sessionId: Long? = null,
    val startedAt: Long? = null,
    val alarmAt: Long? = null,
    val micActive: Boolean = false,
    val ringing: Boolean = false,
    val finishing: Boolean = false,
    /** Durée choisie si c'est une sieste (null = nuit). */
    val napMinutes: Int? = null,
)

/** État partagé en mémoire entre le service, l'alarme et l'interface. */
object TrackingState {
    private val _status = MutableStateFlow(TrackingStatus())
    val status: StateFlow<TrackingStatus> = _status.asStateFlow()

    fun set(status: TrackingStatus) { _status.value = status }
    fun update(transform: (TrackingStatus) -> TrackingStatus) = _status.update(transform)
}
