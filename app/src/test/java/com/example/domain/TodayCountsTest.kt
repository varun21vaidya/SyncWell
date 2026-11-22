package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TodayCountsTest {
    @Test fun showsWhenTheDayFirstReachesEightyPercent() {
        assertFalse(TodayCounts.shouldShow(7, 10, null, "2026-11-22", false))
        assertTrue(TodayCounts.shouldShow(8, 10, null, "2026-11-22", false))
    }

    @Test fun showsOnlyOncePerDayEvenIfTheTickIsUndoneAndRedone() {
        assertFalse(TodayCounts.shouldShow(9, 10, "2026-11-22", "2026-11-22", false))
    }

    @Test fun showsAgainTheNextDay() {
        assertTrue(TodayCounts.shouldShow(8, 10, "2026-11-21", "2026-11-22", false))
    }

    @Test fun neverDuringVacationOrWithAnEmptyList() {
        assertFalse(TodayCounts.shouldShow(8, 10, null, "2026-11-22", vacation = true))
        assertFalse(TodayCounts.shouldShow(0, 0, null, "2026-11-22", false))
    }

    @Test fun copyIsWarmAndShort() {
        assertEquals("Today counts. Streak safe.", TodayCounts.MESSAGE)
    }
}
