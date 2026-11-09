package com.somnia.data.repository

import com.somnia.data.db.ProfileDao
import com.somnia.data.db.ProfileEntity
import com.somnia.domain.sleep.BedtimeCalculator
import com.somnia.domain.sleep.Chronotype
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProfileRepository(private val dao: ProfileDao) {

    val profile: Flow<ProfileEntity> = dao.observe().map { it ?: ProfileEntity() }

    suspend fun get(): ProfileEntity = dao.get() ?: ProfileEntity()

    suspend fun save(profile: ProfileEntity) = dao.upsert(profile)

    suspend fun update(transform: (ProfileEntity) -> ProfileEntity) = dao.upsert(transform(get()))

    companion object {
        fun options(p: ProfileEntity): List<BedtimeCalculator.Option> = BedtimeCalculator.options(
            wakeMin = p.targetWakeMin,
            cycleMin = p.cycleMin,
            latencyMin = p.latencyMin,
            preferredCycles = p.preferredCycles,
            earliestBedMin = p.earliestBedMin,
            latestBedMin = p.latestBedMin,
        )

        fun idealBedtime(p: ProfileEntity): Int = options(p).first { it.ideal }.bedtimeMin

        fun chronotype(p: ProfileEntity): Chronotype =
            runCatching { Chronotype.valueOf(p.chronotype) }.getOrDefault(Chronotype.NEITHER)
    }
}
