package com.example.viewmodel

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class JourneyUseCaseTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: SyncWellDao
    private lateinit var journey: JourneyUseCase

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java).allowMainThreadQueries().build()
        dao = db.syncWellDao()
        journey = JourneyUseCase(db, dao)
    }

    @After fun tearDown() = db.close()

    private suspend fun day(date: String, pct: Double, vacation: Boolean = false) =
        dao.insertDailyAnalytics(DailyAnalytics(dateKey = date, routineCompletionPct = pct, isVacation = vacation, weightKg = null, updatedAt = "x"))

    private fun goal(id: String, region: Int?, kind: String = "COMMITMENT", end: String = "2026-11-29", measure: String = "COUNT", target: Double = 3.0, reward: String? = null, focus: Boolean = false) =
        Goal(id, id, kind, true, measure, target, focusLinked = focus, windowStart = "2026-11-23", windowEnd = end, regionIndex = region, rewardText = reward)

    @Test fun startingFreshHasNoBackfillAndSyncOnlyCountsEndedDays() = runBlocking {
        day("2026-11-20", 1.0) // before the journey: must not count
        day("2026-11-23", 1.0); day("2026-11-24", 0.9); day("2026-11-25", 1.0) // 25 is today
        journey.enable("2026-11-23", "ivy")
        val events = journey.sync("2026-11-25")
        val s = dao.getJourneyState()!!
        assertEquals(2, s.stepInRegion) // 23 and 24, not today and not the day before the start
        assertEquals("2026-11-24", s.lastProcessedDate)
        assertEquals("ivy", s.characterId)
        assertEquals(2, events.size)
        assertTrue(journey.sync("2026-11-25").isEmpty()) // nothing new
    }

    @Test fun aDaysWithoutRowsCountsAsMissedAndVacationIsSkipped() = runBlocking {
        journey.enable("2026-11-22", "max")
        dao.upsertJourneyState(dao.getJourneyState()!!.copy(regionIndex = 2, stepInRegion = 9))
        day("2026-11-23", 0.0, vacation = true)
        dao.insertVacationDay(VacationDay("v", "2026-11-24", createdAt = "x", kind = "FREEZE"))
        journey.sync("2026-11-25") // 23 and 24 skipped, nothing else ended
        assertEquals(9, dao.getJourneyState()!!.stepInRegion)
        journey.sync("2026-11-27") // 25 uses the freeze, 26 goes back to the checkpoint
        assertEquals(7, dao.getJourneyState()!!.stepInRegion)
    }

    @Test fun offThenOnRestartsTheCurrentRegionButKeepsEarlierOnes() = runBlocking {
        journey.enable("2026-11-23", "max")
        dao.upsertJourneyState(dao.getJourneyState()!!.copy(regionIndex = 3, stepInRegion = 11, furthestRegion = 3))
        journey.disable()
        assertFalse(dao.getJourneyState()!!.enabled)
        assertTrue(journey.sync("2026-12-01").isEmpty()) // off: nothing is processed
        journey.enable("2026-12-01", "max")
        val s = dao.getJourneyState()!!
        assertEquals(3, s.regionIndex); assertEquals(0, s.stepInRegion); assertEquals(3, s.furthestRegion)
        assertEquals("2026-11-30", s.lastProcessedDate)
    }

    @Test fun reviewDecidesFromTheLoggedDataAndAMissedCommitmentSendsTheCharacterToTheRegionStart() = runBlocking {
        journey.enable("2026-11-23", "max")
        dao.upsertJourneyState(dao.getJourneyState()!!.copy(regionIndex = 2, stepInRegion = 11))
        journey.saveGoal(goal("short", 2)); journey.saveGoal(goal("done", 2)); journey.saveGoal(goal("weekly", null, kind = "WEEKLY"))
        journey.addProgress("done", "2026-11-24", 3.0)
        val changed = journey.review("2026-12-02") // opened days late: same verdict as on the 30th
        assertEquals(mapOf("short" to "MISSED", "done" to "MET", "weekly" to "MISSED"), changed.associate { it.id to it.status })
        assertEquals(0, dao.getJourneyState()!!.stepInRegion)
        assertTrue(journey.review("2026-12-02").isEmpty()) // already decided
    }

    @Test fun aWeeklyGoalNeverCostsPosition() = runBlocking {
        journey.enable("2026-11-23", "max")
        dao.upsertJourneyState(dao.getJourneyState()!!.copy(regionIndex = 2, stepInRegion = 11))
        journey.saveGoal(goal("weekly", 2, kind = "WEEKLY"))
        journey.review("2026-12-02")
        assertEquals(11, dao.getJourneyState()!!.stepInRegion)
    }

    @Test fun focusLinkedDurationCountsTaggedSessionsInHours() = runBlocking {
        journey.saveGoal(goal("hours", 2, measure = "DURATION", target = 2.0, focus = true))
        dao.insertTrackerSession(TrackerSession("a", "STUDY", "TIMER", "x", "y", 60, "2026-11-24", goalId = "hours"))
        dao.insertTrackerSession(TrackerSession("b", "STUDY", "TIMER", "x", "y", 30, "2026-11-25", goalId = "hours"))
        dao.insertTrackerSession(TrackerSession("c", "STUDY", "TIMER", "x", "y", 600, "2026-11-25")) // not tagged
        journey.addProgress("hours", "2026-11-26", 0.5, "reading")
        assertEquals(2.0, journey.entriesOf(dao.getAllGoals().single()).sumOf { it.amount }, 1e-9)
        assertEquals("MET", journey.review("2026-11-27").single().status) // met before the window ends
    }

    @Test fun theGateOpensOnlyWhenEveryCommitmentIsMetAndStampsThePassport() = runBlocking {
        journey.enable("2026-11-23", "max")
        dao.upsertJourneyState(dao.getJourneyState()!!.copy(regionIndex = 2, stepInRegion = 14))
        journey.saveGoal(goal("a", 2)); journey.saveGoal(goal("b", 2))
        journey.addProgress("a", "2026-11-24", 3.0); journey.review("2026-11-25")
        assertFalse(journey.crossGate("2026-11-25", "Ironwood Outpost")) // b is still open
        journey.addProgress("b", "2026-11-26", 3.0); journey.review("2026-11-26")
        assertTrue(journey.crossGate("2026-11-26", "Ironwood Outpost"))
        val s = dao.getJourneyState()!!
        assertEquals(3, s.regionIndex); assertEquals(0, s.stepInRegion); assertEquals(3, s.furthestRegion)
        assertEquals("Ironwood Outpost", dao.getAllPassportStamps().single().label)
        assertFalse(journey.crossGate("2026-11-26", "x")) // no longer at a gate
    }

    @Test fun aRegionWithNoCommitmentsHasAnOpenGate() = runBlocking {
        assertTrue(journey.commitmentsMet(0))
    }

    @Test fun aRewardIsClaimedOnceAndOnlyWhenMet() = runBlocking {
        journey.saveGoal(goal("g", 2, reward = "Burger"))
        assertFalse(journey.claimReward("g", "2026-11-26")) // still pending
        journey.addProgress("g", "2026-11-24", 3.0); journey.review("2026-11-26")
        assertTrue(journey.claimReward("g", "2026-11-27"))
        assertFalse(journey.claimReward("g", "2026-11-28"))
        assertEquals("2026-11-27", dao.getAllGoals().single().rewardClaimedAt)
        assertEquals("Claimed: Burger", dao.getAllJourneyNotes().single().text)
    }
}
