package com.example.domain

import com.example.data.Goal

/** One day on the trail: done once the character has walked it, with the camp and the gate marked. */
data class Pip(val day: Int, val done: Boolean, val camp: Boolean, val gate: Boolean)

/** The words and pips under the map and on the Home card. Pure, so the layout code only draws what this decides. */
object JourneyRail {

    /** One pip per day of the region. */
    fun pips(region: RegionDef, step: Int): List<Pip> {
        val camp = TravelLog.campDay(region)
        return (1..region.steps).map { Pip(it, done = it <= step, camp = it == camp, gate = it == region.steps) }
    }

    fun daysToGate(region: RegionDef, step: Int) = (region.steps - step).coerceAtLeast(0)

    /** "Gate in 5 days", "Gate in 1 day", or "At the gate". */
    fun gateText(region: RegionDef, step: Int): String = when (val n = daysToGate(region, step)) {
        0 -> "At the gate"
        1 -> "Gate in 1 day"
        else -> "Gate in $n days"
    }

    /** The three labels under the rail: the first day, the camp when the region has one, and the gate. */
    fun labels(region: RegionDef): List<String> = listOfNotNull("Day 1", TravelLog.campDay(region)?.let { "Camp · day $it" }, "Gate · day ${region.steps}")

    /** The day the character is on today: the next one to walk, never past the gate. */
    fun todayDay(region: RegionDef, step: Int) = (step + 1).coerceAtMost(region.steps)

    /**
     * The one-line push on the Home card: days to the gate, and the first reward still waiting on an open commitment
     * ("5 days to the gate. Burger unlocks with DSA 10."). Calm information, no countdown pressure.
     */
    fun motivator(region: RegionDef, step: Int, commitments: List<Goal>): String {
        val left = daysToGate(region, step)
        val head = when (left) { 0 -> "You are at the gate."; 1 -> "1 day to the gate."; else -> "$left days to the gate." }
        val waiting = commitments.firstOrNull { it.status == "PENDING" && it.rewardText != null } ?: return head
        return "$head ${waiting.rewardText} unlocks with ${waiting.title} ${trim(waiting.target)}."
    }

    private fun trim(d: Double) = if (d % 1.0 == 0.0) d.toLong().toString() else "%.1f".format(java.util.Locale.US, d)
}
