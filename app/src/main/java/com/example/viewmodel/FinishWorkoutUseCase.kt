package com.example.viewmodel

import androidx.room.withTransaction
import com.example.data.AppDatabase
import com.example.data.ExerciseProgressionState
import com.example.data.SyncWellDao
import com.example.data.WorkoutSession
import com.example.data.WorkoutSessionExercise
import com.example.data.WorkoutSetLog
import com.example.domain.NextTargetRule
import com.example.domain.PersonalBestRule
import com.example.domain.SetRef
import com.example.domain.WeeklyGymStreak

/**
 * Saves a finished workout in one transaction (a kill halfway must not leave an ended session with only some
 * next-time targets saved) and builds the summary. The next-time targets saved here are the ones the next
 * session loads, so the summary and the next start always agree.
 */
class FinishWorkoutUseCase(private val db: AppDatabase, private val dao: SyncWellDao) {

    suspend operator fun invoke(
        session: WorkoutSession,
        exercises: List<WorkoutSessionExercise>,
        logs: Map<String, List<WorkoutSetLog>>,
        activeSeconds: Int,
        endedIso: String,
        exerciseNames: Map<String, String>,
        today: String,
        cardio: CardioLine?
    ): WorkoutSummary {
        val bests = mutableListOf<PersonalBestLine>()
        val targets = mutableListOf<NextTargetLine>()
        var setCount = 0
        db.withTransaction {
            dao.insertWorkoutSession(session.copy(endedAt = endedIso, activeSeconds = activeSeconds))
            exercises.forEach { se ->
                val completed = (logs[se.id] ?: emptyList()).filter { it.isCompleted }
                if (completed.isEmpty()) return@forEach
                setCount += completed.size
                val name = exerciseNames[se.exerciseId] ?: se.exerciseId

                val sets = completed.map { SetRef(it.weightKg, it.reps) }
                val prIdx = PersonalBestRule.newBestIndex(
                    dao.getBestFinishedSetForExercise(se.exerciseId, session.id)?.let { SetRef(it.weightKg, it.reps) }, sets
                )
                if (prIdx != null) bests += PersonalBestLine(name, sets[prIdx].weightKg, sets[prIdx].reps)

                val next = NextTargetRule.next(completed, se.targetRepsMax, se.targetWeightKg ?: 0.0) ?: return@forEach
                dao.insertExerciseProgression(ExerciseProgressionState(se.exerciseId, next.weightKg, next.reps, endedIso))
                targets += NextTargetLine(name, next)
            }
        }
        val target = dao.getAppSettingsDirectly()?.weeklyGymTarget ?: 3
        return WorkoutSummary(
            dayType = session.dayType,
            activeSeconds = activeSeconds,
            sets = setCount,
            exercises = exercises.count { e -> (logs[e.id] ?: emptyList()).any { it.isCompleted } },
            cardio = cardio,
            personalBests = bests,
            nextTargets = targets,
            streak = WeeklyGymStreak.compute(dao.getFinishedSessionDates(), target, today)
        )
    }
}
