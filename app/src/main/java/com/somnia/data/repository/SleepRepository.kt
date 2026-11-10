package com.somnia.data.repository

import com.somnia.data.db.EpochEntity
import com.somnia.data.db.SleepDao
import com.somnia.data.db.SleepSessionEntity
import kotlinx.coroutines.flow.Flow

class SleepRepository(private val dao: SleepDao) {

    val history: Flow<List<SleepSessionEntity>> = dao.observeProcessedSessions()
    val pendingReveal: Flow<SleepSessionEntity?> = dao.observePendingReveal()

    fun observeSession(id: Long) = dao.observeSession(id)
    suspend fun session(id: Long) = dao.session(id)
    suspend fun openSession() = dao.openSession()
    suspend fun epochs(sessionId: Long) = dao.epochs(sessionId)
    suspend fun lastEpochs(sessionId: Long, limit: Int) = dao.lastEpochs(sessionId, limit).reversed()

    suspend fun startSession(start: Long, micEnabled: Boolean, alarmTarget: Long?, targetBed: Int, targetWake: Int, ritualDone: Boolean = false): Long =
        dao.insertSession(
            SleepSessionEntity(
                startTime = start, micEnabled = micEnabled, alarmTargetTime = alarmTarget,
                targetBedMin = targetBed, targetWakeMin = targetWake, ritualDone = ritualDone,
            )
        )

    suspend fun setFactors(sessionId: Long, factors: Set<com.somnia.domain.sleep.SleepFactor>) =
        dao.setFactors(sessionId, com.somnia.domain.sleep.SleepFactor.encode(factors))

    suspend fun processedBetween(from: Long, to: Long) = dao.processedBetween(from, to)

    suspend fun addEpoch(epoch: EpochEntity) = dao.insertEpoch(epoch)

    suspend fun markAlarmRang(sessionId: Long, at: Long) {
        dao.session(sessionId)?.let { dao.updateSession(it.copy(alarmRangAt = at)) }
    }

    suspend fun markRewardsSeen(sessionId: Long) {
        dao.session(sessionId)?.let { dao.updateSession(it.copy(rewardsSeen = true)) }
    }

    suspend fun delete(sessionId: Long) = dao.deleteSession(sessionId)
}
