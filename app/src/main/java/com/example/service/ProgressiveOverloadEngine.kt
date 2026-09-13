package com.example.service

import com.example.data.WorkoutSetLog

object ProgressiveOverloadEngine {

    /**
     * Evaluates a list of completed workout set logs.
     * If all sets are completed and the rep count matches or exceeds the target rep count in every completed set,
     * the engine recommends increasing the weight by 2.5 kg.
     */
    fun evaluateProgression(completedSets: List<WorkoutSetLog>, targetReps: Int, currentWeight: Double): Double {
        if (completedSets.isEmpty()) return currentWeight

        val allCompletedWithMaxReps = completedSets.all { log ->
            log.isCompleted && log.reps >= targetReps
        }

        return if (allCompletedWithMaxReps) {
            currentWeight + 2.5
        } else {
            currentWeight
        }
    }
}
