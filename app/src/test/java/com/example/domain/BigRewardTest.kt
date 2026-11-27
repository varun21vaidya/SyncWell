package com.example.domain

import com.example.data.JourneyState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BigRewardTest {
    private fun state(start: String? = "2026-11-01", target: String? = "2026-12-01", reward: String? = "Gaming") =
        JourneyState(enabled = true, ambitionText = "Land a role", ambitionStart = start, targetDate = target, bigRewardText = reward)

    @Test fun underSixtySixDaysItUnlocksOnTheTargetDate() {
        assertEquals(BigRewardRule.OnDate("2027-01-05"), BigReward.rule("2026-11-01", "2027-01-05")) // 65 days
    }

    @Test fun sixtySixDaysOrMoreItUnlocksAtTheSummit() {
        assertEquals(BigRewardRule.AtSummit, BigReward.rule("2026-11-01", "2027-01-06")) // exactly 66 days
        assertEquals(BigRewardRule.AtSummit, BigReward.rule("2026-11-01", "2027-06-01"))
    }

    @Test fun theWordsSayWhenTheThingComesBack() {
        assertEquals("It comes back on 2026-12-01.", BigReward.explain("2026-11-01", "2026-12-01"))
        assertEquals("It comes back when you reach the Summit.", BigReward.explain("2026-11-01", "2027-06-01"))
    }

    @Test fun aNearDateIsDueOnThatDayNotBefore() {
        assertFalse(BigReward.due(state(), "2026-11-30", summitReached = false))
        assertTrue(BigReward.due(state(), "2026-12-01", summitReached = false))
        assertTrue(BigReward.due(state(), "2026-12-09", summitReached = false))
        assertTrue(!BigReward.due(state(), "2026-12-09", summitReached = false).not())
    }

    @Test fun aFarDateWaitsForTheSummitNoMatterHowLateItGets() {
        val far = state(target = "2027-06-01")
        assertFalse(BigReward.due(far, "2027-07-01", summitReached = false))
        assertTrue(BigReward.due(far, "2026-11-20", summitReached = true))
    }

    @Test fun nothingIsDueWithoutARewardADateOrAStart() {
        assertFalse(BigReward.due(state(reward = null), "2030-01-01", true))
        assertFalse(BigReward.due(state(target = null), "2030-01-01", true))
        assertFalse(BigReward.due(state(start = null), "2030-01-01", true))
    }

    @Test fun aTargetDateMustBeARealDateFromTodayOn() {
        assertEquals("2026-12-06", Ambition.cleanDate(" 2026-12-06 ", "2026-11-26"))
        assertEquals("2026-11-26", Ambition.cleanDate("2026-11-26", "2026-11-26"))
        assertNull(Ambition.cleanDate("2026-11-25", "2026-11-26")) // yesterday
        assertNull(Ambition.cleanDate("2026-02-30", "2026-01-01")) // not a day that exists
        assertNull(Ambition.cleanDate("soon", "2026-11-26")); assertNull(Ambition.cleanDate("", "2026-11-26")); assertNull(Ambition.cleanDate(null, "2026-11-26"))
    }
}
