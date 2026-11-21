package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeOfDayRuleTest {
    private fun i(id: String, t: String?) = DayItem(id, id, "OTHER", t, false)

    @Test fun groupsByTimeOfDayFromWake() {
        val g = TimeOfDayRule.group(listOf(i("a", "07:00"), i("b", "13:00"), i("c", "19:00"), i("d", "23:30"), i("e", null)), "07:00")
        assertEquals(listOf("Morning", "Afternoon", "Evening", "Night", "Anytime"), g.map { it.label })
    }

    @Test fun lateNightItemAfterMidnightSortsAfterEleven() {
        val g = TimeOfDayRule.group(listOf(i("late", "00:30"), i("eve", "23:00")), "07:00")
        assertEquals(listOf("eve", "late"), g.single().items.map { it.id })
        assertEquals("Night", g.single().label)
    }

    @Test fun emptyGroupsOmitted() {
        assertEquals(listOf("Morning"), TimeOfDayRule.group(listOf(i("a", "08:00")), "07:00").map { it.label })
    }
}
