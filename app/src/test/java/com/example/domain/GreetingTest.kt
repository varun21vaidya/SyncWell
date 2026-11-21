package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class GreetingTest {
    @Test fun greetingFollowsTheTimeOfDay() {
        assertEquals("Hello", greetingFor(0))
        assertEquals("Hello", greetingFor(4))
        assertEquals("Good morning", greetingFor(5))
        assertEquals("Good morning", greetingFor(11))
        assertEquals("Good afternoon", greetingFor(12))
        assertEquals("Good afternoon", greetingFor(16))
        assertEquals("Good evening", greetingFor(17))
        assertEquals("Good evening", greetingFor(23))
    }

    @Test fun everyHourHasAGreeting() {
        (0..23).forEach { assertEquals(true, greetingFor(it).isNotBlank()) }
    }

    @Test fun dateHeaderIsSentenceCaseAndFallsBackOnBadInput() {
        val saved = Locale.getDefault()
        Locale.setDefault(Locale.US)
        try {
            assertEquals("Sunday 22 November", dateHeader("2026-11-22"))
            assertEquals("not a date", dateHeader("not a date"))
        } finally { Locale.setDefault(saved) }
    }
}
