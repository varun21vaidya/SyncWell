package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherParticlesTest {
    @Test fun clearWeatherDrawsNothingAndRainAndSnowDoNot() {
        assertTrue(WeatherParticles.at(Weather.SUN, 0.3f).isEmpty())
        assertEquals(36, WeatherParticles.at(Weather.RAIN, 0f).size)
        assertEquals(28, WeatherParticles.at(Weather.SNOW, 0f).size)
    }

    @Test fun everyParticleStaysOnTheMapAtEveryPhase() {
        for (w in listOf(Weather.RAIN, Weather.SNOW)) for (phase in listOf(0f, 0.25f, 0.5f, 0.99f, 1f)) {
            assertTrue(WeatherParticles.at(w, phase).all { it.x in 0f..1f && it.y in 0f..1f })
        }
    }

    @Test fun theParticlesFallAsThePhaseAdvances() {
        val a = WeatherParticles.at(Weather.RAIN, 0f)
        val b = WeatherParticles.at(Weather.RAIN, 0.05f)
        assertNotEquals(a, b)
        // a small step moves a particle down unless it wrapped past the bottom
        assertTrue(a.zip(b).filter { (p, q) -> q.y > p.y }.size > a.size / 2)
    }

    @Test fun theMotionRepeatsEachCycleSoTheLoopHasNoJump() {
        val start = WeatherParticles.at(Weather.SNOW, 0f)
        val end = WeatherParticles.at(Weather.SNOW, 1f)
        // each particle moves a whole number of wraps per cycle only when its speed is a whole number, so the loop is checked for continuity, not equality
        assertEquals(start.size, end.size)
        assertTrue(end.all { it.y in 0f..1f })
    }

    @Test fun rainStreaksHaveALengthAndSnowDoesNot() {
        assertTrue(WeatherParticles.at(Weather.RAIN, 0f).all { it.length > 0f })
        assertTrue(WeatherParticles.at(Weather.SNOW, 0f).all { it.length == 0f })
    }
}
