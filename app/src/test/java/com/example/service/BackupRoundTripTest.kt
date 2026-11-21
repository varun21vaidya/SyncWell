package com.example.service

import androidx.test.core.app.ApplicationProvider
import androidx.room.Room
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
@Config(sdk = [34]) // the default (36) needs Java 21
class BackupRoundTripTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: SyncWellDao

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        dao = db.syncWellDao()
    }

    @After fun tearDown() = db.close()

    @Test fun backupThenRestoreBringsEverythingBack() = runBlocking {
        val now = "2026-11-07T10:00:00Z"
        dao.insertAppSettings(AppSettings(id = "s", firebaseUid = "me@x.com", targetWeightKg = 80.0, createdAt = now, updatedAt = now, wakeTime = "06:30", vacationSince = "2026-11-05", weeklyGymTarget = 4, restSeconds = 120))
        dao.insertTodos(listOf(Todo(id = "t1", title = "Buy milk", createdAt = now, updatedAt = now, weeklyTarget = 4)))
        dao.insertGoalCompletion(GoalCompletion("gc1", "t1", "Buy milk", "SHORT_TERM", "2026-11-06"))
        dao.insertWeightLogs(listOf(WeightLog("w1", "2026-11-07", 81.5, false, now)))
        dao.insertRoutineTemplate(RoutineTemplate(id = "r1", name = "Mine", repeatType = "ONCE", onceDate = "2026-11-09"))
        dao.insertRoutineTemplateItems(listOf(RoutineTemplateItem("i1", "r1", "Stretch", "HEALTH", 1, "07:00")))
        dao.insertExerciseLibrary(listOf(ExerciseLibraryItem("custom_1", "My curl", "BICEPS", 3, 8, 12, 60, null, null)))
        dao.insertWorkoutSession(WorkoutSession("ws1", "2026-11-07", "PUSH", "GYM", now, now, activeSeconds = 1800))
        dao.insertDailyRoutine(DailyRoutine("d1", "2026-11-07", null, 0.5))
        dao.insertDailyRoutineItems(listOf(
            DailyRoutineItem("m1", "d1", "Lunch", "DIET", 1, "13:00", isDone = true, proteinAnswer = "NO"),
            DailyRoutineItem("m2", "d1", "Dinner", "DIET", 2, "20:00", isDone = false)
        ))

        val (json, summary) = CloudBackupService.buildBackup(dao)
        assertEquals(1, summary.todosCount)

        db.clearAllTables()
        CloudBackupService.restoreFromJson(dao, json)

        assertEquals("Buy milk", dao.getAllTodos().single().title)
        assertEquals(4, dao.getAllTodos().single().weeklyTarget)
        assertEquals("2026-11-06", dao.getAllGoalCompletions().single().completedOn)
        assertEquals(81.5, dao.getAllWeightLogs().single().weightKg, 0.0)
        val template = dao.getRoutineTemplates().single()
        assertEquals("2026-11-09", template.onceDate)
        assertEquals("Stretch", dao.getAllRoutineTemplateItems().single().title)
        assertTrue(dao.getExerciseLibrary().any { it.id == "custom_1" })
        val settings = dao.getAppSettingsDirectly()!!
        assertEquals("06:30", settings.wakeTime)
        assertEquals(80.0, settings.targetWeightKg, 0.0)
        assertTrue(settings.onboardingAnswered)
        assertEquals("2026-11-05", settings.vacationSince)
        assertEquals(4, settings.weeklyGymTarget)
        assertEquals(120, settings.restSeconds)
        assertEquals(1800, dao.getAllWorkoutSessions().single().activeSeconds)
        val meals = dao.getAllDailyRoutineItems().associateBy { it.id }
        assertEquals("NO", meals.getValue("m1").proteinAnswer)
        assertEquals(null, meals.getValue("m2").proteinAnswer)
    }

    @Test fun olderBackupWithoutAnswerKeepsTickedMealsAsProteinMet() = runBlocking {
        val json = """{"dailyRoutines":[{"id":"d1","date":"2026-11-07","completionPct":0.5,"isVacation":false}],
            "dailyRoutineItems":[
              {"id":"a","dailyRoutineId":"d1","title":"Lunch","category":"DIET","orderIndex":1,"isDone":true},
              {"id":"b","dailyRoutineId":"d1","title":"Dinner","category":"DIET","orderIndex":2,"isDone":false}]}"""
        CloudBackupService.restoreFromJson(dao, json)
        val items = dao.getAllDailyRoutineItems().associateBy { it.id }
        assertEquals("YES", items.getValue("a").proteinAnswer)
        assertEquals(null, items.getValue("b").proteinAnswer)
    }

    @Test fun olderBackupWithoutNewSettingsFallsBackToDefaults() = runBlocking {
        val json = """{"appSettings":{"id":"s","createdAt":"x","updatedAt":"x"},
            "workoutSessions":[{"id":"w","date":"2026-11-07","dayType":"PUSH","startedAt":"x"}]}"""
        CloudBackupService.restoreFromJson(dao, json)
        val s = dao.getAppSettingsDirectly()!!
        assertEquals(null, s.vacationSince); assertEquals(3, s.weeklyGymTarget); assertEquals(false, s.restTimerEnabled); assertEquals(90, s.restSeconds)
        assertEquals(0, dao.getAllWorkoutSessions().single().activeSeconds)
    }

    @Test fun restoreWithSameNameCustomExerciseReusesThePhonesExercise() = runBlocking {
        val now = "2026-11-07T10:00:00Z"
        dao.insertExerciseLibrary(listOf(ExerciseLibraryItem("phone_1", "My curl", "BICEPS", 3, 8, 12, 60, null, null)))
        dao.insertWorkoutSession(WorkoutSession("local", "2026-11-06", "PULL", "GYM", now, now))
        dao.insertWorkoutSessionExercises(listOf(WorkoutSessionExercise("lse", "local", "phone_1", 1, 3, 10, null)))
        val json = """{"customExercises":[{"id":"bk_1","name":"My curl","category":"BICEPS","defaultSets":3,"defaultRepsMin":8,"defaultRepsMax":12,"defaultRestSec":60}],
            "workoutSessions":[{"id":"bs","date":"2026-11-01","dayType":"PULL","startedAt":"$now","endedAt":"$now"}],
            "workoutSessionExercises":[{"id":"bse","sessionId":"bs","exerciseId":"bk_1","orderIndex":1,"targetSets":3,"targetRepsMax":10}]}"""
        CloudBackupService.restoreFromJson(dao, json)
        assertEquals(1, dao.getExerciseLibrary().count { it.name == "My curl" })
        assertEquals("phone_1", dao.getAllWorkoutSessionExercises().single { it.id == "bse" }.exerciseId)
        assertEquals("phone_1", dao.getAllWorkoutSessionExercises().single { it.id == "lse" }.exerciseId)
    }

    @Test fun olderBackupWithoutGoalHistoryRestoresTodosWithoutATarget() = runBlocking {
        val json = """{"todos":[{"id":"t","title":"Old goal","createdAt":"2026-11-01T08:00:00Z"}]}"""
        CloudBackupService.restoreFromJson(dao, json)
        assertEquals(null, dao.getAllTodos().single().weeklyTarget)
        assertTrue(dao.getAllGoalCompletions().isEmpty())
    }
}
