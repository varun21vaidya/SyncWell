package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import com.example.domain.WeekMark.COUNTED
import com.example.domain.WeekMark.EMPTY
import com.example.domain.WeekMark.TODAY_IN_PROGRESS

class WeekBarTest {
    // 2026-11-16 is a Monday
    @Test fun countedDaysAndTodayInProgress() {
        val marks = WeekBar.of("2026-11-19", setOf("2026-11-16", "2026-11-18")) // Thursday
        assertEquals(listOf(COUNTED, EMPTY, COUNTED, TODAY_IN_PROGRESS, EMPTY, EMPTY, EMPTY), marks)
    }

    @Test fun todayThatAlreadyCountsIsCountedNotInProgress() {
        val marks = WeekBar.of("2026-11-19", setOf("2026-11-19"))
        assertEquals(COUNTED, marks[3])
    }

    @Test fun mondayStartsAFreshWeekAndSundayIsTheLastSlot() {
        assertEquals(listOf(TODAY_IN_PROGRESS, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY), WeekBar.of("2026-11-16", emptySet()))
        assertEquals(TODAY_IN_PROGRESS, WeekBar.of("2026-11-22", emptySet())[6])
    }

    @Test fun datesOfOtherWeeksAndTheFutureNeverCount() {
        val marks = WeekBar.of("2026-11-17", setOf("2026-11-10", "2026-11-20")) // last week, and a future day
        assertEquals(listOf(EMPTY, TODAY_IN_PROGRESS, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY), marks)
    }
}
