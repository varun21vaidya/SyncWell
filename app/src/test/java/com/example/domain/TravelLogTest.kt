package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TravelLogTest {
    private val name = { i: Int -> listOf("Home Start", "Verdant Trail", "Ironwood Outpost")[i] }

    @Test fun eachEventGetsOneCalmLineAndVacationNone() {
        assertEquals("Day 3 of Verdant Trail", TravelLog.line(JourneyEvent.Step("d", 1, 3), name))
        assertEquals("Reached the gate of Ironwood Outpost", TravelLog.line(JourneyEvent.GateReached("d", 2), name))
        assertEquals("Back to day 7 in Ironwood Outpost", TravelLog.line(JourneyEvent.Setback("d", 2, 9, 7), name))
        assertEquals("Back to the start of Ironwood Outpost", TravelLog.line(JourneyEvent.Setback("d", 2, 9, 0), name))
        assertEquals("Rest day used, no change on the map", TravelLog.line(JourneyEvent.FreezeUsed("d"), name))
        assertNull(TravelLog.line(JourneyEvent.Vacation("d"), name))
    }

    @Test fun theLogNeverSaysStep() {
        val lines = listOf(JourneyEvent.Step("d", 1, 3), JourneyEvent.GateReached("d", 1), JourneyEvent.Setback("d", 1, 5, 3), JourneyEvent.FreezeUsed("d"))
            .mapNotNull { TravelLog.line(it, name) } + TravelLog.crossed("X") + TravelLog.metCommitment("Y") + TravelLog.missedCommitment("Z")
        assertTrue(lines.none { it.contains("step", ignoreCase = true) })
    }

    @Test fun goalLinesNameTheGoal() {
        assertEquals("Met: DSA", TravelLog.metCommitment("DSA"))
        assertEquals("Not met: DSA", TravelLog.missedCommitment("DSA"))
        assertEquals("DSA: two easy ones", TravelLog.progress("DSA", "two easy ones"))
    }
}
