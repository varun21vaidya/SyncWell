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
        dao.insertAppSettings(AppSettings(id = "s", firebaseUid = "me@x.com", targetWeightKg = 80.0, createdAt = now, updatedAt = now, wakeTime = "06:30"))
        dao.insertTodos(listOf(Todo(id = "t1", title = "Buy milk", createdAt = now, updatedAt = now)))
        dao.insertWeightLogs(listOf(WeightLog("w1", "2026-11-07", 81.5, false, now)))
        dao.insertRoutineTemplate(RoutineTemplate(id = "r1", name = "Mine", repeatType = "ONCE", onceDate = "2026-11-09"))
        dao.insertRoutineTemplateItems(listOf(RoutineTemplateItem("i1", "r1", "Stretch", "HEALTH", 1, "07:00")))
        dao.insertExerciseLibrary(listOf(ExerciseLibraryItem("custom_1", "My curl", "BICEPS", 3, 8, 12, 60, null, null)))

        val (json, summary) = CloudBackupService.buildBackup(dao)
        assertEquals(1, summary.todosCount)

        db.clearAllTables()
        CloudBackupService.restoreFromJson(dao, json)

        assertEquals("Buy milk", dao.getAllTodos().single().title)
        assertEquals(81.5, dao.getAllWeightLogs().single().weightKg, 0.0)
        val template = dao.getRoutineTemplates().single()
        assertEquals("2026-11-09", template.onceDate)
        assertEquals("Stretch", dao.getAllRoutineTemplateItems().single().title)
        assertTrue(dao.getExerciseLibrary().any { it.id == "custom_1" })
        val settings = dao.getAppSettingsDirectly()!!
        assertEquals("06:30", settings.wakeTime)
        assertEquals(80.0, settings.targetWeightKg, 0.0)
        assertTrue(settings.onboardingAnswered)
    }
}
