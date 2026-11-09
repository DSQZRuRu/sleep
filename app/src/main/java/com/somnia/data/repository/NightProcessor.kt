package com.somnia.data.repository

import androidx.room.withTransaction
import com.somnia.data.db.BadgeUnlockEntity
import com.somnia.data.db.ChallengeCompletionEntity
import com.somnia.data.db.CreatureCatchEntity
import com.somnia.data.db.CreatureProgressEntity
import com.somnia.data.db.GameStateEntity
import com.somnia.data.db.NightRewardEntity
import com.somnia.data.db.SessionSource
import com.somnia.data.db.SleepSessionEntity
import com.somnia.data.db.SomniaDatabase
import com.somnia.domain.game.DreamCreatureCatalog
import com.somnia.domain.game.EquippedCreature
import com.somnia.domain.game.GameSnapshot
import com.somnia.domain.game.SeasonalEvent
import com.somnia.domain.game.NightRewards
import com.somnia.domain.game.RewardEngine
import com.somnia.domain.game.StreakState
import com.somnia.domain.game.WeekKey
import com.somnia.domain.sleep.AdaptiveParams
import com.somnia.domain.sleep.EpochInput
import com.somnia.domain.sleep.NightAnalysis
import com.somnia.domain.sleep.ProfileAdjuster
import com.somnia.domain.sleep.RegularityCalculator
import com.somnia.domain.sleep.SleepScoreCalculator
import com.somnia.domain.sleep.SleepStageClassifier
import com.somnia.domain.sleep.TimeMath
import kotlin.math.roundToInt

/**
 * Cas d'usage central : clôture d'une nuit.
 * Analyse des phases → score → ajustement du profil → récompenses, dans une seule transaction.
 */
class NightProcessor(private val db: SomniaDatabase, private val activeEvent: () -> SeasonalEvent? = { null }) {

    private val sleepDao = db.sleepDao()
    private val gameDao = db.gameDao()
    private val profileDao = db.profileDao()

    /** @return l'id de la session traitée, ou null si la nuit était trop courte pour être gardée. */
    suspend fun finalizeSensorSession(sessionId: Long, endTime: Long): Long? = db.withTransaction {
        val session = sleepDao.session(sessionId) ?: return@withTransaction null
        if (session.processed) return@withTransaction sessionId
        val epochs = sleepDao.epochs(sessionId)
        if (epochs.size < MIN_EPOCHS) {
            sleepDao.deleteSession(sessionId)
            return@withTransaction null
        }
        val analysis = SleepStageClassifier.classify(
            epochs.map { EpochInput(it.timestamp, it.motionCount, it.loudEvents, it.phoneUsed) }
        )
        sleepDao.updateEpochs(epochs.mapIndexed { i, e -> e.copy(stage = analysis.stages[i].code) })

        val onset = analysis.sleepOnsetIndex?.let { epochs[it].timestamp }
        val finalWake = analysis.finalWakeIndex?.let { epochs[it].timestamp + 60_000L }
        val deviation = TimeMath.diff(Clock.minuteOfDay(session.startTime), session.targetBedMin)
        val score = SleepScoreCalculator.score(analysis, deviation).total

        val updated = session.copy(
            endTime = endTime,
            sleepOnset = onset,
            finalWake = finalWake,
            totalSleepMin = analysis.totalSleepMin,
            lightMin = analysis.lightMin,
            deepMin = analysis.deepMin,
            awakeMin = analysis.awakeMin,
            latencyMin = analysis.latencyMin,
            awakenings = analysis.awakenings,
            efficiency = analysis.efficiency,
            score = score,
            snoreMinutes = epochs.count { it.snoreLike },
            processed = true,
        )
        sleepDao.updateSession(updated)
        learnFromNight(analysis)
        grantRewards(updated)
        sessionId
    }

    /** Nuit estimée à partir de l'usage du téléphone (détection de secours). */
    suspend fun createUsageSession(start: Long, end: Long, targetBed: Int, targetWake: Int): Long = db.withTransaction {
        val profile = profileDao.get() ?: com.somnia.data.db.ProfileEntity()
        val inBed = ((end - start) / 60_000L).toInt()
        val sleep = (inBed - profile.latencyMin).coerceAtLeast(0)
        val analysis = NightAnalysis(
            stages = emptyList(), sleepOnsetIndex = null, finalWakeIndex = null,
            totalSleepMin = sleep, lightMin = sleep, deepMin = 0, awakeMin = inBed - sleep,
            latencyMin = profile.latencyMin, awakenings = 0, efficiency = 0f, detectedCycles = 0,
        )
        val deviation = TimeMath.diff(Clock.minuteOfDay(start), targetBed)
        val score = SleepScoreCalculator.score(analysis, deviation, hasStages = false).total
        val id = sleepDao.insertSession(
            SleepSessionEntity(
                startTime = start, endTime = end, source = SessionSource.PHONE_USAGE,
                sleepOnset = start + profile.latencyMin * 60_000L, finalWake = end,
                totalSleepMin = sleep, lightMin = sleep, awakeMin = inBed - sleep,
                latencyMin = profile.latencyMin, score = score,
                targetBedMin = targetBed, targetWakeMin = targetWake, processed = true,
            )
        )
        grantRewards(sleepDao.session(id)!!)
        id
    }

    suspend fun setMorningRating(sessionId: Long, rating: Int) = db.withTransaction {
        val s = sleepDao.session(sessionId) ?: return@withTransaction
        val firstRating = s.morningRating == null
        sleepDao.updateSession(s.copy(morningRating = rating))
        if (!firstRating) return@withTransaction
        val p = profileDao.get() ?: return@withTransaction
        val slept = (s.totalSleepMin.toFloat() / p.cycleMin).roundToInt()
        val adjusted = ProfileAdjuster.adjust(
            AdaptiveParams(p.latencyMin, p.cycleMin, p.preferredCycles, p.nightsLearned), null, rating, slept,
        )
        profileDao.upsert(p.copy(preferredCycles = adjusted.preferredCycles))
    }

    private suspend fun learnFromNight(analysis: NightAnalysis) {
        val p = profileDao.get() ?: return
        val adjusted = ProfileAdjuster.adjust(
            AdaptiveParams(p.latencyMin, p.cycleMin, p.preferredCycles, p.nightsLearned), analysis, null, null,
        )
        profileDao.upsert(
            p.copy(latencyMin = adjusted.latencyMin, cycleMin = adjusted.cycleMin, nightsLearned = adjusted.nightsLearned)
        )
    }

    private suspend fun grantRewards(session: SleepSessionEntity): NightRewards {
        val night = session.toSummary()
        val week = WeekKey.of(night.nightDay)
        val weekStart = Clock.startOfDayMillis(WeekKey.mondayOf(night.nightDay)) + 12 * 3_600_000L
        val weekNights = sleepDao.processedSince(weekStart)
            .map { it.toSummary() }
            .filter { WeekKey.of(it.nightDay) == week }
        val recent = sleepDao.recentProcessed(7)
        val regularity = RegularityCalculator.compute(
            recent.map { Clock.minuteOfDay(it.startTime) },
            recent.map { Clock.minuteOfDay(it.wakeTime) },
        ).score

        val state = gameDao.state() ?: GameStateEntity()
        val catches = gameDao.catches()
        val progress = gameDao.progress()
        val equipped = progress.filter { it.slot != null }.sortedBy { it.slot }.mapNotNull { p ->
            DreamCreatureCatalog.byId(p.creatureId)?.let { EquippedCreature(it, p.level) }
        }
        val previousScore = sleepDao.recentProcessed(2).firstOrNull { it.id != session.id }?.score
        val event = activeEvent()
        val snapshot = GameSnapshot(
            totalXp = state.totalXp,
            streak = StreakState(state.streak, state.bestStreak, state.lastStreakNightDay, state.freezeUsedWeek),
            companionCare = state.companionCare,
            ownedCreatureIds = catches.map { it.creatureId }.toSet(),
            creatureCatches = catches.size,
            unlockedBadges = gameDao.badges().map { it.badgeId }.toSet(),
            challengesCompletedTotal = gameDao.completionCount(),
            completedThisWeek = gameDao.completions(week).map { it.challengeId }.toSet(),
            stardust = state.stardust,
            bestDream = state.bestDream,
            equipped = equipped,
            maxCreatureLevel = progress.maxOfOrNull { it.level } ?: 1,
        )
        val rewards = RewardEngine.process(
            night, weekNights, snapshot, sleepDao.processedCount(), regularity,
            previousScore = previousScore, event = event,
        )
        val now = System.currentTimeMillis()

        val s = rewards.streak.state
        gameDao.upsertState(
            state.copy(
                totalXp = rewards.totalXpAfter,
                streak = s.current, bestStreak = s.best,
                lastStreakNightDay = s.lastNightDay, freezeUsedWeek = s.freezeUsedWeek,
                companionCare = state.companionCare + rewards.careGained,
                stardust = state.stardust + rewards.stardustGained,
                bestDream = maxOf(state.bestDream, rewards.dream.total),
            )
        )
        if (rewards.creatures.isNotEmpty()) {
            gameDao.insertCatches(rewards.creatures.map { CreatureCatchEntity(creatureId = it.id, sessionId = session.id, caughtAt = now) })
            gameDao.insertProgress(rewards.creatures.map { CreatureProgressEntity(it.id) })
        }
        gameDao.insertBadges(rewards.newBadges.map { BadgeUnlockEntity(it.id, now) })
        gameDao.insertChallengeCompletions(rewards.completedChallenges.map { ChallengeCompletionEntity(week, it.id, now) })
        gameDao.upsertReward(
            NightRewardEntity(
                sessionId = session.id,
                xpLines = rewards.xpLines.joinToString(";") { "${it.label.replace(";", ",").replace("|", "/")}|${it.xp}" },
                creatureIds = rewards.creatures.joinToString(",") { it.id },
                newCreatureIds = rewards.newCreatureIds.joinToString(","),
                badgeIds = rewards.newBadges.joinToString(",") { it.id },
                challengeIds = rewards.completedChallenges.joinToString(",") { it.id },
                streakAfter = s.current,
                freezeUsed = rewards.streak.freezeUsed,
                streakBroken = rewards.streak.broken,
                levelBefore = rewards.levelBefore,
                levelAfter = rewards.levelAfter,
                stageBefore = rewards.stageBefore.name,
                stageAfter = rewards.stageAfter.name,
                totalXpAfter = rewards.totalXpAfter,
                dreamBase = rewards.dream.base,
                dreamScore = rewards.dream.total,
                dreamLines = encodeDreamLines(rewards.dream.lines) + "##" + encodeDreamLines(rewards.dream.stardustLines),
                stardustGained = rewards.stardustGained,
                duplicateStardust = rewards.duplicateStardust,
                eventId = rewards.event?.id,
            )
        )
        return rewards
    }

    companion object {
        /** En dessous de 20 minutes, on considère la nuit comme annulée. */
        const val MIN_EPOCHS = 20
    }
}
