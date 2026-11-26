package com.example.domain

import com.example.data.Goal

/** What a Warden says at the gate. If the user set an ambition or identity line, the Warden quotes it; otherwise the lines stay generic. */
object GateScript {

    /** The challenge, then the user's own words when they wrote any. */
    fun opening(w: Warden, ambition: String?, identity: String?): List<String> = listOfNotNull(
        w.challenge,
        ambition?.trim()?.takeIf { it.isNotEmpty() }?.let { "You told me you are after \"$it\"." },
        identity?.trim()?.takeIf { it.isNotEmpty() }?.let { "And you are becoming someone who \"${it.removePrefix("I'm becoming someone who ").removePrefix("I am becoming someone who ")}\"." }
    )

    fun verdict(w: Warden, allMet: Boolean) = if (allMet) w.open else w.closed
}

/** Goals that were decided since the user last looked: shown one at a time, oldest window first. */
object ReviewQueue {
    fun unseen(goals: List<Goal>, seen: Set<String>): List<Goal> =
        goals.filter { it.kind == "COMMITMENT" && it.status != "PENDING" && it.id !in seen }.sortedBy { it.windowEnd }

    /** Pass or fail and what changed, in plain words. [logged] is the total logged inside the window. */
    fun describe(g: Goal, logged: Double): String {
        val got = "You logged ${trim(logged)} of ${trim(g.target)}${g.targetLabel?.let { " $it" } ?: ""} by ${g.windowEnd}."
        return when (g.status) {
            "MET" -> got + (g.rewardText?.let { " Your reward is unlocked: $it." } ?: "")
            else -> "$got The character goes back to the start of the region." + (g.rewardText?.let { " Your reward \"$it\" carries over to the next try." } ?: "")
        }
    }

    private fun trim(d: Double) = if (d % 1.0 == 0.0) d.toLong().toString() else "%.1f".format(java.util.Locale.US, d)
}

object SetbackNote {
    /** Explains a move back on the map, from the position the user last saw to the one now. Null when the character did not go back. */
    fun of(regionName: String, seenStep: Int, nowStep: Int, campDay: Int?): String? = when {
        nowStep >= seenStep -> null
        nowStep == 0 -> "Back to the start of $regionName. Only the map position changes: your level, badges and stamps are safe."
        campDay != null && nowStep == campDay -> "Back to the camp on day $campDay. A missed day goes to the last camp, and only the map position changes."
        else -> "Back to day $nowStep. Only the map position changes: your level, badges and stamps are safe."
    }
}
