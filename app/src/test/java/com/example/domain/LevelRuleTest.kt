package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class LevelRuleTest {
    @Test fun thresholdsAreThreeSevenThenEveryFifteen() {
        listOf(0 to 0, 2 to 0, 3 to 1, 6 to 1, 7 to 2, 21 to 2, 22 to 3, 36 to 3, 37 to 4, 52 to 5, 97 to 8).forEach { (days, level) ->
            assertEquals("days=$days", level, LevelRule.of(days).level)
        }
    }

    @Test fun namesFollowTheLadderAndStopAtLegend() {
        assertEquals("Starter", LevelRule.of(3).name)
        assertEquals("Consistent", LevelRule.of(37).name)
        assertEquals("Legend", LevelRule.of(97).name)
        assertEquals("Legend", LevelRule.of(400).name)
    }

    @Test fun progressInsideALevel() {
        val r = LevelRule.of(30) // level 3 began at 22, level 4 starts at 37
        assertEquals(8, r.daysIntoLevel)
        assertEquals(7, r.daysToNext)
        assertEquals(3, LevelRule.of(0).daysToNext)
    }

    @Test fun negativeCountIsTreatedAsZero() {
        assertEquals(0, LevelRule.of(-5).level)
    }
}
