package com.example.ui

import com.example.domain.JourneyCharacters
import org.junit.Assert.assertEquals
import org.junit.Test

class CharacterLooksTest {
    /** A character added to the pack without its avatar would silently be drawn as Max. */
    @Test fun everyCharacterHasItsOwnAvatar() {
        assertEquals(JourneyCharacters.all.map { it.id }.toSet(), CHARACTER_AVATARS.keys)
    }
}
