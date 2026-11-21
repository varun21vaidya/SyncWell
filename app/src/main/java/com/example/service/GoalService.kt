package com.example.service

import com.example.data.GoalCompletion
import com.example.data.SyncWellDao
import com.example.data.Todo
import com.example.domain.WeekKey
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

/** Goal completion history and the boundary reset. Every completion leaves a row in goal_completions (one per goal per day). */
object GoalService {

    /** Ticks or unticks a goal and keeps today's history row in step. */
    suspend fun setCompleted(dao: SyncWellDao, todo: Todo, done: Boolean, todayKey: String, nowIso: String) {
        dao.insertTodo(todo.copy(isCompleted = done, updatedAt = nowIso))
        if (done) dao.insertGoalCompletion(GoalCompletion(UUID.randomUUID().toString(), todo.id, todo.title, todo.goalType, todayKey))
        else dao.deleteGoalCompletion(todo.id, todayKey)
    }

    /**
     * Unticks goals whose period is over, recording each completion in the history first.
     * Daily goals (and goals with a weekly target, which are ticked once a day) reset when the day changes;
     * other weekly goals reset when the Monday-to-Sunday week of their last change is over.
     */
    suspend fun resetAtBoundary(dao: SyncWellDao, todayKey: String, nowIso: String) {
        val thisWeek = WeekKey.of(todayKey)
        val completed = (dao.getActiveShortTermGoals().first() + dao.getActiveLongTermGoals().first()).filter { it.isCompleted }
        completed.forEach { g ->
            val doneOn = localDate(g.updatedAt)
            val due = if (g.goalType == "SHORT_TERM" || g.weeklyTarget != null) doneOn < todayKey else WeekKey.of(doneOn) != thisWeek
            if (!due) return@forEach
            dao.insertGoalCompletion(GoalCompletion(UUID.randomUUID().toString(), g.id, g.title, g.goalType, doneOn)) // ignored when already recorded
            dao.insertTodo(g.copy(isCompleted = false, updatedAt = nowIso))
        }
    }

    /** Local calendar date of a stored UTC timestamp, so it matches the day keys used everywhere else. */
    internal fun localDate(iso: String): String {
        val parsed = try {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.parse(iso)
        } catch (_: Exception) { null }
        return parsed?.let { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(it) } ?: iso.take(10)
    }
}
