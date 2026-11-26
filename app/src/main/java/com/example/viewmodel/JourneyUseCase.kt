package com.example.viewmodel

import androidx.room.withTransaction
import com.example.data.AppDatabase
import com.example.data.Goal
import com.example.data.GoalProgress
import com.example.data.JourneyNote
import com.example.data.JourneyState
import com.example.data.PassportStamp
import com.example.data.SyncWellDao
import com.example.domain.GoalEntry
import com.example.domain.GoalMeasure
import com.example.domain.GoalReview
import com.example.domain.GoalSpec
import com.example.domain.GoalStatus
import com.example.domain.JourneyDay
import com.example.domain.JourneyEvent
import com.example.domain.JourneyPosition
import com.example.domain.JourneyRules
import com.example.domain.WeekKey
import java.util.UUID

/**
 * Connects the pure Journey rules to the database: turning the journey on or off, catching up the days that have ended,
 * reviewing goals, crossing a gate and claiming a reward. Each write is one transaction.
 */
class JourneyUseCase(private val db: AppDatabase, private val dao: SyncWellDao) {

    private fun JourneyState.position() = JourneyPosition(regionIndex, stepInRegion, freezeUsedWeekStart)
    private fun JourneyState.with(p: JourneyPosition) =
        copy(regionIndex = p.regionIndex, stepInRegion = p.stepInRegion, freezeUsedWeekStart = p.freezeWeek, furthestRegion = maxOf(furthestRegion, p.regionIndex))

    /** Turns the journey on. A new journey starts at Home Start with no backfill; coming back from a pause restarts the current region. */
    suspend fun enable(today: String, characterId: String) = db.withTransaction {
        val yesterday = WeekKey.addDays(today, -1)
        val old = dao.getJourneyState()
        val state = if (old == null) JourneyState(enabled = true, characterId = characterId, lastProcessedDate = yesterday)
        else old.with(JourneyRules.resume(old.position())).copy(enabled = true, characterId = characterId, lastProcessedDate = yesterday)
        dao.upsertJourneyState(state)
    }

    suspend fun disable() = db.withTransaction { dao.getJourneyState()?.let { dao.upsertJourneyState(it.copy(enabled = false)) } }

    /** Applies every day that has ended since the last time. Today is never counted. Returns the events, for the walking animation. */
    suspend fun sync(today: String): List<JourneyEvent> = db.withTransaction {
        val state = dao.getJourneyState()?.takeIf { it.enabled && it.lastProcessedDate != null } ?: return@withTransaction emptyList()
        val vacation = dao.getAllVacationDays().map { it.date }.toSet() // freezes are stored here too
        val rows = dao.getAllDailyAnalytics().associateBy { it.dateKey }
        val (position, events, last) = JourneyRules.catchUp(state.position(), state.lastProcessedDate!!, today) { date ->
            val row = rows[date]
            val off = (row?.isVacation == true) || date in vacation
            if (row != null || off) JourneyDay(date, row?.let { com.example.domain.DayCompletion.qualifies(it.routineCompletionPct) } ?: false, vacation = off) else null
        }
        dao.upsertJourneyState(state.with(position).copy(lastProcessedDate = last))
        events
    }

    suspend fun saveGoal(goal: Goal) = dao.insertGoals(listOf(goal))

    /** Adds one dated progress entry (manual; focus time is added automatically for linked goals). */
    suspend fun addProgress(goalId: String, date: String, amount: Double, note: String? = null) =
        dao.insertGoalProgressEntry(GoalProgress(UUID.randomUUID().toString(), goalId, date, amount, note?.takeIf { it.isNotBlank() }))

    private fun Goal.spec() = GoalSpec(id, GoalMeasure.valueOf(measure), target, windowStart, windowEnd, GoalStatus.valueOf(status))

    /** Manual entries plus, for a focus-linked duration goal, the hours of the focus sessions tagged with it, one entry per day. */
    suspend fun entriesOf(goal: Goal): List<GoalEntry> {
        val manual = dao.getGoalProgress(goal.id).map { GoalEntry(it.date, it.amount) }
        if (!goal.focusLinked) return manual
        val focus = dao.getFocusSessionsForGoal(goal.id).groupBy { it.dateKey }.map { (d, s) -> GoalEntry(d, s.sumOf { it.durationMin ?: 0 } / 60.0) }
        return manual + focus
    }

    /** What is logged toward a goal inside its window: manual entries plus linked focus hours. */
    suspend fun progressOf(goal: Goal): Double = GoalReview.progress(goal.spec(), entriesOf(goal))

    suspend fun setCharacter(id: String) = db.withTransaction { dao.getJourneyState()?.let { dao.upsertJourneyState(it.copy(characterId = id)) } }

    /**
     * Decides every pending goal from the logged data, so opening on Monday or on Wednesday gives the same result.
     * A missed commitment sends the character to the start of its region. Weekly goals are never penalised. Returns the goals that changed.
     */
    suspend fun review(today: String): List<Goal> = db.withTransaction {
        val changed = mutableListOf<Goal>()
        var state = dao.getJourneyState()
        for (goal in dao.getAllGoals().filter { it.status == "PENDING" }.sortedBy { it.windowEnd }) {
            val status = GoalReview.status(goal.spec(), entriesOf(goal), today)
            if (status == GoalStatus.PENDING) continue
            val updated = goal.copy(status = status.name)
            dao.insertGoals(listOf(updated))
            changed += updated
            val s = state
            if (status == GoalStatus.MISSED && goal.kind == "COMMITMENT" && s != null && s.enabled && goal.regionIndex == s.regionIndex) {
                state = s.with(JourneyRules.commitmentMissed(s.position())).also { dao.upsertJourneyState(it) }
            }
        }
        changed
    }

    /** True when every commitment set for the region is met. With none set (Home Start) the gate is open. */
    suspend fun commitmentsMet(region: Int) = dao.getAllGoals().filter { it.kind == "COMMITMENT" && it.regionIndex == region }.all { it.status == "MET" }

    /** Crosses the gate when the character stands at it and every commitment is met, and stamps the passport. Returns false otherwise. */
    suspend fun crossGate(today: String, regionName: String): Boolean = db.withTransaction {
        val s = dao.getJourneyState()?.takeIf { it.enabled } ?: return@withTransaction false
        val next = JourneyRules.crossGate(s.position(), commitmentsMet(s.regionIndex)) ?: return@withTransaction false
        dao.upsertJourneyState(s.with(next))
        dao.insertPassportStamps(listOf(PassportStamp(UUID.randomUUID().toString(), "REGION", s.regionIndex, today, regionName)))
        true
    }

    /** Claims the reward of a met commitment once and records it in the travel log. */
    suspend fun claimReward(goalId: String, today: String): Boolean = db.withTransaction {
        val g = dao.getAllGoals().firstOrNull { it.id == goalId } ?: return@withTransaction false
        val reward = g.rewardText ?: return@withTransaction false
        if (g.status != "MET" || g.rewardClaimedAt != null) return@withTransaction false
        dao.insertGoals(listOf(g.copy(rewardClaimedAt = today)))
        dao.insertJourneyNotes(listOf(JourneyNote(UUID.randomUUID().toString(), today, "LOG", "Claimed: $reward")))
        true
    }
}
