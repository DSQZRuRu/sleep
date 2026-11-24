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
import com.somnia.domain.game.DecorCatalog
import com.somnia.domain.game.DecorSlot
import com.somnia.domain.game.DreamCreatureCatalog
import com.somnia.domain.game.ExpeditionState
import com.somnia.domain.sleep.AutoNightTrimmer
import com.somnia.domain.sleep.MoonPhase
import com.somnia.domain.sleep.NightMerger
import com.somnia.domain.sleep.SleepGapFinder
import com.somnia.domain.sleep.SleepStage
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
class NightProcessor(
    private val db: SomniaDatabase,
    private val activeEvent: () -> SeasonalEvent? = { null },
    /** Jour où l'utilisateur a confirmé la lumière du matin (−1 si jamais). */
    private val morningLightDay: () -> Long = { -1L },
    /** Sommeil probable avant le lancement du suivi, d'après l'usage du téléphone. */
    private val sleepGapBefore: (Long) -> SleepGapFinder.Gap? = { null },
    /** Le soir donné (epochDay) a-t-il été déclaré « soirée exceptionnelle » ? */
    private val isExceptional: (Long) -> Boolean = { false },
) {

    private val sleepDao = db.sleepDao()
    private val gameDao = db.gameDao()
    private val profileDao = db.profileDao()

    /** @return l'id de la session traitée, ou null si la nuit était trop courte pour être gardée. */
    suspend fun finalizeSensorSession(sessionId: Long, endTime: Long, detectEarlierSleep: Boolean = true): Long? {
        // Hors transaction : lecture de l'historique d'usage du téléphone.
        val pre = sleepDao.session(sessionId)
        val gap = if (detectEarlierSleep && pre != null && !pre.isNap && !pre.processed && !pre.autoStarted)
            runCatching { sleepGapBefore(pre.startTime) }.getOrNull() else null
        val id = finalizeInTransaction(sessionId, endTime, gap) ?: return null
        // Rendormi après un réveil ? On regroupe avec la partie précédente de la nuit.
        return if (detectEarlierSleep) mergeIfSameNight(id) else id
    }

    /**
     * Si une autre nuit du même soir s'est terminée moins de 3 h avant le début de celle-ci
     * (ex. réveillé par l'alarme, puis rendormi), les deux sont regroupées.
     * @return l'id de la nuit finale.
     */
    suspend fun mergeIfSameNight(sessionId: Long): Long {
        val s = sleepDao.session(sessionId) ?: return sessionId
        if (!s.processed || s.isNap || s.startCandidate != null) return sessionId
        val previous = sleepDao.processedNear(sessionId, s.startTime - 16 * 3_600_000L, s.startTime).firstOrNull { p ->
            p.startCandidate == null &&
                Clock.nightDay(p.startTime) == Clock.nightDay(s.startTime) &&
                s.startTime - (p.endTime ?: p.wakeTime) <= NightMerger.MAX_GAP_MS
        } ?: return sessionId
        return mergeSessions(previous.id, sessionId)
    }

    /** Regroupe deux nuits en une seule (l'intervalle compte comme éveil), puis recalcule les récompenses. */
    suspend fun mergeSessions(aId: Long, bId: Long): Long = db.withTransaction {
        val a = sleepDao.session(aId) ?: return@withTransaction bId
        val b = sleepDao.session(bId) ?: return@withTransaction aId
        val (first, second) = if (a.startTime <= b.startTime) a to b else b to a
        revertRewards(first)
        revertRewards(second)
        val profile = profileDao.get() ?: com.somnia.data.db.ProfileEntity()

        val originals = mutableMapOf<Long, com.somnia.data.db.EpochEntity>()
        fun minuteKey(ts: Long) = ts / 60_000L
        suspend fun part(s: SleepSessionEntity): NightMerger.Part {
            val ep = sleepDao.epochs(s.id)
            ep.forEach { originals[minuteKey(it.timestamp)] = it }
            return if (ep.isNotEmpty()) NightMerger.Part(ep.first().timestamp, ep.size, ep.map { SleepStage.fromCode(it.stage) })
            else NightMerger.Part(s.startTime, (((s.endTime ?: s.wakeTime) - s.startTime) / 60_000L).toInt().coerceAtLeast(1), null)
        }
        val merged = NightMerger.merge(listOf(part(first), part(second)), profile.latencyMin.coerceIn(5, 30))

        // Époques de la nuit regroupée : mesures d'origine quand elles existent, sinon minutes estimées.
        sleepDao.deleteEpochs(first.id)
        sleepDao.deleteEpochs(second.id)
        sleepDao.insertEpochs(merged.stages.mapIndexed { i, st ->
            val ts = merged.start + i * 60_000L
            val o = originals[minuteKey(ts)]
            com.somnia.data.db.EpochEntity(
                sessionId = first.id, timestamp = ts, motionCount = o?.motionCount ?: 0f, noiseDb = o?.noiseDb ?: 0f,
                loudEvents = o?.loudEvents ?: 0, snoreLike = o?.snoreLike ?: false, phoneUsed = o?.phoneUsed ?: false, stage = st.code,
            )
        })

        val an = merged.analysis
        val exceptional = first.exceptional || second.exceptional
        val deviation = if (exceptional) 0 else TimeMath.diff(Clock.minuteOfDay(merged.start), first.targetBedMin)
        val sensors = first.source == SessionSource.SENSORS || second.source == SessionSource.SENSORS
        val updated = first.copy(
            startTime = merged.start,
            endTime = maxOf(first.endTime ?: first.wakeTime, second.endTime ?: second.wakeTime),
            source = if (sensors) SessionSource.SENSORS else first.source,
            micEnabled = first.micEnabled || second.micEnabled,
            sleepOnset = an.sleepOnsetIndex?.let { merged.start + it * 60_000L },
            finalWake = an.finalWakeIndex?.let { merged.start + (it + 1) * 60_000L },
            totalSleepMin = an.totalSleepMin, lightMin = an.lightMin, deepMin = an.deepMin, awakeMin = an.awakeMin,
            latencyMin = an.latencyMin, awakenings = an.awakenings, efficiency = an.efficiency,
            score = SleepScoreCalculator.score(an, deviation, hasStages = sensors).total,
            snoreMinutes = first.snoreMinutes + second.snoreMinutes,
            factors = com.somnia.domain.sleep.SleepFactor.encode(
                com.somnia.domain.sleep.SleepFactor.parse(first.factors) + com.somnia.domain.sleep.SleepFactor.parse(second.factors)
            ),
            ritualDone = first.ritualDone || second.ritualDone,
            dreamNote = listOf(first.dreamNote, second.dreamNote).filter { it.isNotBlank() }.joinToString("\n"),
            morningRating = second.morningRating ?: first.morningRating,
            exceptional = exceptional,
            processed = true,
            rewardsSeen = false,
            startCandidate = null,
        )
        sleepDao.updateSession(updated)
        sleepDao.deleteSession(second.id)
        grantRewards(updated)
        first.id
    }

    /** Retire les récompenses déjà attribuées à une nuit (avant de la regrouper avec une autre). */
    private suspend fun revertRewards(s: SleepSessionEntity) {
        val r = gameDao.reward(s.id) ?: return
        val xp = r.xpLines.split(";").mapNotNull { it.substringAfterLast("|", "").toIntOrNull() }.sum()
        val summary = s.toSummary()
        val care = com.somnia.domain.game.CompanionLogic.careForNight(s.score, summary.regular, summary.healthyDuration)
        val st = gameDao.state() ?: GameStateEntity()
        gameDao.upsertState(
            st.copy(
                totalXp = (st.totalXp - xp).coerceAtLeast(0),
                stardust = (st.stardust - r.stardustGained).coerceAtLeast(0),
                companionCare = (st.companionCare - care).coerceAtLeast(0),
                expeditionProgress = if (r.regionsCompleted.isBlank()) (st.expeditionProgress - r.expeditionSteps).coerceAtLeast(0)
                else st.expeditionProgress,
            )
        )
        val caught = gameDao.catchesForSession(s.id).map { it.creatureId }.toSet()
        gameDao.deleteCatchesForSession(s.id)
        for (id in caught) {
            val n = gameDao.catchCount(id)
            val creature = DreamCreatureCatalog.byId(id) ?: continue
            if (n == 0) gameDao.deleteProgress(id)
            else com.somnia.domain.game.CreatureStars.addDuplicates(creature.rarity, 0, 0, n - 1).let { gameDao.setStars(id, it.stars, it.shards) }
        }
        gameDao.deleteReward(s.id)
    }

    private suspend fun finalizeInTransaction(sessionId: Long, endTime: Long, gap: SleepGapFinder.Gap?): Long? = db.withTransaction {
        val session0 = sleepDao.session(sessionId) ?: return@withTransaction null
        if (session0.processed) return@withTransaction sessionId
        val allEpochs = sleepDao.epochs(sessionId)
        if (allEpochs.size < (if (session0.isNap) MIN_NAP_EPOCHS else MIN_EPOCHS)) {
            sleepDao.deleteSession(sessionId)
            return@withTransaction null
        }
        var epochs = allEpochs
        var session = session0
        if (session.autoStarted) {
            // Suivi automatique : la nuit commence au dernier usage du téléphone avant l'endormissement.
            val first = SleepStageClassifier.classify(epochs.map { EpochInput(it.timestamp, it.motionCount, it.loudEvents, it.phoneUsed) })
            val onset = first.sleepOnsetIndex
            if (onset == null || first.totalSleepMin < AUTO_MIN_SLEEP) {
                // Pas de vraie nuit (soirée sans dormir) : on jette la session en silence.
                sleepDao.deleteSession(sessionId)
                return@withTransaction null
            }
            val lastUse = AutoNightTrimmer.startIndex(first.stages, epochs.map { it.phoneUsed }) ?: 0
            if (lastUse > 0) {
                val newStart = epochs[lastUse].timestamp
                sleepDao.deleteEpochsBefore(sessionId, newStart)
                epochs = epochs.drop(lastUse)
                session = session.copy(startTime = newStart)
            }
        }
        val analysis = SleepStageClassifier.classify(
            epochs.map { EpochInput(it.timestamp, it.motionCount, it.loudEvents, it.phoneUsed) }
        )
        sleepDao.updateEpochs(epochs.mapIndexed { i, e -> e.copy(stage = analysis.stages[i].code) })

        val onset = analysis.sleepOnsetIndex?.let { epochs[it].timestamp }
        val finalWake = analysis.finalWakeIndex?.let { epochs[it].timestamp + 60_000L }
        val exceptional = isExceptional(Clock.nightDay(session.startTime))
        // Soirée exceptionnelle : l'heure de coucher n'est pas pénalisée.
        val deviation = if (exceptional) 0 else TimeMath.diff(Clock.minuteOfDay(session.startTime), session.targetBedMin)
        val score = SleepScoreCalculator.score(analysis, deviation).total

        val updated = session.copy(
            exceptional = exceptional,
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
            // Lumière du matin confirmée le jour qui précède cette nuit : ajoutée aux facteurs.
            factors = if (morningLightDay() == Clock.nightDay(session.startTime))
                com.somnia.domain.sleep.SleepFactor.encode(
                    com.somnia.domain.sleep.SleepFactor.parse(session.factors) + com.somnia.domain.sleep.SleepFactor.MORNING_LIGHT
                ) else session.factors,
        )
        if (session.isNap) {
            // Sieste : enregistrée pour mémoire, sans score de nuit, apprentissage ni récompenses.
            sleepDao.updateSession(updated.copy(rewardsSeen = true))
            return@withTransaction sessionId
        }
        if (gap != null) {
            // Sommeil probablement commencé avant le lancement : on demandera à l'utilisateur
            // au réveil, et les récompenses attendront sa réponse.
            sleepDao.updateSession(updated.copy(startCandidate = gap.asleepFrom))
            return@withTransaction sessionId
        }
        sleepDao.updateSession(updated)
        learnFromNight(analysis)
        grantRewards(updated)
        sessionId
    }

    /**
     * Réponse à « Dormiez-vous déjà avant de lancer la nuit ? ».
     * Oui : la nuit commence à l'heure probable d'endormissement ; cette partie, non mesurée par
     * les capteurs, compte comme sommeil léger estimé, après le temps d'endormissement habituel.
     * Le réveil pour lancer le suivi devient un réveil nocturne. Puis les récompenses sont attribuées.
     */
    suspend fun resolveStartCandidate(sessionId: Long, accept: Boolean) {
        resolveInTransaction(sessionId, accept)
        mergeIfSameNight(sessionId)
    }

    private suspend fun resolveInTransaction(sessionId: Long, accept: Boolean) = db.withTransaction {
        val s = sleepDao.session(sessionId) ?: return@withTransaction
        val candidate = s.startCandidate ?: return@withTransaction
        val prefixMin = ((s.startTime - candidate) / 60_000L).toInt()
        if (!accept || prefixMin < 1) {
            val kept = s.copy(startCandidate = null)
            sleepDao.updateSession(kept)
            grantRewards(kept)
            return@withTransaction
        }
        val profile = profileDao.get() ?: com.somnia.data.db.ProfileEntity()
        val latency = profile.latencyMin.coerceIn(5, 30).coerceAtMost(prefixMin)
        for (i in 0 until prefixMin) {
            sleepDao.insertEpoch(
                com.somnia.data.db.EpochEntity(
                    sessionId = sessionId, timestamp = candidate + i * 60_000L, motionCount = 0f,
                    stage = if (i < latency) SleepStage.AWAKE.code else SleepStage.LIGHT.code,
                )
            )
        }
        val prefixSleep = prefixMin - latency
        val realMinutes = sleepDao.epochs(sessionId).size - prefixMin
        val total = s.totalSleepMin + prefixSleep
        val awake = s.awakeMin + latency
        val awakenings = s.awakenings + if (s.latencyMin > 0) 1 else 0
        val efficiency = total.toFloat() / (prefixMin + realMinutes).coerceAtLeast(1)
        val analysis = NightAnalysis(
            stages = emptyList(), sleepOnsetIndex = latency, finalWakeIndex = null,
            totalSleepMin = total, lightMin = s.lightMin + prefixSleep, deepMin = s.deepMin, awakeMin = awake,
            latencyMin = latency, awakenings = awakenings, efficiency = efficiency, detectedCycles = 0,
        )
        val deviation = TimeMath.diff(Clock.minuteOfDay(candidate), s.targetBedMin)
        val extended = s.copy(
            startTime = candidate,
            sleepOnset = candidate + latency * 60_000L,
            totalSleepMin = total,
            lightMin = s.lightMin + prefixSleep,
            awakeMin = awake,
            latencyMin = latency,
            awakenings = awakenings,
            efficiency = efficiency,
            score = SleepScoreCalculator.score(analysis, deviation).total,
            startCandidate = null,
        )
        sleepDao.updateSession(extended)
        grantRewards(extended)
    }

    /**
     * Outil développeur : crée une nuit factice de 8 h se terminant maintenant, puis la traite
     * comme une vraie nuit (l'écran du matin s'ouvre). À utiliser en mode test pour pouvoir l'annuler.
     */
    suspend fun simulateNight(): Long? {
        val profile = profileDao.get() ?: com.somnia.data.db.ProfileEntity()
        val end = System.currentTimeMillis()
        val start = end - 8 * 3_600_000L
        val id = sleepDao.insertSession(
            SleepSessionEntity(
                startTime = start,
                targetBedMin = Clock.minuteOfDay(start),
                targetWakeMin = profile.targetWakeMin,
            )
        )
        com.somnia.domain.sleep.NightSimulator.generate(start, 480).forEach {
            sleepDao.insertEpoch(com.somnia.data.db.EpochEntity(sessionId = id, timestamp = it.timestamp, motionCount = it.motionCount))
        }
        return finalizeSensorSession(id, end, detectEarlierSleep = false)
    }

    /** Nuit estimée à partir de l'usage du téléphone (détection de secours). */
    suspend fun createUsageSession(
        start: Long, end: Long, targetBed: Int, targetWake: Int, source: String = SessionSource.PHONE_USAGE,
    ): Long = db.withTransaction {
        val profile = profileDao.get() ?: com.somnia.data.db.ProfileEntity()
        val inBed = ((end - start) / 60_000L).toInt()
        val sleep = (inBed - profile.latencyMin).coerceAtLeast(0)
        val analysis = NightAnalysis(
            stages = emptyList(), sleepOnsetIndex = null, finalWakeIndex = null,
            totalSleepMin = sleep, lightMin = sleep, deepMin = 0, awakeMin = inBed - sleep,
            latencyMin = profile.latencyMin, awakenings = 0, efficiency = 0f, detectedCycles = 0,
        )
        val exceptional = isExceptional(Clock.nightDay(start))
        val deviation = if (exceptional) 0 else TimeMath.diff(Clock.minuteOfDay(start), targetBed)
        val score = SleepScoreCalculator.score(analysis, deviation, hasStages = false).total
        val id = sleepDao.insertSession(
            SleepSessionEntity(
                startTime = start, endTime = end, source = source,
                sleepOnset = start + profile.latencyMin * 60_000L, finalWake = end,
                totalSleepMin = sleep, lightMin = sleep, awakeMin = inBed - sleep,
                latencyMin = profile.latencyMin, score = score,
                targetBedMin = targetBed, targetWakeMin = targetWake, processed = true, exceptional = exceptional,
            )
        )
        grantRewards(sleepDao.session(id)!!)
        id
    }.let { mergeIfSameNight(it) }

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
        val recent = sleepDao.recentProcessed(10).filter { !it.exceptional }.take(7)
        val regularity = RegularityCalculator.compute(
            recent.map { Clock.minuteOfDay(it.startTime) },
            recent.map { Clock.minuteOfDay(it.wakeTime) },
        ).score

        val state = gameDao.state() ?: GameStateEntity()
        val catches = gameDao.catches()
        val progress = gameDao.progress()
        val equipped = progress.filter { it.slot != null }.sortedBy { it.slot }.mapNotNull { p ->
            DreamCreatureCatalog.byId(p.creatureId)?.let { EquippedCreature(it, p.level, p.stars) }
        }
        val previousScore = sleepDao.recentProcessed(2).firstOrNull { it.id != session.id }?.score
        val event = activeEvent()
        // Pleine lune évaluée au milieu de la nuit.
        val fullMoon = MoonPhase.isFullMoon((session.startTime + session.wakeTime) / 2)
        val decor = DecorCatalog.layout(
            gameDao.layout().mapNotNull { l -> runCatching { DecorSlot.valueOf(l.slot) }.getOrNull()?.let { it to l.itemId } }.toMap()
        ).values.toList()
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
            stars = progress.associate { it.creatureId to (it.stars to it.shards) },
        )
        val rewards = RewardEngine.process(
            night, weekNights, snapshot, sleepDao.processedCount(), regularity,
            previousScore = previousScore, event = event,
            fullMoon = fullMoon, ritualDone = session.ritualDone, decor = decor,
            expedition = ExpeditionState(state.expeditionRegion, state.expeditionProgress),
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
                expeditionRegion = rewards.expedition.state.regionIndex,
                expeditionProgress = rewards.expedition.state.progress,
            )
        )
        if (rewards.creatures.isNotEmpty()) {
            gameDao.insertCatches(rewards.creatures.map { CreatureCatchEntity(creatureId = it.id, sessionId = session.id, caughtAt = now) })
            gameDao.insertProgress(rewards.creatures.map { CreatureProgressEntity(it.id) })
        }
        rewards.starUpdates.forEach { (id, r) -> gameDao.setStars(id, r.stars, r.shards) }
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
                expeditionSteps = rewards.expedition.stepsGained,
                regionsCompleted = rewards.expedition.completed.joinToString(",") { it.id },
                fullMoon = fullMoon,
                starUps = rewards.starUps.joinToString(",") { "${it.first.id}:${it.second}" },
            )
        )
        return rewards
    }

    companion object {
        /** En dessous de 20 minutes, on considère la nuit comme annulée. */
        const val MIN_EPOCHS = 20
        const val MIN_NAP_EPOCHS = 5
        /** En dessous de 2 h de sommeil, une session automatique est considérée comme une soirée sans dormir. */
        const val AUTO_MIN_SLEEP = 120
    }
}
