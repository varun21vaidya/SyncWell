package com.example.domain

/**
 * Active-time clock for a workout. Only time spent running counts, so a pause stops it and the
 * saved session time excludes pauses. [runningSinceMs] is null while paused.
 */
data class SessionClock(val accumulatedMs: Long = 0L, val runningSinceMs: Long? = null) {
    val isPaused: Boolean get() = runningSinceMs == null

    fun elapsedMs(nowMs: Long): Long = accumulatedMs + (runningSinceMs?.let { (nowMs - it).coerceAtLeast(0L) } ?: 0L)

    fun elapsedSeconds(nowMs: Long): Int = (elapsedMs(nowMs) / 1000L).toInt()

    fun pause(nowMs: Long): SessionClock = if (isPaused) this else SessionClock(elapsedMs(nowMs), null)

    fun resume(nowMs: Long): SessionClock = if (isPaused) SessionClock(accumulatedMs, nowMs) else this

    companion object {
        fun startedAt(nowMs: Long) = SessionClock(0L, nowMs)

        /** Encoding for SharedPreferences: runningSince is -1 while paused. */
        fun fromStored(accumulatedMs: Long, runningSinceMs: Long): SessionClock =
            SessionClock(accumulatedMs.coerceAtLeast(0L), runningSinceMs.takeIf { it >= 0L })
    }
}
