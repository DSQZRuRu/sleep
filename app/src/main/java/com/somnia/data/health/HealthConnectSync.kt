package com.somnia.data.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.SleepSessionRecord
import com.somnia.data.db.EpochEntity
import com.somnia.data.db.SleepSessionEntity
import com.somnia.data.repository.wakeTime
import com.somnia.domain.sleep.SleepStage
import java.time.Instant
import java.time.ZoneId

/**
 * Envoi des nuits vers Health Connect (le service santé d'Android), sur demande de l'utilisateur.
 * Seules les sessions de sommeil et leurs phases estimées sont écrites ; rien n'est lu.
 */
object HealthConnectSync {
    val PERMISSIONS = setOf(HealthPermission.getWritePermission(SleepSessionRecord::class))

    fun available(context: Context): Boolean =
        HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    suspend fun hasPermission(context: Context): Boolean = runCatching {
        HealthConnectClient.getOrCreate(context).permissionController.getGrantedPermissions().containsAll(PERMISSIONS)
    }.getOrDefault(false)

    suspend fun write(context: Context, session: SleepSessionEntity, epochs: List<EpochEntity>): Boolean = runCatching {
        if (!available(context) || !hasPermission(context)) return false
        val start = Instant.ofEpochMilli(session.startTime)
        val end = Instant.ofEpochMilli(maxOf(session.endTime ?: session.wakeTime, session.startTime + 60_000L))
        val zone = ZoneId.systemDefault().rules
        val stages = mutableListOf<SleepSessionRecord.Stage>()
        var i = 0
        while (i < epochs.size) {
            val st = epochs[i].stage
            var j = i
            while (j + 1 < epochs.size && epochs[j + 1].stage == st) j++
            val type = when (SleepStage.fromCode(st)) {
                SleepStage.AWAKE -> SleepSessionRecord.STAGE_TYPE_AWAKE
                SleepStage.LIGHT -> SleepSessionRecord.STAGE_TYPE_LIGHT
                SleepStage.DEEP -> SleepSessionRecord.STAGE_TYPE_DEEP
            }
            val s = Instant.ofEpochMilli(epochs[i].timestamp).coerceAtLeast(start)
            val e = Instant.ofEpochMilli(epochs[j].timestamp + 60_000L).coerceAtMost(end)
            if (st >= 0 && e.isAfter(s)) stages += SleepSessionRecord.Stage(s, e, type)
            i = j + 1
        }
        val record = SleepSessionRecord(
            startTime = start,
            startZoneOffset = zone.getOffset(start),
            endTime = end,
            endZoneOffset = zone.getOffset(end),
            title = "Nuit Somnia",
            notes = "Score ${session.score}/100",
            stages = stages,
        )
        HealthConnectClient.getOrCreate(context).insertRecords(listOf(record))
        true
    }.getOrDefault(false)
}
