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
import com.example.domain.TravelLog
import com.example.domain.WeekKey
import java.util.UUID

/**
 * Connects the pure Journey rules to the database: turning the journey on or off, catching up the days that have ended,
 * reviewing goals, crossing a gate and claiming a reward. Each write is one transaction.
 */
private const val MAX_REFLECTION = 140

class JourneyUseCase(private val db: AppDatabase, private val dao: SyncWellDao, private val regionName: (Int) -> String = { "Region ${it + 1}" }) {

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
        // fixed ids: replaying the same day can never write its line twice
        dao.insertJourneyNotes(events.mapIndexedNotNull { i, e -> TravelLog.line(e, regionName)?.let { JourneyNote("auto-${e.date}-$i", e.date, "LOG", it) } })
        events
    }

    suspend fun saveGoal(goal: Goal) = dao.insertGoals(listOf(goal))

    /** Adds one dated progress entry (manual; focus time is added automatically for linked goals). */
    suspend fun addProgress(goalId: String, date: String, amount: Double, note: String? = null) {
        val clean = note?.trim()?.takeIf { it.isNotEmpty() }
        val id = UUID.randomUUID().toString()
        dao.insertGoalProgressEntry(GoalProgress(id, goalId, date, amount, clean))
        if (clean != null) {
            val title = dao.getAllGoals().firstOrNull { it.id == goalId }?.title ?: return
            dao.insertJourneyNotes(listOf(JourneyNote("progress-$id", date, "LOG", TravelLog.progress(title, clean))))
        }
    }

    /** One optional line at a region's camp, kept in the travel log. One per region: writing it again replaces it. Returns false for an empty line. */
    suspend fun saveReflection(region: Int, text: String, today: String): Boolean {
        val clean = text.trim().take(MAX_REFLECTION).trimEnd().ifEmpty { return false }
        dao.insertJourneyNotes(listOf(JourneyNote("camp-$region", today, "REFLECTION", clean)))
        return true
    }

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

    /** Saves the optional ambition and identity lines. A new or changed ambition starts its own clock; clearing it clears the clock. */
    suspend fun saveAmbition(ambition: String?, identity: String?, today: String) = db.withTransaction {
        val s = dao.getJourneyState() ?: return@withTransaction
        val a = com.example.domain.Ambition.clean(ambition, com.example.domain.Ambition.MAX_AMBITION)
        val start = if (a == null) null else if (a == s.ambitionText) s.ambitionStart else today
        dao.upsertJourneyState(s.copy(ambitionText = a, identityText = com.example.domain.Ambition.clean(identity, com.example.domain.Ambition.MAX_IDENTITY), ambitionStart = start))
    }

    suspend fun setCharacter(id: String) = db.withTransaction { dao.getJourneyState()?.let { dao.upsertJourneyState(it.copy(characterId = id)) } }

    /**
     * Decides every pending goal from the logged data, so opening on Monday or on Wednesday gives the same result.
     * A missed commitment sends the character to the start of its region. Weekly goals are never penalised. Returns the goals that changed.
     */
    suspend fun review(today: String): List<Goal> = db.withTransaction {
        dao.getAllGoals().filter { it.status == "PENDING" }.sortedBy { it.windowEnd }.mapNotNull { goal ->
            val status = GoalReview.status(goal.spec(), entriesOf(goal), today)
            if (status == GoalStatus.PENDING) null else settle(goal, status, today)
        }
    }

    /** Records one decided goal: saves its status, stamps the passport for a met commitment, sends the character back for a missed one. */
    private suspend fun settle(goal: Goal, status: GoalStatus, today: String): Goal {
        val updated = goal.copy(status = status.name)
        dao.insertGoals(listOf(updated))
        if (goal.kind == "COMMITMENT") {
            val met = status == GoalStatus.MET
            if (met) dao.insertPassportStamps(listOf(PassportStamp("commit-${goal.id}", "COMMITMENT", goal.regionIndex ?: 0, today, goal.title))) else sendBackToRegionStart(goal)
            dao.insertJourneyNotes(listOf(JourneyNote("goal-${goal.id}", today, "LOG", if (met) TravelLog.metCommitment(goal.title) else TravelLog.missedCommitment(goal.title))))
        }
        return updated
    }

    /** A missed commitment sends the character to the start of its region, but only while it is still in that region. */
    private suspend fun sendBackToRegionStart(goal: Goal) {
        val s = dao.getJourneyState()?.takeIf { it.enabled && goal.regionIndex == it.regionIndex } ?: return
        dao.upsertJourneyState(s.with(JourneyRules.commitmentMissed(s.position())))
    }

    /** True when every commitment set for the region is met. With none set (Home Start) the gate is open. */
    suspend fun commitmentsMet(region: Int) = dao.getAllGoals().filter { it.kind == "COMMITMENT" && it.regionIndex == region }.all { it.status == "MET" }

    /** Crosses the gate when the character stands at it and every commitment is met, and stamps the passport. Returns false otherwise. */
    suspend fun crossGate(today: String, regionName: String): Boolean = db.withTransaction {
        val s = dao.getJourneyState()?.takeIf { it.enabled } ?: return@withTransaction false
        val next = JourneyRules.crossGate(s.position(), commitmentsMet(s.regionIndex)) ?: return@withTransaction false
        dao.upsertJourneyState(s.with(next))
        dao.insertJourneyNotes(listOf(JourneyNote("cross-${s.regionIndex}", today, "LOG", TravelLog.crossed(regionName))))
        dao.insertPassportStamps(listOf(PassportStamp("region-${s.regionIndex}", "REGION", s.regionIndex, today, regionName))) // a fixed id, so crossing twice cannot stamp twice
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
