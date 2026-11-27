package com.example.domain

/** The map: steps per region, and the step each region's checkpoint sits on (null = none). Regions after the Summit repeat the last row. */
object JourneyRoute {
    private val steps = listOf(3, 7, 14, 14, 14, 14) // Home Start, Verdant, Ironwood, Tidewater, Obsidian, Summit region
    private val checkpoints = listOf<Int?>(null, 3, 7, 7, 7, 7)
    const val SUMMIT_REGION = 5 // its gate is the Summit
    const val SUMMIT_STEPS = 66 // 3 + 7 + 14 + 14 + 14 + 14

    fun stepsIn(region: Int) = steps.getOrElse(region) { steps.last() }
    fun checkpointOf(region: Int): Int? = checkpoints.getOrElse(region) { checkpoints.last() }

    /** Steps walked since Home Start, across regions. Reaches [SUMMIT_STEPS] at the Summit gate. */
    fun totalSteps(p: JourneyPosition) = (0 until p.regionIndex).sumOf { stepsIn(it) } + p.stepInRegion
}

/** Where the character stands. [freezeWeek] is the Monday of the week whose freeze is already used. */
data class JourneyPosition(
    val regionIndex: Int = 0,
    val stepInRegion: Int = 0,
    val freezeWeek: String? = null
) {
    val atGate get() = stepInRegion >= JourneyRoute.stepsIn(regionIndex)
}

/** One finished day as the Journey needs it. [covered] = already protected by a recorded streak freeze. */
data class JourneyDay(val date: String, val qualifies: Boolean, val vacation: Boolean = false, val covered: Boolean = false)

sealed class JourneyEvent {
    abstract val date: String
    data class Step(override val date: String, val region: Int, val day: Int) : JourneyEvent()
    data class Vacation(override val date: String) : JourneyEvent()
    data class FreezeUsed(override val date: String) : JourneyEvent()
    data class Setback(override val date: String, val region: Int, val fromStep: Int, val toStep: Int) : JourneyEvent()
    data class GateReached(override val date: String, val region: Int) : JourneyEvent()
}

data class JourneyAdvance(val position: JourneyPosition, val events: List<JourneyEvent>)

object JourneyRules {

    /** One finished day. A qualifying day adds a step; a vacation or covered day is skipped; a missed day uses the weekly freeze, then falls back to the checkpoint. */
    fun advance(p: JourneyPosition, day: JourneyDay): JourneyAdvance = when {
        day.qualifies -> step(p, day.date)
        day.vacation -> JourneyAdvance(p, listOf(JourneyEvent.Vacation(day.date)))
        day.covered -> JourneyAdvance(p, listOf(JourneyEvent.FreezeUsed(day.date)))
        else -> missed(p, day.date)
    }

    private fun step(p: JourneyPosition, date: String): JourneyAdvance {
        if (p.atGate) return JourneyAdvance(p, emptyList()) // waiting at the gate: more good days do not add steps
        val next = p.copy(stepInRegion = p.stepInRegion + 1)
        val events = buildList {
            add(JourneyEvent.Step(date, next.regionIndex, next.stepInRegion))
            if (next.atGate) add(JourneyEvent.GateReached(date, next.regionIndex))
        }
        return JourneyAdvance(next, events)
    }

    private fun missed(p: JourneyPosition, date: String): JourneyAdvance {
        val week = WeekKey.of(date)
        if (p.freezeWeek != week) return JourneyAdvance(p.copy(freezeWeek = week), listOf(JourneyEvent.FreezeUsed(date)))
        if (p.regionIndex == 0) return JourneyAdvance(p, emptyList()) // Home Start has no penalties
        val back = JourneyRoute.checkpointOf(p.regionIndex)?.takeIf { p.stepInRegion >= it } ?: 0
        if (back == p.stepInRegion) return JourneyAdvance(p, emptyList())
        return JourneyAdvance(p.copy(stepInRegion = back), listOf(JourneyEvent.Setback(date, p.regionIndex, p.stepInRegion, back)))
    }

    /**
     * Processes every day after [lastProcessed] up to yesterday, oldest first. Today is never processed, so it never counts as missed,
     * and a day with no stored result is a missed day. Opening the app on Monday or on Wednesday gives the same position.
     * Returns the new position, the events for animations, and the new last processed date.
     */
    fun catchUp(p: JourneyPosition, lastProcessed: String, today: String, dayOn: (String) -> JourneyDay?): Triple<JourneyPosition, List<JourneyEvent>, String> {
        val yesterday = WeekKey.addDays(today, -1)
        var pos = p
        val events = mutableListOf<JourneyEvent>()
        for (date in WeekKey.range(WeekKey.addDays(lastProcessed, 1), yesterday)) {
            val r = advance(pos, dayOn(date) ?: JourneyDay(date, qualifies = false))
            pos = r.position
            events += r.events
        }
        return Triple(pos, events, maxOf(lastProcessed, yesterday))
    }

    /** Crossing a gate needs every set commitment met. Returns null while the character is not at the gate or a commitment is open. */
    fun crossGate(p: JourneyPosition, commitmentsMet: Boolean): JourneyPosition? =
        if (p.atGate && commitmentsMet) p.copy(regionIndex = p.regionIndex + 1, stepInRegion = 0) else null

    /** The Summit has been crossed once the character is past Region 6. */
    fun summitReached(p: JourneyPosition) = p.regionIndex > JourneyRoute.SUMMIT_REGION

    /** A missed commitment goes back to the start of its region, even after the campfire. */
    fun commitmentMissed(p: JourneyPosition): JourneyPosition = p.copy(stepInRegion = 0)

    /** Back from a pause or mode off: the start of the current region. Earlier regions stay. */
    fun resume(p: JourneyPosition): JourneyPosition = p.copy(stepInRegion = 0)
}

enum class GoalMeasure { COUNT, DURATION, MILESTONE, PERCENT }
enum class GoalStatus { PENDING, MET, MISSED }

data class GoalEntry(val date: String, val amount: Double)

data class GoalSpec(val id: String, val measure: GoalMeasure, val target: Double, val windowStart: String, val windowEnd: String, val status: GoalStatus = GoalStatus.PENDING)

object GoalReview {
    /** "2", "1,5" or "0.5" to an amount; null when empty, not a number, not above 0 or absurdly large. */
    fun parseAmount(text: String): Double? =
        text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it > 0.0 && it <= 100_000.0 }

    /** Entries inside the window only. Count and duration add up, percent uses the latest entry, a milestone is met by any entry that reaches the target. */
    fun progress(goal: GoalSpec, entries: List<GoalEntry>): Double {
        val inside = entries.filter { it.date >= goal.windowStart && it.date <= goal.windowEnd }
        return when (goal.measure) {
            GoalMeasure.COUNT, GoalMeasure.DURATION -> inside.sumOf { it.amount }
            GoalMeasure.PERCENT -> inside.maxByOrNull { it.date }?.amount ?: 0.0
            GoalMeasure.MILESTONE -> inside.maxOfOrNull { it.amount } ?: 0.0
        }
    }

    /** Met as soon as the target is reached. Past the window end and short of it: missed. Otherwise still pending. Only reads the logged data, so a late open gives the same answer. */
    fun status(goal: GoalSpec, entries: List<GoalEntry>, today: String): GoalStatus = when {
        goal.target <= 0 -> GoalStatus.PENDING
        progress(goal, entries) >= goal.target -> GoalStatus.MET
        today > goal.windowEnd -> GoalStatus.MISSED
        else -> GoalStatus.PENDING
    }

    /** Goals whose window has ended and that are still pending in storage, oldest window end first, for the review queue. */
    fun reviewQueue(goals: List<GoalSpec>, entriesOf: (String) -> List<GoalEntry>, today: String): List<Pair<GoalSpec, GoalStatus>> =
        goals.filter { it.status == GoalStatus.PENDING && today > it.windowEnd }
            .sortedBy { it.windowEnd }
            .map { it to status(it, entriesOf(it.id), today) }
}
