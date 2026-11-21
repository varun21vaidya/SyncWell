package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LinkMatchRuleTest {
    private val items = listOf(
        LinkItem("wake", "Wake up & drink water", "HYGIENE", false),
        LinkItem("focus1", "Focus block", "STUDY", false),
        LinkItem("warm", "Warm-up", "WORKOUT", false),
        LinkItem("gym", "Workout", "WORKOUT", false),
        LinkItem("cool", "Cool-down & shower", "WORKOUT", false),
        LinkItem("creatine", "Creatine", "SUPPLEMENT", false),
        LinkItem("lunch", "Lunch", "DIET", false)
    )

    @Test fun workoutMatchesTheWorkoutItemNotWarmUpOrCoolDown() =
        assertEquals("gym", LinkMatchRule.match(LinkEvent.WORKOUT, items, emptySet())?.id)

    @Test fun focusMatchesTheFirstStudyItem() =
        assertEquals("focus1", LinkMatchRule.match(LinkEvent.FOCUS, items, emptySet())?.id)

    @Test fun waterMatchesAHygieneItemMentioningWater() =
        assertEquals("wake", LinkMatchRule.match(LinkEvent.WATER, items, emptySet())?.id)

    @Test fun supplementMatchesByNameIgnoringCase() {
        assertEquals("creatine", LinkMatchRule.match(LinkEvent.SUPPLEMENT, items, emptySet(), " creatine ")?.id)
        assertNull(LinkMatchRule.match(LinkEvent.SUPPLEMENT, items, emptySet(), "Zinc"))
        assertNull(LinkMatchRule.match(LinkEvent.SUPPLEMENT, items, emptySet(), null))
    }

    @Test fun anAlreadyTickedItemIsNeverOffered() {
        val ticked = items.map { if (it.id == "gym") it.copy(isDone = true) else it }
        assertNull(LinkMatchRule.match(LinkEvent.WORKOUT, ticked, emptySet()))
    }

    @Test fun anItemPromptedTodayIsNotOfferedAgain() =
        assertNull(LinkMatchRule.match(LinkEvent.WORKOUT, items, setOf("gym")))

    @Test fun mealsAreNeverOfferedByAnyEvent() =
        LinkEvent.values().forEach { assertNull(LinkMatchRule.match(it, listOf(items.last()), emptySet(), "Lunch")) }
}
