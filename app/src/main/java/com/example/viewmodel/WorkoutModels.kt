package com.example.viewmodel

import com.example.data.WorkoutSession
import com.example.domain.GymStreakResult
import com.example.domain.NextTarget
import com.example.domain.SetRef
import com.example.domain.Split
import java.text.SimpleDateFormat
import java.util.Locale

/** What the last finished session of an exercise looked like, for the "previous" column and personal-best checks. */
data class ExerciseHistory(val previous: List<SetRef> = emptyList(), val best: SetRef? = null)

/** Rest countdown shown under the session header. [remaining] stays at 0 for a few seconds so it can buzz and say "done". */
data class RestState(val remaining: Int, val total: Int, val nextLabel: String)

/** A finished session as listed in Recent and in the full history. Volume is deliberately not part of it. */
data class WorkoutHistoryItem(
    val session: WorkoutSession,
    val sets: Int,
    val exercises: Int,
    val newBests: Int,
    val activeSeconds: Int,
    val cardio: List<CardioLine> = emptyList()
)

data class CardioLine(val type: String, val minutes: Int, val distanceKm: Double?, val inclinePct: Double?, val effort: String)

data class WorkoutStartState(
    val next: Split = Split.PUSH,
    val nextLastDate: String? = null,
    val nextLastBest: String? = null,
    val lastDone: Map<Split, String> = emptyMap(),
    val weekDates: List<String> = emptyList(),
    val weekDone: Set<String> = emptySet(),
    val thisWeekCount: Int = 0,
    val weeklyTarget: Int = 3,
    val today: String = "",
    val recent: List<WorkoutHistoryItem> = emptyList(),
    val cardioMinutesThisWeek: Int = 0
)

data class PersonalBestLine(val exerciseName: String, val weightKg: Double, val reps: Int)
data class NextTargetLine(val exerciseName: String, val target: NextTarget)

/** Shown once after finishing, before the ask-first routine prompt. */
data class WorkoutSummary(
    val dayType: String,
    val activeSeconds: Int,
    val sets: Int,
    val exercises: Int,
    val cardio: CardioLine?,
    val personalBests: List<PersonalBestLine>,
    val nextTargets: List<NextTargetLine>,
    val streak: GymStreakResult
)

private val isoFormat get() = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)

/** Session time for sessions saved before active time was tracked: start to end. */
fun fallbackSeconds(startedAt: String, endedAt: String?): Int {
    if (endedAt == null) return 0
    return try {
        val f = isoFormat
        ((f.parse(endedAt)!!.time - f.parse(startedAt)!!.time) / 1000L).toInt().coerceAtLeast(0)
    } catch (e: Exception) { 0 }
}

fun formatKg(kg: Double): String =
    if (kg % 1.0 == 0.0) kg.toInt().toString() else String.format(Locale.US, "%.1f", kg)

fun formatClock(totalSeconds: Int): String {
    val s = totalSeconds.coerceAtLeast(0)
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, sec) else String.format(Locale.US, "%02d:%02d", m, sec)
}
