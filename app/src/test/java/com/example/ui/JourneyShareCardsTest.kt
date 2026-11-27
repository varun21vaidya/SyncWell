package com.example.ui

import com.example.domain.ShareLines
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class JourneyShareCardsTest {
    // Robolectric has no real font, so the width of a text is counted here: 10 per character
    private val tenPerChar = { s: String -> s.length * 10f }

    @Test fun aCardIsTheSameSizeAsTheOtherShareCards() {
        val bmp = JourneyShareCards.render(ShareLines("My promise", listOf("DSA questions", "10 questions by 2026-11-29")))
        assertEquals(ShareCard.W, bmp.width); assertEquals(ShareCard.H, bmp.height)
    }

    @Test fun longTextWrapsInsteadOfRunningOffTheCard() {
        val text = "Land a backend role at a company that ships things people use every single day"
        val lines = JourneyShareCards.wrap(text, 300f, tenPerChar)
        assertTrue(lines.size > 1)
        assertTrue(lines.all { tenPerChar(it) <= 300f })
        assertEquals(text, lines.joinToString(" "))
    }

    @Test fun aSingleWordWiderThanTheCardIsCutNotLost() {
        val word = "x".repeat(100)
        val lines = JourneyShareCards.wrap(word, 300f, tenPerChar)
        assertEquals(listOf(30, 30, 30, 10), lines.map { it.length })
        assertEquals(word, lines.joinToString(""))
    }

    @Test fun aShortLineStaysOnOneLine() = assertEquals(listOf("Day 9 of 14"), JourneyShareCards.wrap("Day 9 of 14", 300f, tenPerChar))

    @Test fun anEmptyLineStillGivesOneLineToDraw() = assertEquals(listOf(""), JourneyShareCards.wrap("", 300f, tenPerChar))
}
