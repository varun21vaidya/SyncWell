package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionClockTest {
    @Test fun runningClockCountsFromStart() {
        val c = SessionClock.startedAt(1_000)
        assertEquals(60, c.elapsedSeconds(61_000))
    }

    @Test fun pauseStopsTheClockAndResumeContinues() {
        val paused = SessionClock.startedAt(0).pause(30_000)
        assertTrue(paused.isPaused)
        assertEquals(30, paused.elapsedSeconds(500_000)) // a long pause adds nothing
        val resumed = paused.resume(500_000)
        assertFalse(resumed.isPaused)
        assertEquals(40, resumed.elapsedSeconds(510_000))
    }

    @Test fun pausingTwiceOrResumingTwiceChangesNothing() {
        val paused = SessionClock.startedAt(0).pause(10_000)
        assertEquals(paused, paused.pause(20_000))
        val running = paused.resume(20_000)
        assertEquals(running, running.resume(30_000))
    }

    @Test fun storedFormRoundTripsBothStates() {
        val running = SessionClock(5_000, 9_000)
        assertEquals(running, SessionClock.fromStored(running.accumulatedMs, running.runningSinceMs!!))
        val paused = SessionClock(7_000, null)
        assertEquals(paused, SessionClock.fromStored(7_000, -1))
    }

    @Test fun clockNeverGoesNegativeIfTheDeviceClockMovesBack() {
        assertEquals(0, SessionClock.startedAt(10_000).elapsedSeconds(5_000))
    }
}
