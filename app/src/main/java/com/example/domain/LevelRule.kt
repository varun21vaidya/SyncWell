package com.example.domain

data class LevelResult(val level: Int, val name: String, val daysIntoLevel: Int, val daysToNext: Int)

/** Level comes from the total number of qualifying days: 3, 7, then every 15 (22, 37, 52 ...). */
object LevelRule {
    private val names = listOf("Starter", "Rhythm", "Habit Builder", "Consistent", "Disciplined", "Relentless", "Unshakeable", "Legend")

    private fun threshold(level: Int) = when (level) {
        0 -> 0
        1 -> 3
        else -> 7 + 15 * (level - 2)
    }

    fun of(qualifyingDays: Int): LevelResult {
        val days = qualifyingDays.coerceAtLeast(0)
        val level = when {
            days < 3 -> 0
            days < 7 -> 1
            else -> 2 + (days - 7) / 15
        }
        // Names beyond level 8 are not defined yet, so the number keeps going and the name stays Legend.
        val name = if (level == 0) "Getting started" else names[minOf(level, names.size) - 1]
        return LevelResult(level, name, days - threshold(level), threshold(level + 1) - days)
    }
}
