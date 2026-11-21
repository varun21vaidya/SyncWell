package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NextSplitRuleTest {
    @Test fun freshInstallStartsAtPush() {
        assertEquals(Split.PUSH, NextSplitRule.next(emptyList()))
    }

    @Test fun cyclesPushPullLegsPush() {
        assertEquals(Split.PULL, NextSplitRule.next(listOf(Split.PUSH.label)))
        assertEquals(Split.LEGS, NextSplitRule.next(listOf(Split.PULL.label, Split.PUSH.label)))
        assertEquals(Split.PUSH, NextSplitRule.next(listOf(Split.LEGS.label)))
    }

    @Test fun readsOlderStoredLabels() {
        assertEquals(Split.LEGS, NextSplitRule.splitOf("Legs + Abs Day — Thighs + Calves + Core"))
        assertEquals(Split.PULL, NextSplitRule.splitOf("PULL"))
        assertNull(NextSplitRule.splitOf("Cardio"))
    }

    @Test fun skipsUnknownTypesWhenFindingTheLastSplit() {
        assertEquals(Split.LEGS, NextSplitRule.next(listOf("Mystery", Split.PULL.label)))
    }

    @Test fun lastDoneKeepsTheNewestDatePerSplit() {
        val m = NextSplitRule.lastDone(listOf(Split.PUSH.label to "2026-01-05", Split.PUSH.label to "2026-01-12", Split.PULL.label to "2026-01-07"))
        assertEquals("2026-01-12", m[Split.PUSH])
        assertEquals("2026-01-07", m[Split.PULL])
        assertNull(m[Split.LEGS])
    }
}
