package com.example.service

import com.example.data.DailyAnalytics
import com.example.data.SyncWellDao
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AnalyticsComputationService {

    suspend fun recalculateDayCache(dao: SyncWellDao, dateKey: String): Boolean {
        return try {
            val dailyRoutine = dao.getDailyRoutineDirectly(dateKey)
            val routinePct = dailyRoutine?.completionPct ?: 0.0

            // Query tracker sessions
            // The DAO returns isDeleted = 0. We'll filter the list for correct categories.
            var studyMins = 0
            var workMins = 0
            var timepassMins = 0
            var sleepMins = 0

            // We retrieve tracker sessions directly from Flow inside ViewModel, but for computation,
            // we will query a list. Let's make sure our Dao has a direct retrieve query of list if needed,
            // or we can collect from flow briefly, or we can add a synchronous method to Dao.
            // Let's add a list-returning query to SyncWellDao or query it. Wait! Let's check our SyncWellDao. It returns Flow<List<TrackerSession>>.
            // To collect a single list safely, we can do flow.first() or similar. Since we want standard list retrieve,
            // let's look at what SyncWellDao already has:
            // Flow<List<TrackerSession>> getTrackerSessions(dateKey).
            // Flow lists are fully reactive. Let's write a suspend function in DAO that returns list directly,
            // or we can check what DAO queries can be written. Yes! We can just add another Query to DAO or add a direct getter.
            // Wait, we have getTrackerSessions(dateKey) returning Flow. We can collect it or simply use a direct list query.
            // Let's check if we can add a list-getter, or we can just use a helper DAO method or use flow collection.
            // Let's add a Direct List query in DAO or make a robust retrieve. Let's check our SyncWellDao again.
            // Ah! We can easily use flow collection: `import kotlinx.coroutines.flow.first`.
            // But let's look at the tracker sessions we can gather. Let's query them.
            
            // Wait! To keep it simple, we can add queries to SyncWellDao that return Lists directly for computational services if needed.
            // Wait, we can implement it by just having a Direct List retrieve, or we can write a simple database list getter.
            // Since we created SyncWellDao in a previous step, let's see if we can edit it or if we can use flow.first().
            // `kotlinx.coroutines.flow.firstOrNull` or `first` is standard and extremely easy to use! Let's use `first()` on the flow returned.
            // Let's list analytical components.
            
            val trackerFlow = dao.getTrackerSessions(dateKey)
            // Let's collect the first emission from the flow
            val sessions: List<com.example.data.TrackerSession> = trackerFlow.first()
            sessions.forEach { s ->
                val dur = s.durationMin ?: 0
                when (s.activityType) {
                    "STUDY" -> studyMins += dur
                    "WORK" -> workMins += dur
                    "TIMEPASS" -> timepassMins += dur
                    "SLEEP" -> sleepMins += dur
                }
            }

            // Water logs
            val waterFlow = dao.getWaterLogs(dateKey)
            val waterEntries: List<com.example.data.WaterLog> = waterFlow.first()
            val totalWater = waterEntries.sumOf { it.amountMl }

            // Steps logs
            val stepsEntry = dao.getStepsLog(dateKey)
            val totalSteps = stepsEntry?.stepCount ?: 0

            // Weight logs
            val weightEntry = dao.getWeightLogByDate(dateKey)
            val weightKg = weightEntry?.weightKg

            // Vacation status
            val isVacation = dao.getVacationDayByDate(dateKey) != null

            // Streak check - compute active streaks using last 7 days or we can fetch/compute recursively.
            var streakValue = 0
            var gymStreakValue = 0
            var dietStreakValue = 0

            val yesterdayStr = getOffsetDateString(dateKey, -1)
            val yesterdayAnalyticsResult = dao.getDailyAnalyticsDirectly(yesterdayStr)
            
            val yesterdayStreak = yesterdayAnalyticsResult?.streakValue ?: 0
            val yesterdayGymStreak = yesterdayAnalyticsResult?.gymStreakValue ?: 0
            val yesterdayDietStreak = yesterdayAnalyticsResult?.dietStreakValue ?: 0

            // General Habit Streak
            if (routinePct >= 0.8) {
                streakValue = yesterdayStreak + 1
            } else if (isVacation) {
                streakValue = yesterdayStreak
            }

            // Gym Streak Logic
            val isGymCompleted = dao.getDailyRoutineItems(dailyRoutine?.id ?: "").any {
                it.category == "WORKOUT" && it.isDone
            }
            if (isGymCompleted) {
                gymStreakValue = yesterdayGymStreak + 1
            } else if (isVacation) {
                gymStreakValue = yesterdayGymStreak
            }

            // Diet Streak Logic (Protein Points)
            // Assuming dietProteinPoints is updated elsewhere and stored in DB, or we compute here.
            // Let's compute based on current items.
            val proteinItems = dao.getDailyRoutineItems(dailyRoutine?.id ?: "").filter { it.category == "DIET" }
            val dietProteinPoints = proteinItems.count { it.isDone }
            val isDietCompleted = proteinItems.isNotEmpty() && dietProteinPoints >= minOf(4, proteinItems.size)

            if (isDietCompleted) {
                dietStreakValue = yesterdayDietStreak + 1
            } else if (isVacation) {
                dietStreakValue = yesterdayDietStreak
            }

            // Workout Sessions & Duration
            val workoutSessions = dao.getWorkoutSessionsByDate(dateKey)
            var workoutMins = 0
            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            workoutSessions.forEach { ws ->
                if (ws.endedAt != null) {
                    try {
                        val sTime = isoFormat.parse(ws.startedAt)?.time ?: 0L
                        val eTime = isoFormat.parse(ws.endedAt)?.time ?: 0L
                        if (eTime > sTime) {
                            workoutMins += ((eTime - sTime) / (1000 * 60)).toInt()
                        }
                    } catch (_: Exception) {}
                }
            }

            val todayIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())

            val computedAnalytics = DailyAnalytics(
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
                dietProteinPoints = dietProteinPoints,
                isGymCompleted = isGymCompleted,
                isDietCompleted = isDietCompleted,
                isVacation = isVacation,
                updatedAt = todayIso
            )

            dao.insertDailyAnalytics(computedAnalytics)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun getOffsetDateString(dateStr: String, daysOffset: Int): String {
        return try {
            val dfs = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val date = dfs.parse(dateStr) ?: Date()
            val calendar = java.util.Calendar.getInstance().apply {
                time = date
                add(java.util.Calendar.DAY_OF_YEAR, daysOffset)
            }
            dfs.format(calendar.time)
        } catch (e: Exception) {
            dateStr
        }
    }
}
