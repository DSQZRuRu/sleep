package com.somnia.domain

import com.somnia.domain.sleep.BedtimeCalculator
import com.somnia.domain.sleep.ChronotypeQuestionnaire
import com.somnia.domain.sleep.Chronotype
import com.somnia.domain.sleep.TimeMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileTest {
    @Test
    fun bedtimeOptionsFor7am() {
        val opts = BedtimeCalculator.options(7 * 60, 90, 15, 5, null, null)
        val five = opts.first { it.cycles == 5 }
        assertEquals("23:15", TimeMath.format(five.bedtimeMin))
        assertTrue(five.recommended && five.ideal)
        assertTrue(!opts.first { it.cycles == 4 }.recommended)
    }

    @Test
    fun constraintAcrossMidnight() {
        assertTrue(BedtimeCalculator.fits(23 * 60 + 30, 22 * 60, 0))
        assertTrue(!BedtimeCalculator.fits(21 * 60, 22 * 60, 0))
        assertTrue(BedtimeCalculator.fits(15, 22 * 60, 60))
    }

    @Test
    fun circularMeanAroundMidnight() {
        assertEquals(0, TimeMath.circularMean(listOf(23 * 60 + 30, 30)))
    }

    @Test
    fun chronotypeExtremes() {
        assertEquals(Chronotype.DEFINITE_MORNING, ChronotypeQuestionnaire.classify(ChronotypeQuestionnaire.maxScore))
        assertEquals(Chronotype.DEFINITE_EVENING, ChronotypeQuestionnaire.classify(ChronotypeQuestionnaire.minScore))
    }
}
