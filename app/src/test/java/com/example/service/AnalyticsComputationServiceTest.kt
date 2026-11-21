package com.example.service

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AnalyticsComputationServiceTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: SyncWellDao

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java).allowMainThreadQueries().build()
        dao = db.syncWellDao()
    }

    @After fun tearDown() = db.close()

    private suspend fun day(date: String, done: Int, total: Int) {
        dao.insertDailyRoutine(DailyRoutine("r$date", date, null, 0.0))
        dao.insertDailyRoutineItems((1..total).map { DailyRoutineItem("i$date$it", "r$date", "Item $it", "HEALTH", it, null, isDone = it <= done) })
    }

    @Test fun percentIsWrittenFromItemsNotTrustedFromTheRow() = runBlocking {
        day("2026-11-10", done = 4, total = 5)
        val result = AnalyticsComputationService.recalculateDayCache(dao, "2026-11-10").getOrThrow()
        assertEquals(0.8, result.routineCompletionPct, 1e-9)
        assertEquals(0.8, dao.getDailyRoutineDirectly("2026-11-10")!!.completionPct, 1e-9)
        assertEquals(1, result.streakValue)
    }

    @Test fun streakShowsYesterdaysRunWhileTodayIsBelowEighty() = runBlocking {
        listOf("2026-11-08", "2026-11-09").forEach { day(it, 5, 5); AnalyticsComputationService.recalculateDayCache(dao, it) }
        day("2026-11-10", 1, 5)
        assertEquals(2, AnalyticsComputationService.recalculateDayCache(dao, "2026-11-10").getOrThrow().streakValue)
    }

    @Test fun vacationDaysWithoutRowsKeepTheStreak() = runBlocking {
        day("2026-11-06", 5, 5); AnalyticsComputationService.recalculateDayCache(dao, "2026-11-06")
        // 7th and 8th: app never opened, only vacation rows exist
        dao.insertVacationDays(listOf("2026-11-07", "2026-11-08").map { VacationDay("v$it", it, false, "x") })
        day("2026-11-09", 5, 5)
        assertEquals(2, AnalyticsComputationService.recalculateDayCache(dao, "2026-11-09").getOrThrow().streakValue)
    }

    @Test fun missedDayWithoutVacationBreaksTheStreak() = runBlocking {
        day("2026-11-06", 5, 5); AnalyticsComputationService.recalculateDayCache(dao, "2026-11-06")
        day("2026-11-09", 5, 5)
        assertEquals(1, AnalyticsComputationService.recalculateDayCache(dao, "2026-11-09").getOrThrow().streakValue)
    }

    @Test fun discardedSessionDoesNotCountAsGym() = runBlocking {
        dao.insertWorkoutSession(WorkoutSession("w", "2026-11-10", "PUSH", "GYM", "2026-11-10T10:00:00Z", "2026-11-10T11:00:00Z"))
        assertTrue(AnalyticsComputationService.recalculateDayCache(dao, "2026-11-10").getOrThrow().isGymCompleted)
        dao.softDeleteWorkoutSession("w")
        assertEquals(false, AnalyticsComputationService.recalculateDayCache(dao, "2026-11-10").getOrThrow().isGymCompleted)
    }

    @Test fun gymStreakUsesTheWeeklyTargetNotTheSetupDays() = runBlocking {
        // setup days Mon-Fri would make the target 5; the setting says 2
        dao.insertAppSettings(AppSettings(id = "s", firebaseUid = null, createdAt = "x", updatedAt = "x", workoutDays = "[1,2,3,4,5]", weeklyGymTarget = 2))
        listOf("2026-11-09", "2026-11-10").forEach { dao.insertWorkoutSession(WorkoutSession("w$it", it, "PUSH", "GYM", "${it}T10:00:00Z", "${it}T11:00:00Z")) }
        assertEquals(1, AnalyticsComputationService.recalculateDayCache(dao, "2026-11-17").getOrThrow().gymStreakValue)
    }

    @Test fun aFrozenDayPausesTheStreakInsteadOfBreakingIt() = runBlocking {
        day("2026-11-06", 5, 5); AnalyticsComputationService.recalculateDayCache(dao, "2026-11-06")
        dao.insertVacationDay(VacationDay("f", "2026-11-07", false, "x", "FREEZE")) // frozen, nothing ticked
        day("2026-11-08", 5, 5)
        assertEquals(2, AnalyticsComputationService.recalculateDayCache(dao, "2026-11-08").getOrThrow().streakValue)
        dao.insertVacationDay(VacationDay("f", "2026-11-07", true, "x", "FREEZE")) // undone: the missed day now breaks it
        assertEquals(1, AnalyticsComputationService.recalculateDayCache(dao, "2026-11-08").getOrThrow().streakValue)
    }
}
