package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonalBestRuleTest {
    private fun s(w: Double, r: Int) = SetRef(w, r)

    @Test fun heavierWeightBeatsAndSameWeightNeedsMoreReps() {
        assertTrue(PersonalBestRule.beats(s(62.5, 5), s(60.0, 12)))
        assertTrue(PersonalBestRule.beats(s(60.0, 9), s(60.0, 8)))
        assertTrue(!PersonalBestRule.beats(s(60.0, 8), s(60.0, 8)))
    }

    @Test fun noEarlierHistoryMarksNothing() {
        assertNull(PersonalBestRule.newBestIndex(null, listOf(s(60.0, 8))))
    }

    @Test fun onlyTheBestSetIsMarkedOnce() {
        val sets = listOf(s(60.0, 8), s(62.5, 6), s(62.5, 6), s(60.0, 8))
        assertEquals(1, PersonalBestRule.newBestIndex(s(60.0, 8), sets)) // the earliest of the tied best sets
    }

    @Test fun equalOrLowerThanTheEarlierBestIsNotMarked() {
        assertNull(PersonalBestRule.newBestIndex(s(62.5, 6), listOf(s(62.5, 6), s(60.0, 10))))
    }

    @Test fun historyCountsEachNewBestOverEveryEarlierSession() {
        val rows = listOf(
            HistoricSet("a", "2026-01-01", "bench", 60.0, 8),
            HistoricSet("b", "2026-01-08", "bench", 60.0, 8),   // equal: not a record
            HistoricSet("c", "2026-01-15", "bench", 62.5, 6),   // record
            HistoricSet("c", "2026-01-15", "squat", 80.0, 5),   // first squat ever: not a record
            HistoricSet("d", "2026-01-22", "bench", 60.0, 10),  // lighter: not a record
        )
        assertEquals(mapOf("c" to 1), PersonalBestRule.newBestsBySession(rows))
    }

    @Test fun historyIsOrderedByTimeNotByInputOrder() {
        val rows = listOf(
            HistoricSet("late", "2026-02-01", "bench", 70.0, 5),
            HistoricSet("early", "2026-01-01", "bench", 60.0, 5),
        )
        assertEquals(mapOf("late" to 1), PersonalBestRule.newBestsBySession(rows))
    }
}
