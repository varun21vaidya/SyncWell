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
}
