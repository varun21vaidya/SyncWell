package com.example.domain

import com.example.data.WorkoutSetLog
import com.example.service.ProgressiveOverloadEngine

/** The weight and reps an exercise starts with next time. The summary shows it and the next session loads it. */
data class NextTarget(val weightKg: Double, val reps: Int)

object NextTargetRule {
    /**
     * From the sets finished in a session and the target it started with. Null when no set was completed
     * (the exercise keeps whatever target it had). The overload engine adds 2.5 kg only when every set hit the target reps.
     */
    fun next(completed: List<WorkoutSetLog>, targetReps: Int, targetWeightKg: Double): NextTarget? {
        val done = completed.filter { it.isCompleted }
        val best = done.maxWithOrNull(compareBy({ it.weightKg }, { it.reps })) ?: return null
        val weight = maxOf(best.weightKg, targetWeightKg)
        val reps = if (best.weightKg >= targetWeightKg) maxOf(best.reps, targetReps) else targetReps
        val engine = ProgressiveOverloadEngine.evaluateProgression(done, targetReps, targetWeightKg)
        return if (engine > weight) NextTarget(engine, targetReps) else NextTarget(weight, reps)
    }
}
