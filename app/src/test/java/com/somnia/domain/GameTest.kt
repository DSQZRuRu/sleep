package com.somnia.domain

import com.somnia.domain.game.GameSnapshot
import com.somnia.domain.game.NightSummary
import com.somnia.domain.game.RewardEngine
import com.somnia.domain.game.StreakManager
import com.somnia.domain.game.StreakState
import com.somnia.domain.game.XpCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GameTest {
    private fun night(day: Long, bed: Int = 23 * 60, sleep: Int = 450, eff: Float = 0.92f) = NightSummary(
        sessionId = day, nightDay = day, bedtimeMin = bed, wakeMin = 7 * 60, totalSleepMin = sleep,
        efficiency = eff, score = 80, deepRatio = 0.18f, restless = false, targetBedMin = 23 * 60, targetWakeMin = 7 * 60,
    )

    @Test
    fun streakWithWeeklyFreeze() {
        // 2026-11-09 est un lundi
        val monday = java.time.LocalDate.of(2026, 11, 9).toEpochDay()
        var s = StreakState()
        s = StreakManager.apply(s, night(monday)).state
        s = StreakManager.apply(s, night(monday + 1)).state
        assertEquals(2, s.current)
        // nuit ratée le mercredi → protection utilisée, série conservée
        val r = StreakManager.apply(s, night(monday + 3))
        assertTrue(r.freezeUsed)
        assertEquals(3, r.state.current)
        // nouvelle nuit ratée la même semaine → la série repart
        val r2 = StreakManager.apply(r.state, night(monday + 5))
        assertEquals(1, r2.state.current)
    }

    @Test
    fun noRewardForLyingInBedAwake() {
        // 11 h au lit mais 2 h de sommeil réel
        val n = night(1, sleep = 120, eff = 0.18f)
        assertTrue(XpCalculator.forNight(n).none { it.label == "Durée de sommeil" || it.label == "Sommeil efficace" })
        assertTrue(RewardEngine.rollCreatures(n, Random(1)).isEmpty())
    }

    @Test
    fun oversleepingIsNotRewardedMore() {
        val good = XpCalculator.forNight(night(1, sleep = 480)).sumOf { it.xp }
        val long = XpCalculator.forNight(night(1, sleep = 660)).sumOf { it.xp }
        assertTrue(good > long)
    }

    @Test
    fun levels() {
        assertEquals(1, XpCalculator.levelFor(0).level)
        assertEquals(2, XpCalculator.levelFor(100).level)
        assertEquals(3, XpCalculator.levelFor(260).level)
    }

    @Test
    fun processGivesCreaturesAndFirstBadge() {
        val n = night(20000)
        val snap = GameSnapshot(0, StreakState(), 0, emptySet(), 0, emptySet(), 0, emptySet())
        val r = RewardEngine.process(n, listOf(n), snap, nightsCount = 1, regularityScore = 0)
        assertTrue(r.creatures.isNotEmpty())
        assertTrue(r.newBadges.any { it.id == "first_night" })
        assertTrue(r.xpTotal > 0)
    }
}
