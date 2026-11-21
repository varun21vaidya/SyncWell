package com.example.data

/** One completed set of a finished, kept session, as read for history, personal bests and the start screen. */
data class FinishedSetRow(
    val sessionId: String,
    val date: String,
    val startedAt: String,
    val exerciseId: String,
    val weightKg: Double,
    val reps: Int
)
