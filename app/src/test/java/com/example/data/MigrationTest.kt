package com.example.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MigrationTest {
    @get:Rule val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    // the helper wants the full path under Robolectric
    private val dbName get() = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>().getDatabasePath("mig").absolutePath

    @Test fun migrate8To9KeepsExistingRows() {
        helper.createDatabase(dbName, 8).apply {
            execSQL("INSERT INTO app_settings (id, firebaseUid, defaultWaterMl, targetWeightKg, themeMode, backupEnabled, vacationPauseEnabled, workoutLocationMode, createdAt, updatedAt, wakeTime, sleepTime, workoutDays, onboardingAnswered) VALUES ('s', NULL, 2500, 80.0, 'dark', 0, 0, 'GYM', 'x', 'x', '06:30', '23:00', '[1,2,3]', 1)")
            execSQL("INSERT INTO daily_routines (id, date, templateId, completionPct, isVacation) VALUES ('d1', '2026-11-07', NULL, 0.5, 0)")
            execSQL("INSERT INTO daily_routine_items (id, dailyRoutineId, title, category, orderIndex, scheduledTime, isDone) VALUES ('a', 'd1', 'Lunch', 'DIET', 1, NULL, 1)")
            execSQL("INSERT INTO daily_routine_items (id, dailyRoutineId, title, category, orderIndex, scheduledTime, isDone) VALUES ('b', 'd1', 'Dinner', 'DIET', 2, NULL, 0)")
            execSQL("INSERT INTO workout_sessions (id, date, dayType, environment, startedAt, endedAt, isDeleted) VALUES ('w1', '2026-11-07', 'PUSH', 'GYM', 'x', 'y', 0)")
            execSQL("INSERT INTO todos (id, title, priority, goalType, isCompleted, isDeleted, createdAt, updatedAt) VALUES ('t1', 'Goal', 'HIGH', 'LONG_TERM', 1, 0, 'x', 'x')")
            close()
        }

        val db = helper.runMigrationsAndValidate(dbName, 9, true, AppDatabase.MIGRATION_8_9)

        db.query("SELECT workoutDays, weeklyGymTarget, restTimerEnabled, restSeconds, vacationSince FROM app_settings").use {
            assertTrue(it.moveToFirst())
            assertEquals("[1,2,3]", it.getString(0))
            assertEquals(3, it.getInt(1)); assertEquals(0, it.getInt(2)); assertEquals(90, it.getInt(3)); assertTrue(it.isNull(4))
        }
        db.query("SELECT completionPct FROM daily_routines WHERE id = 'd1'").use { it.moveToFirst(); assertEquals(0.5, it.getDouble(0), 0.0) }
        // a ticked meal keeps counting as protein met; an unticked one stays unanswered
        db.query("SELECT proteinAnswer FROM daily_routine_items WHERE id = 'a'").use { it.moveToFirst(); assertEquals("YES", it.getString(0)) }
        db.query("SELECT proteinAnswer FROM daily_routine_items WHERE id = 'b'").use { it.moveToFirst(); assertTrue(it.isNull(0)) }
        db.query("SELECT activeSeconds, isDeleted FROM workout_sessions WHERE id = 'w1'").use { it.moveToFirst(); assertEquals(0, it.getInt(0)); assertEquals(0, it.getInt(1)) }
        db.query("SELECT isCompleted FROM todos WHERE id = 't1'").use { it.moveToFirst(); assertEquals(1, it.getInt(0)) }
    }

    @Test fun migrate9To10KeepsGoalsAndSeedsHistoryForTickedOnes() {
        helper.createDatabase(dbName, 9).apply {
            execSQL("INSERT INTO todos (id, title, priority, goalType, isCompleted, isDeleted, createdAt, updatedAt) VALUES ('t1', 'Read', 'HIGH', 'LONG_TERM', 1, 0, '2026-11-01T08:00:00Z', '2026-11-18T08:00:00Z')")
            execSQL("INSERT INTO todos (id, title, priority, goalType, isCompleted, isDeleted, createdAt, updatedAt) VALUES ('t2', 'Run', 'LOW', 'SHORT_TERM', 0, 0, '2026-11-01T08:00:00Z', '2026-11-18T08:00:00Z')")
            close()
        }
        val db = helper.runMigrationsAndValidate(dbName, 10, true, AppDatabase.MIGRATION_9_10)
        db.query("SELECT title, isCompleted, weeklyTarget FROM todos ORDER BY id").use {
            assertTrue(it.moveToFirst()); assertEquals("Read", it.getString(0)); assertEquals(1, it.getInt(1)); assertTrue(it.isNull(2))
            assertTrue(it.moveToNext()); assertEquals("Run", it.getString(0))
        }
        db.query("SELECT todoId, completedOn FROM goal_completions").use {
            assertEquals(1, it.count)
            it.moveToFirst(); assertEquals("t1", it.getString(0)); assertEquals("2026-11-18", it.getString(1))
        }
    }

    @Test fun migrate10To11LinksExistingItemsAndKeepsVacationDays() {
        helper.createDatabase(dbName, 10).apply {
            execSQL("INSERT INTO routine_templates (id, name, dayType, isActive, repeatType, daysOfWeek, onceDate, isStarter) VALUES ('tpl', 'Daily', '', 1, 'DAILY', '[1]', NULL, 0)")
            execSQL("INSERT INTO routine_template_items (id, templateId, title, category, orderIndex, scheduledTime, isRequired) VALUES ('ti1', 'tpl', 'Sunscreen', 'SKINCARE', 1, '08:00', 1)")
            execSQL("INSERT INTO daily_routines (id, date, templateId, completionPct, isVacation) VALUES ('d1', '2026-11-07', 'tpl', 1.0, 0)")
            execSQL("INSERT INTO daily_routine_items (id, dailyRoutineId, title, category, orderIndex, scheduledTime, isDone, proteinAnswer) VALUES ('a', 'd1', 'Sunscreen', 'SKINCARE', 1, '08:00', 1, NULL)")
            execSQL("INSERT INTO daily_routine_items (id, dailyRoutineId, title, category, orderIndex, scheduledTime, isDone, proteinAnswer) VALUES ('b', 'd1', 'Custom one-off', 'OTHER', 2, NULL, 0, NULL)")
            execSQL("INSERT INTO vacation_days (id, date, isDeleted, createdAt) VALUES ('v1', '2026-11-05', 0, 'x')")
            close()
        }
        val db = helper.runMigrationsAndValidate(dbName, 11, true, AppDatabase.MIGRATION_10_11)
        db.query("SELECT id, templateItemId FROM daily_routine_items ORDER BY id").use {
            assertTrue(it.moveToFirst()); assertEquals("ti1", it.getString(1)) // linked by title and category
            assertTrue(it.moveToNext()); assertTrue(it.isNull(1)) // no template item to link to
        }
        db.query("SELECT kind FROM vacation_days").use { it.moveToFirst(); assertEquals("VACATION", it.getString(0)) }
    }

    @Test fun migrate11To12AddsCardioTableAndKeepsSessions() {
        helper.createDatabase(dbName, 11).apply {
            execSQL("INSERT INTO workout_sessions (id, date, dayType, environment, startedAt, endedAt, isDeleted, activeSeconds) VALUES ('w1', '2026-11-07', 'PUSH', 'GYM', 'x', 'y', 0, 1800)")
            close()
        }
        val db = helper.runMigrationsAndValidate(dbName, 12, true, AppDatabase.MIGRATION_11_12)
        db.execSQL("INSERT INTO cardio_entries (id, sessionId, type, minutes, distanceKm, inclinePct, effort) VALUES ('c1', 'w1', 'TREADMILL', 15, NULL, 6.0, 'HARD')")
        db.query("SELECT COUNT(*) FROM cardio_entries").use { it.moveToFirst(); assertEquals(1, it.getInt(0)) }
        db.query("SELECT activeSeconds FROM workout_sessions WHERE id = 'w1'").use { it.moveToFirst(); assertEquals(1800, it.getInt(0)) }
    }

    @Test fun migrate12To13AddsJourneyTablesAndGoalIdAndKeepsFocusSessions() {
        helper.createDatabase(dbName, 12).apply {
            execSQL("INSERT INTO tracker_sessions (id, activityType, source, startTime, endTime, durationMin, dateKey, isDeleted) VALUES ('t1', 'STUDY', 'TIMER', 'x', 'y', 45, '2026-11-07', 0)")
            close()
        }
        val db = helper.runMigrationsAndValidate(dbName, 13, true, AppDatabase.MIGRATION_12_13)
        db.query("SELECT durationMin, goalId FROM tracker_sessions WHERE id = 't1'").use { it.moveToFirst(); assertEquals(45, it.getInt(0)); assertTrue(it.isNull(1)) }
        db.execSQL("INSERT INTO goals (id, title, kind, mandatory, measure, target, focusLinked, windowStart, windowEnd, status) VALUES ('g1', 'DSA', 'COMMITMENT', 1, 'COUNT', 10.0, 0, '2026-11-23', '2026-11-29', 'PENDING')")
        db.execSQL("INSERT INTO goal_progress (id, goalId, date, amount) VALUES ('p1', 'g1', '2026-11-24', 2.0)")
        db.execSQL("INSERT INTO journey_state (id, enabled, characterId, regionIndex, stepInRegion, furthestRegion) VALUES (1, 1, 'max', 0, 0, 0)")
        db.execSQL("INSERT INTO passport (id, kind, regionIndex, earnedAt, label) VALUES ('s1', 'REGION', 0, 'x', 'Home Start')")
        db.execSQL("INSERT INTO journey_notes (id, date, kind, text) VALUES ('n1', '2026-11-24', 'LOG', 'hi')")
        db.query("SELECT COUNT(*) FROM goal_progress").use { it.moveToFirst(); assertEquals(1, it.getInt(0)) }
    }
}
