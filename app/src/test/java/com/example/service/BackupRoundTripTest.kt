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

    @Test fun templateLinkAndFreezeKindRoundTripAndOlderBackupsDefault() = runBlocking {
        val now = "2026-11-07T10:00:00Z"
        dao.insertDailyRoutine(DailyRoutine("d1", "2026-11-07", null, 0.0))
        dao.insertDailyRoutineItems(listOf(DailyRoutineItem("i1", "d1", "Sunscreen", "SKINCARE", 1, "08:00", templateItemId = "ti1")))
        dao.insertVacationDays(listOf(VacationDay("f1", "2026-11-09", false, now, "FREEZE")))
        val (json, _) = CloudBackupService.buildBackup(dao)
        db.clearAllTables()
        CloudBackupService.restoreFromJson(dao, json)
        assertEquals("ti1", dao.getAllDailyRoutineItems().single().templateItemId)
        assertEquals("FREEZE", dao.getAllVacationDays().single().kind)

        db.clearAllTables()
        CloudBackupService.restoreFromJson(dao, """{"vacationDays":[{"id":"v","date":"2026-11-02","createdAt":"x"}],
            "dailyRoutines":[{"id":"d","date":"2026-11-02","completionPct":0.0}],
            "dailyRoutineItems":[{"id":"i","dailyRoutineId":"d","title":"Old","category":"OTHER","orderIndex":1}]}""")
        assertEquals("VACATION", dao.getAllVacationDays().single().kind)
        assertEquals(null, dao.getAllDailyRoutineItems().single().templateItemId)
    }

    @Test fun cardioEntriesRoundTripAndOlderBackupsHaveNone() = runBlocking {
        dao.insertCardioEntries(listOf(
            CardioEntry("c1", "w1", "TREADMILL", 20, null, 5.0, "HARD"),
            CardioEntry("c2", "w1", "CYCLING", 30, 12.5, null, "EASY")
        ))
        val (json, _) = CloudBackupService.buildBackup(dao)
        db.clearAllTables()
        CloudBackupService.restoreFromJson(dao, json)
        val back = dao.getAllCardioEntries().associateBy { it.id }
        assertEquals(5.0, back.getValue("c1").inclinePct!!, 0.0)
        assertEquals(null, back.getValue("c1").distanceKm)
        assertEquals(12.5, back.getValue("c2").distanceKm!!, 0.0)

        db.clearAllTables()
        CloudBackupService.restoreFromJson(dao, """{"todos":[]}""")
        assertTrue(dao.getAllCardioEntries().isEmpty())
    }

    @Test fun journeyBackupBringsBackStateGoalsProgressAndFocusLink() = runBlocking {
        dao.upsertJourneyState(JourneyState(enabled = true, characterId = "ivy", regionIndex = 2, stepInRegion = 9, furthestRegion = 2,
            freezeUsedWeekStart = "2026-11-23", lastProcessedDate = "2026-11-25", ambitionText = "Land a backend role"))
        dao.insertGoals(listOf(Goal("g1", "DSA", "COMMITMENT", true, "COUNT", 10.0, regionIndex = 2, windowStart = "2026-11-23", windowEnd = "2026-11-29", rewardText = "Burger", planText = "After lunch", planTime = "13:30")))
        dao.insertGoalProgress(listOf(GoalProgress("p1", "g1", "2026-11-24", 3.0, "two easy ones")))
        dao.insertPassportStamps(listOf(PassportStamp("s1", "REGION", 0, "2026-11-10", "Home Start")))
        dao.insertJourneyNotes(listOf(JourneyNote("n1", "2026-11-24", "REFLECTION", "Good week")))
        dao.insertTrackerSession(TrackerSession("t1", "STUDY", "TIMER", "x", "y", 45, "2026-11-24", goalId = "g1"))

        val (json, _) = CloudBackupService.buildBackup(dao)
        db.clearAllTables()
        CloudBackupService.restoreFromJson(dao, json)

        val s = dao.getJourneyState()!!
        assertEquals("2026-11-25", s.lastProcessedDate) // without it old days would be replayed
        assertEquals(9, s.stepInRegion); assertEquals("ivy", s.characterId); assertEquals("2026-11-23", s.freezeUsedWeekStart)
        assertEquals("Land a backend role", s.ambitionText); assertEquals(null, s.targetDate)
        val g = dao.getAllGoals().single()
        assertEquals("Burger", g.rewardText); assertEquals(2, g.regionIndex); assertEquals("13:30", g.planTime); assertEquals(null, g.ifThenText)
        assertEquals(3.0, dao.getAllGoalProgress().single().amount, 0.0)
        assertEquals("Home Start", dao.getAllPassportStamps().single().label)
        assertEquals("Good week", dao.getAllJourneyNotes().single().text)
        assertEquals("g1", dao.getAllTrackerSessions().single().goalId)
    }

    @Test fun olderBackupWithoutJourneyKeysLeavesNoJourneyRows() = runBlocking {
        CloudBackupService.restoreFromJson(dao, """{"trackerSessions":[{"id":"t","activityType":"STUDY","startTime":"x","dateKey":"2026-11-07"}]}""")
        assertEquals(null, dao.getJourneyState())
        assertTrue(dao.getAllGoals().isEmpty())
        assertEquals(null, dao.getAllTrackerSessions().single().goalId)
    }
}
