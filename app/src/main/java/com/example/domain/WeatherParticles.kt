package com.example.domain

import kotlin.math.PI
import kotlin.math.sin

/** One drop or flake, as fractions of the map: where it is and how long a rain streak is. */
data class Particle(val x: Float, val y: Float, val length: Float)

/** The light weather laid over a painted region. A fixed set of particles moving with a phase, so it is cheap and the same on every device. */
object WeatherParticles {
    fun count(weather: Weather) = when (weather) { Weather.RAIN -> 36; Weather.SNOW -> 28; Weather.SUN -> 0 }

    /** The particles at [phase] (0 to 1, repeating). Clear weather has none. */
    fun at(weather: Weather, phase: Float): List<Particle> = List(count(weather)) { i ->
        val speed = 0.6f + (i % 5) * 0.1f
        val y = frac(i * 0.37f + phase * speed)
        when (weather) {
            Weather.RAIN -> Particle(frac(i * 0.618f - y * 0.06f), y, 0.04f)
            else -> Particle(frac(i * 0.618f + 0.02f * sin(2 * PI * (phase + i * 0.1)).toFloat()), y, 0f) // snow drifts side to side
        }
    }

    private fun frac(v: Float): Float = v - kotlin.math.floor(v)
}
