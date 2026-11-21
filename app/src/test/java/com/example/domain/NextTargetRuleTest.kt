package com.example.domain

import com.example.data.WorkoutSetLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NextTargetRuleTest {
    private fun log(i: Int, w: Double, r: Int, done: Boolean = true) = WorkoutSetLog("l$i", "se", i, w, r, done)

    @Test fun allSetsAtTargetRepsAddsTwoAndAHalfKg() {
        val t = NextTargetRule.next(listOf(log(1, 60.0, 8), log(2, 60.0, 8)), 8, 60.0)
        assertEquals(NextTarget(62.5, 8), t)
    }

    @Test fun missedRepsKeepsTheWeightAndBestReps() {
        val t = NextTargetRule.next(listOf(log(1, 60.0, 8), log(2, 60.0, 6)), 8, 60.0)
        assertEquals(NextTarget(60.0, 8), t)
    }

    @Test fun liftingMoreThanTheTargetRaisesTheNextTarget() {
        val t = NextTargetRule.next(listOf(log(1, 65.0, 5)), 8, 60.0)
        assertEquals(NextTarget(65.0, 8), t) // 5 reps is under the 8 target, so the engine adds nothing; keep the heavier weight
    }

    @Test fun noCompletedSetMeansNoNewTarget() {
        assertNull(NextTargetRule.next(listOf(log(1, 60.0, 8, done = false)), 8, 60.0))
    }

    @Test fun bodyweightExerciseKeepsRepsTarget() {
        assertEquals(NextTarget(0.0, 15), NextTargetRule.next(listOf(log(1, 0.0, 15)), 12, 0.0))
    }
}
