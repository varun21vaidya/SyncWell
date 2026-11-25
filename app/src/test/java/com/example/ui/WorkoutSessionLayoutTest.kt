package com.example.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.data.ExerciseLibraryItem
import com.example.data.WorkoutSession
import com.example.data.WorkoutSessionExercise
import com.example.data.WorkoutSetLog
import com.example.domain.SessionClock
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.ExerciseHistory
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Renders the session screen at the width of the owner's phone (display scaled up, about 324dp wide)
 * and checks what went wrong there: the reps "+" was pushed off screen and "Paused" wrapped one letter per line.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w324dp-h760dp-xxhdpi")
class WorkoutSessionLayoutTest {
    @get:Rule val compose = createComposeRule()

    private val session = WorkoutSession("s1", "2026-11-21", "Push Day — Chest + Shoulders + Triceps", "GYM", "2026-11-21T10:00:00Z", null)
    private val exercise = WorkoutSessionExercise("se1", "s1", "ex_flat_bench", 1, 3, 8, 22.5)
    private val library = mapOf("ex_flat_bench" to ExerciseLibraryItem("ex_flat_bench", "Barbell Flat Bench Press", "CHEST", 3, 6, 8, 90, null, null))
    private val logs = mapOf("se1" to (1..3).map { WorkoutSetLog("l$it", "se1", it, 22.5, 19, false) })

    private var kgDeltas = mutableListOf<Pair<Int, Double>>()
    private var pauses = 0
    private var resumes = 0

    private fun show(clock: SessionClock) = compose.setContent {
        MyApplicationTheme {
            ActiveSessionContent(
                session = session, exercises = listOf(exercise), logs = logs, library = library,
                history = mapOf("ex_flat_bench" to ExerciseHistory()), clock = clock, rest = null,
                onBack = {}, onPause = { pauses++ }, onResume = { resumes++ }, onFinish = {}, onDiscard = {},
                onSkipRest = {}, onAdjustRest = {}, onCompleteSet = { _, _, _, _ -> }, onUncompleteSet = { _, _ -> },
                onSetValues = { _, _, reps, kg -> kgDeltas += reps to kg },
                onAddSet = {}, onDeleteSet = { _, _ -> }, onRemoveExercise = {}, onNotes = {}, onAddExercise = {}
            )
        }
    }

    @Test fun repsPlusAndMinusAreOnScreenAndIncreaseReps() {
        show(SessionClock.startedAt(System.currentTimeMillis()))
        compose.onAllNodesWithDescriptionFirst("Increase reps").assertIsDisplayed()
        compose.onAllNodesWithDescriptionFirst("Decrease reps").assertIsDisplayed()
        compose.onAllNodesWithDescriptionFirst("Increase kg").assertIsDisplayed()
        compose.onAllNodesWithDescriptionFirst("Increase reps").performClick()
        assertEquals(listOf(20 to 22.5), kgDeltas) // 19 reps + 1, weight unchanged
    }

    @Test fun pausedHeaderShowsLabelAndResumeWorks() {
        show(SessionClock(accumulatedMs = 10_000, runningSinceMs = null))
        // Robolectric has no real fonts (every line measures 24dp, text is ~0 wide), so text sizes cannot be checked here:
        // the label wrap itself is checked on the phone. This only checks that the header parts are all on screen.
        compose.onNodeWithTag("paused_label").assertIsDisplayed()
        compose.onNodeWithTag("session_timer").assertIsDisplayed()
        compose.onNodeWithTag("finish_workout_button").assertIsDisplayed()
        compose.onNodeWithTag("pause_resume_button").performClick()
        assertEquals(1, resumes)
        assertEquals(0, pauses)
    }

    @Test fun pauseButtonCallsPauseWhileRunning() {
        show(SessionClock.startedAt(System.currentTimeMillis()))
        compose.onNodeWithTag("pause_resume_button").performClick()
        assertEquals(1, pauses)
    }

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.onAllNodesWithDescriptionFirst(d: String) =
        onAllNodesWithContentDescription(d)[0]
}
