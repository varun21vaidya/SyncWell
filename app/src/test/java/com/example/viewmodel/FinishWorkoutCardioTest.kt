package com.example.viewmodel

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FinishWorkoutCardioTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: SyncWellDao

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java).allowMainThreadQueries().build()
        dao = db.syncWellDao()
    }

    @After fun tearDown() = db.close()

    private suspend fun finish(id: String, cardio: CardioLine?): WorkoutSummary {
        val session = WorkoutSession(id, "2026-11-10", "PUSH", "GYM", "2026-11-10T10:00:00Z", null)
        dao.insertWorkoutSession(session)
        val se = WorkoutSessionExercise("se$id", id, "ex_flat_bench", 1, 3, 8, 60.0)
        dao.insertExerciseLibrary(listOf(ExerciseLibraryItem("ex_flat_bench", "Bench", "CHEST", 3, 8, 12, 90, null, null)))
        dao.insertWorkoutSessionExercise(se)
        val logs = mapOf(se.id to listOf(WorkoutSetLog("l$id", se.id, 1, 60.0, 8, true)))
        return FinishWorkoutUseCase(db, dao)(session, listOf(se), logs, 1800, "2026-11-10T10:30:00Z", mapOf("ex_flat_bench" to "Bench"), "2026-11-10", cardio)
    }

    @Test fun finishingWithCardioSavesItAndTheSummaryShowsIt() = runBlocking {
        val summary = finish("s1", CardioLine("TREADMILL", 15, 2.0, 6.0, "HARD"))
        assertEquals(15, summary.cardio?.minutes)
        val saved = dao.getAllCardioEntries().single()
        assertEquals("s1", saved.sessionId); assertEquals(6.0, saved.inclinePct!!, 0.0); assertEquals("HARD", saved.effort)
        assertEquals(1, dao.getCardioEntriesFlow().first().size)
    }

    @Test fun finishingWithoutCardioSavesNoCardioLine() = runBlocking {
        val summary = finish("s2", null)
        assertNull(summary.cardio)
        assertTrue(dao.getAllCardioEntries().isEmpty())
    }

    @Test fun cardioOfADiscardedSessionNeverCounts() = runBlocking {
        finish("s3", CardioLine("WALK", 30, null, null, "EASY"))
        dao.softDeleteWorkoutSession("s3")
        assertTrue(dao.getCardioEntriesFlow().first().isEmpty())
    }
}
