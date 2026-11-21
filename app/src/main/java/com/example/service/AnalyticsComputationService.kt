package com.example.service

import com.example.data.DailyAnalytics
import com.example.data.SyncWellDao
import com.example.domain.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AnalyticsComputationService {

    /**
     * Recomputes one day: writes the routine's percent from its items (the only place that math lives),
     * then the day's analytics row with streaks derived from the whole history.
     */
    suspend fun recalculateDayCache(dao: SyncWellDao, dateKey: String): Result<DailyAnalytics> {
        return try {
            val dailyRoutine = dao.getDailyRoutineDirectly(dateKey)
            val items = dailyRoutine?.let { dao.getDailyRoutineItems(it.id) } ?: emptyList()
            val routinePct = DayCompletion.of(items.count { it.isDone }, items.size).percent
            if (dailyRoutine != null && dailyRoutine.completionPct != routinePct) {
                dao.updateDailyRoutine(dailyRoutine.copy(completionPct = routinePct))
            }

            var studyMins = 0
            var workMins = 0
            var timepassMins = 0
            var sleepMins = 0
            dao.getTrackerSessions(dateKey).first().forEach { s ->
                val dur = s.durationMin ?: 0
                when (s.activityType) {
                    "STUDY" -> studyMins += dur
                    "WORK" -> workMins += dur
                    "TIMEPASS" -> timepassMins += dur
                    "SLEEP" -> sleepMins += dur
                }
            }

            val totalWater = dao.getWaterLogs(dateKey).first().sumOf { it.amountMl }
            val totalSteps = dao.getStepsLog(dateKey)?.stepCount ?: 0
            val weightKg = dao.getWeightLogByDate(dateKey)?.weightKg
            val isVacation = dao.getVacationDayByDate(dateKey) != null

            // The caller-visible history is dense: a past day with no row is a 0% day unless it was a vacation day
            val vacationDates = dao.getAllVacationDays().map { it.date }.toSet()
            val history = dao.getAnalyticsBetween("0000-01-01", WeekKey.addDays(dateKey, -1)).first()
            val rowDates = history.map { it.dateKey }.toSet()
            val vacationOnly = vacationDates.filter { it < dateKey && it !in rowDates }

            val routineDays = history.map { StreakDay(it.dateKey, DayCompletion.qualifies(it.routineCompletionPct), it.isVacation || it.dateKey in vacationDates) } +
                vacationOnly.map { StreakDay(it, false, true) } +
                StreakDay(dateKey, DayCompletion.qualifies(routinePct), isVacation)
            val streakValue = StreakRule.compute(routineDays, dateKey).current

            // Gym: finished sessions only (a discarded session is stored as deleted), counted in weeks
            val finishedDates = dao.getFinishedSessionDates()
            val isGymCompleted = dateKey in finishedDates
            val weeklyTarget = dao.getAppSettingsDirectly()?.weeklyGymTarget ?: 3
            val gymStreakValue = WeeklyGymStreak.compute(finishedDates, weeklyTarget, dateKey).currentWeeks

            // Diet: a meal counts only when ticked with protein answered Yes
            val diet = DietRule.evaluate(items.filter { it.category == "DIET" }.map { DietMeal(it.isDone, it.proteinAnswer) })
            val dietDays = history.map { StreakDay(it.dateKey, it.isDietCompleted, it.isVacation || it.dateKey in vacationDates) } +
                vacationOnly.map { StreakDay(it, false, true) } +
                StreakDay(dateKey, diet.qualifies, isVacation)
            val dietStreakValue = StreakRule.compute(dietDays, dateKey).current

            // Workout minutes: active time when recorded, otherwise start to end
            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            var workoutMins = 0
            dao.getWorkoutSessionsByDate(dateKey).forEach { ws ->
                if (ws.endedAt == null) return@forEach
                if (ws.activeSeconds > 0) {
                    workoutMins += ws.activeSeconds / 60
                } else try {
                    val span = (isoFormat.parse(ws.endedAt)?.time ?: 0L) - (isoFormat.parse(ws.startedAt)?.time ?: 0L)
                    if (span > 0) workoutMins += (span / 60_000).toInt()
                } catch (_: Exception) {}
            }

            val computed = DailyAnalytics(
                dateKey = dateKey,
                routineCompletionPct = routinePct,
                totalStudyMinutes = studyMins,
                workMin = workMins,
                timepassMin = timepassMins,
                totalWorkoutMinutes = workoutMins,
                totalWaterMl = totalWater,
                totalSteps = totalSteps,
                sleepMin = sleepMins,
                weightKg = weightKg,
                streakValue = streakValue,
                gymStreakValue = gymStreakValue,
                dietStreakValue = dietStreakValue,
                dietProteinPoints = diet.points.coerceAtMost(4),
                isGymCompleted = isGymCompleted,
                isDietCompleted = diet.qualifies,
                isVacation = isVacation,
                updatedAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
            )
            dao.insertDailyAnalytics(computed)
            Result.success(computed)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Recomputes each day from [fromKey] to [toKey], oldest first, so later days see the earlier rows. */
    suspend fun recalculateRange(dao: SyncWellDao, fromKey: String, toKey: String) {
        WeekKey.range(fromKey, toKey).forEach { recalculateDayCache(dao, it) }
    }
}
