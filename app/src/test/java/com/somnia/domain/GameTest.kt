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

class DreamAndEventsTest {
    private fun night(score: Int = 80, sleep: Int = 450, bed: Int = 23 * 60) = com.somnia.domain.game.NightSummary(
        sessionId = 1, nightDay = 1, bedtimeMin = bed, wakeMin = 7 * 60, totalSleepMin = sleep,
        efficiency = 0.92f, score = score, deepRatio = 0.18f, restless = false, targetBedMin = 23 * 60, targetWakeMin = 7 * 60,
    )

    private fun c(id: String) = com.somnia.domain.game.DreamCreatureCatalog.byId(id)!!

    @Test
    fun bonusesOnlyApplyWhenConditionMet() {
        val equipped = listOf(com.somnia.domain.game.EquippedCreature(c("plumeline"), 1)) // coucher à l'heure, +4 %
        val onTime = com.somnia.domain.game.DreamScoreCalculator.compute(night(), null, 0, equipped, com.somnia.domain.game.CompanionStage.EGG, null)
        assertEquals(83, onTime.total)
        val late = com.somnia.domain.game.DreamScoreCalculator.compute(night(bed = 1 * 60), null, 0, equipped, com.somnia.domain.game.CompanionStage.EGG, null)
        assertEquals(80, late.total)
    }

    @Test
    fun noDreamRewardsWithoutRealSleep() {
        val equipped = listOf(com.somnia.domain.game.EquippedCreature(c("abyssin"), 10))
        val d = com.somnia.domain.game.DreamScoreCalculator.compute(night(score = 30, sleep = 120), null, 5, equipped, com.somnia.domain.game.CompanionStage.ASTRAL, null)
        assertEquals(30, d.total)
        assertEquals(0, d.stardust)
    }

    @Test
    fun christmasEventSpansNewYear() {
        val e = com.somnia.domain.game.EventCalendar.byId("givres")!!
        assertTrue(e.isActive(java.time.LocalDate.of(2026, 12, 24)))
        assertTrue(e.isActive(java.time.LocalDate.of(2027, 1, 2)))
        assertTrue(!e.isActive(java.time.LocalDate.of(2026, 11, 9)))
        assertEquals(null, com.somnia.domain.game.EventCalendar.active(java.time.LocalDate.of(2026, 11, 9)))
    }

    @Test
    fun levelingIncreasesBonusAndCost() {
        val p = com.somnia.domain.game.CreatureProgression
        assertTrue(p.bonusPct(c("onirion"), 10) > p.bonusPct(c("onirion"), 1))
        assertTrue(p.upgradeCost(c("onirion"), 2) > p.upgradeCost(c("plumeline"), 2))
        assertEquals(1, p.bedSlots(1)); assertEquals(3, p.bedSlots(6))
    }
}
