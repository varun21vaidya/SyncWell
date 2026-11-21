package com.example.service

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GoalServiceTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: SyncWellDao
    private val zone = TimeZone.getDefault()

    @Before fun setUp() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC")) // stored timestamps are UTC; keep local = UTC in the test
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java).allowMainThreadQueries().build()
        dao = db.syncWellDao()
    }

    @After fun tearDown() { db.close(); TimeZone.setDefault(zone) }

    private fun goal(id: String, type: String, target: Int? = null) =
        Todo(id, "Goal $id", "MEDIUM", type, false, false, "2026-11-01T08:00:00Z", "2026-11-01T08:00:00Z", target)

    @Test fun completingThenResettingLeavesAHistoryRecord() = runBlocking {
        val g = goal("g1", "SHORT_TERM"); dao.insertTodo(g)
        GoalService.setCompleted(dao, g, true, "2026-11-18", "2026-11-18T09:00:00Z")
        GoalService.resetAtBoundary(dao, "2026-11-19", "2026-11-19T00:01:00Z")
        assertFalse(dao.getAllTodos().single().isCompleted)
        assertEquals(listOf("2026-11-18"), dao.getAllGoalCompletions().map { it.completedOn })
    }

    @Test fun untickingTheSameDayRemovesTheHistoryRow() = runBlocking {
        val g = goal("g1", "SHORT_TERM"); dao.insertTodo(g)
        GoalService.setCompleted(dao, g, true, "2026-11-18", "2026-11-18T09:00:00Z")
        GoalService.setCompleted(dao, g, false, "2026-11-18", "2026-11-18T09:05:00Z")
        assertTrue(dao.getAllGoalCompletions().isEmpty())
    }

    @Test fun aGoalIsNotRecordedTwiceForTheSameDay() = runBlocking {
        val g = goal("g1", "SHORT_TERM"); dao.insertTodo(g)
        GoalService.setCompleted(dao, g, true, "2026-11-18", "2026-11-18T09:00:00Z")
        GoalService.resetAtBoundary(dao, "2026-11-19", "2026-11-19T00:01:00Z")
        assertEquals(1, dao.getAllGoalCompletions().size)
    }

    @Test fun weeklyGoalSurvivesUntilItsWeekIsOverEvenWithoutASundayOpen() = runBlocking {
        val g = goal("w1", "LONG_TERM"); dao.insertTodo(g)
        GoalService.setCompleted(dao, g, true, "2026-11-18", "2026-11-18T09:00:00Z") // Wednesday
        GoalService.resetAtBoundary(dao, "2026-11-21", "2026-11-21T08:00:00Z") // Saturday, same week
        assertTrue(dao.getAllTodos().single().isCompleted)
        GoalService.resetAtBoundary(dao, "2026-11-23", "2026-11-23T08:00:00Z") // Monday, opened first time since
        assertFalse(dao.getAllTodos().single().isCompleted)
        assertEquals(1, dao.getAllGoalCompletions().size)
    }

    @Test fun aGoalWithAWeeklyTargetResetsEachDayAndCountsDays() = runBlocking {
        val g = goal("t1", "LONG_TERM", target = 3); dao.insertTodo(g)
        GoalService.setCompleted(dao, g, true, "2026-11-16", "2026-11-16T09:00:00Z")
        GoalService.resetAtBoundary(dao, "2026-11-17", "2026-11-17T00:01:00Z")
        assertFalse(dao.getAllTodos().single().isCompleted)
        val ticked = dao.getAllTodos().single()
        GoalService.setCompleted(dao, ticked, true, "2026-11-17", "2026-11-17T09:00:00Z")
        val week = dao.getGoalCompletionsBetween("2026-11-16", "2026-11-22").first()
        assertEquals(2, week.size)
    }
}
