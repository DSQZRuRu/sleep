package com.somnia.domain

import com.somnia.domain.game.BonusKind
import com.somnia.domain.game.CompanionStage
import com.somnia.domain.game.DecorCatalog
import com.somnia.domain.game.DecorSlot
import com.somnia.domain.game.DreamScoreCalculator
import com.somnia.domain.game.Expedition
import com.somnia.domain.game.ExpeditionState
import com.somnia.domain.game.GameSnapshot
import com.somnia.domain.game.NightSummary
import com.somnia.domain.game.RewardEngine
import com.somnia.domain.game.StreakState
import com.somnia.domain.sleep.FactorInsights
import com.somnia.domain.sleep.FactorNight
import com.somnia.domain.sleep.MoonPhase
import com.somnia.domain.sleep.SleepFactor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZonedDateTime
import java.time.ZoneOffset

class FeaturesTest {
    private fun night(score: Int = 80, sleep: Int = 450, bed: Int = 23 * 60) = NightSummary(
        sessionId = 1, nightDay = 1, bedtimeMin = bed, wakeMin = 7 * 60, totalSleepMin = sleep,
        efficiency = 0.92f, score = score, deepRatio = 0.18f, restless = false, targetBedMin = 23 * 60, targetWakeMin = 7 * 60,
    )

    private fun ms(y: Int, m: Int, d: Int, h: Int) = ZonedDateTime.of(y, m, d, h, 0, 0, 0, ZoneOffset.UTC).toInstant().toEpochMilli()

    @Test
    fun moonPhaseMatchesKnownDates() {
        // Pleine lune du 24 novembre 2026 et nouvelle lune du 9 novembre 2026.
        assertTrue(MoonPhase.isFullMoon(ms(2026, 11, 24, 15)))
        assertFalse(MoonPhase.isFullMoon(ms(2026, 11, 9, 7)))
        val f = MoonPhase.fraction(ms(2026, 11, 9, 7))
        assertTrue("nouvelle lune attendue, fraction=$f", f < 0.05 || f > 0.95)
    }

    @Test
    fun factorInsightsNeedEnoughNights() {
        val sport = setOf(SleepFactor.SPORT)
        val nights = List(4) { FactorNight(sport, 8, 470, 82) } + List(4) { FactorNight(emptySet(), 20, 430, 72) }
        val insight = FactorInsights.compute(nights).first { it.factor == SleepFactor.SPORT }
        assertEquals(-12, insight.latencyDelta)
        assertEquals(40, insight.sleepDelta)
        assertTrue(insight.notable)
        assertTrue(FactorInsights.compute(nights.take(5)).none { it.factor == SleepFactor.SPORT })
    }

    @Test
    fun phoneOnNightstandIsDetected() {
        assertTrue(FactorInsights.phoneProbablyNotOnBed(List(300) { 0.5f }))
        assertFalse(FactorInsights.phoneProbablyNotOnBed(List(300) { if (it % 20 == 0) 12f else 0f }))
    }

    @Test
    fun expeditionCompletesRegionsAndCarriesOver() {
        val r = Expedition.advance(ExpeditionState(0, 2), 3) // prairie : 3 étapes
        assertEquals(1, r.state.regionIndex)
        assertEquals(2, r.state.progress)
        assertEquals("prairie", r.completed.single().id)
        assertEquals(0, Expedition.stepsForNight(night(sleep = 120)))
        assertEquals(3, Expedition.stepsForNight(night()))
        val end = Expedition.advance(ExpeditionState(Expedition.regions.size, 9), 11)
        assertEquals(160, end.stardust) // 2 voyages libres
    }

    @Test
    fun decorDefaultsAndBonus() {
        val layout = DecorCatalog.layout(mapOf(DecorSlot.QUILT to "quilt_stars"))
        assertEquals("quilt_stars", layout.getValue(DecorSlot.QUILT).id)
        assertTrue(layout.getValue(DecorSlot.LAMP).isDefault)
        val d = DreamScoreCalculator.compute(night(), null, 0, emptyList(), CompanionStage.EGG, null, layout.values.toList())
        assertEquals(82, d.total) // couette étoilée +3 % si profond ≥ 15 %
        assertTrue(DecorCatalog.all.filter { it.bonus != null }.all { it.bonusPct in 1..3 && it.bonus != BonusKind.STARDUST || it.bonusPct <= 3 })
    }

    @Test
    fun ritualXpOnlyWithRealSleep() {
        val snap = GameSnapshot(0, StreakState(), 0, emptySet(), 0, emptySet(), 0, emptySet())
        val ok = RewardEngine.process(night(), listOf(night()), snap, 1, 0, ritualDone = true)
        assertTrue(ok.xpLines.any { it.label == "Rituel du soir" })
        val short = RewardEngine.process(night(sleep = 100), listOf(night()), snap, 1, 0, ritualDone = true)
        assertTrue(short.xpLines.none { it.label == "Rituel du soir" })
    }
}
