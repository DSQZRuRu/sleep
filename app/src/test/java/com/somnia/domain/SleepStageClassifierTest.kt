package com.somnia.domain

import com.somnia.domain.sleep.EpochInput
import com.somnia.domain.sleep.SleepStage
import com.somnia.domain.sleep.SleepStageClassifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SleepStageClassifierTest {

    private fun night(vararg segments: Pair<Int, Float>): List<EpochInput> {
        val out = mutableListOf<EpochInput>()
        segments.forEach { (minutes, motion) -> repeat(minutes) { out += EpochInput(out.size * 60_000L, motion) } }
        return out
    }

    @Test
    fun detectsOnsetDeepAndFinalWake() {
        // 20 min agité, 60 min immobile, 30 min léger, 60 min immobile, 15 min agité
        val epochs = night(20 to 40f, 60 to 0f, 30 to 6f, 60 to 0f, 15 to 40f)
        val a = SleepStageClassifier.classify(epochs)
        assertNotNull(a.sleepOnsetIndex)
        assertTrue("latence ~20 min, obtenu ${a.latencyMin}", a.latencyMin in 18..24)
        assertTrue(a.deepMin >= 80)
        assertTrue(a.lightMin >= 20)
        assertTrue(a.detectedCycles >= 2)
        assertEquals(SleepStage.AWAKE, a.stages.last())
        assertEquals(a.lightMin + a.deepMin, a.totalSleepMin)
    }

    @Test
    fun phoneUseMeansAwake() {
        val epochs = night(30 to 0f).mapIndexed { i, e -> if (i in 10..14) e.copy(phoneUsed = true) else e }
        val a = SleepStageClassifier.classify(epochs)
        (10..14).forEach { assertEquals(SleepStage.AWAKE, a.stages[it]) }
    }

    @Test
    fun allAwakeGivesNoSleep() {
        val a = SleepStageClassifier.classify(night(60 to 50f))
        assertEquals(0, a.totalSleepMin)
    }
}
