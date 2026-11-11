package com.somnia.tracking

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Process
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Détection de secours : quand aucune nuit n'a été suivie, on estime le sommeil à partir
 * du dernier usage du téléphone le soir et du premier usage le matin (UsageStatsManager,
 * données locales du système, nécessite l'« accès aux données d'utilisation »).
 */
class PhoneUsageDetector(private val context: Context) {

    data class Estimate(val sleepStart: Long, val wakeTime: Long)

    fun hasAccess(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** Instants d'utilisation du téléphone entre [from] et [to] (vide sans autorisation). */
    fun interactionsBetween(from: Long, to: Long): List<Long> {
        if (!hasAccess() || to <= from) return emptyList()
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val events = usm.queryEvents(from, to)
        val out = mutableListOf<Long>()
        val e = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            if (isInteraction(e.eventType)) out += e.timeStamp
        }
        return out
    }

    /** Sommeil probable avant le lancement du suivi (voir SleepGapFinder). */
    fun sleepGapBefore(trackingStart: Long): com.somnia.domain.sleep.SleepGapFinder.Gap? =
        com.somnia.domain.sleep.SleepGapFinder.find(
            interactionsBetween(trackingStart - com.somnia.domain.sleep.SleepGapFinder.LOOKBACK_MS, trackingStart + 60_000L),
            trackingStart,
        )

    /** Estime la nuit précédente : plus longue période sans usage entre 19 h la veille et 14 h. */
    fun estimateLastNight(now: Long = System.currentTimeMillis()): Estimate? {
        if (!hasAccess()) return null
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val from = today.minusDays(1).atTime(LocalTime.of(19, 0)).atZone(zone).toInstant().toEpochMilli()
        val to = minOf(now, today.atTime(LocalTime.of(14, 0)).atZone(zone).toInstant().toEpochMilli())
        if (to <= from) return null

        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val events = usm.queryEvents(from, to)
        val times = mutableListOf<Long>()
        val e = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            if (isInteraction(e.eventType)) times += e.timeStamp
        }
        if (times.size < 2) return null
        times.sort()

        var bestStart = 0L
        var bestEnd = 0L
        for (i in 1 until times.size) {
            val gap = times[i] - times[i - 1]
            if (gap > bestEnd - bestStart) { bestStart = times[i - 1]; bestEnd = times[i] }
        }
        val hours = (bestEnd - bestStart) / 3_600_000.0
        return if (hours in 3.0..14.0) Estimate(bestStart, bestEnd) else null
    }

    /**
     * Heure à laquelle l'écran s'est éteint pour la dernière fois, d'après les événements système
     * (API 28+, accès aux données d'utilisation requis). Null si l'information n'est pas disponible.
     */
    fun screenOffSince(now: Long = System.currentTimeMillis()): Long? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P || !hasAccess()) return null
        val from = now - 30 * 60_000L
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val events = usm.queryEvents(from, now)
        val e = UsageEvents.Event()
        var lastOn = -1L
        var lastOff = -1L
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            when (e.eventType) {
                UsageEvents.Event.SCREEN_INTERACTIVE -> lastOn = e.timeStamp
                UsageEvents.Event.SCREEN_NON_INTERACTIVE -> lastOff = e.timeStamp
            }
        }
        return when {
            lastOff > lastOn -> lastOff
            lastOn < 0 && lastOff < 0 -> from // aucun événement : écran éteint depuis au moins 30 min
            else -> null
        }
    }

    @Suppress("DEPRECATION")
    private fun isInteraction(type: Int): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (type == UsageEvents.Event.KEYGUARD_HIDDEN || type == UsageEvents.Event.SCREEN_INTERACTIVE) return true
        }
        return type == UsageEvents.Event.MOVE_TO_FOREGROUND
    }
}
