package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncWellDao {

    // App Settings
    @Query("SELECT * FROM app_settings LIMIT 1")
    suspend fun getAppSettingsDirectly(): AppSettings?

    @Query("SELECT * FROM app_settings LIMIT 1")
    fun getAppSettings(): Flow<AppSettings?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppSettings(settings: AppSettings)

    // Notification Settings
    @Query("SELECT * FROM notification_settings LIMIT 1")
    fun getNotificationSettings(): Flow<NotificationSettings?>

    @Query("SELECT * FROM notification_settings LIMIT 1")
    suspend fun getNotificationSettingsDirectly(): NotificationSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotificationSettings(settings: NotificationSettings)

    // Reminders
    @Query("SELECT * FROM reminders WHERE isDeleted = 0")
    fun getActiveReminders(): Flow<List<Reminder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: Reminder)

    // Vacation Days
    @Query("SELECT * FROM vacation_days WHERE isDeleted = 0")
    fun getActiveVacationDays(): Flow<List<VacationDay>>

    @Query("SELECT * FROM vacation_days WHERE date = :date AND isDeleted = 0 LIMIT 1")
    suspend fun getVacationDayByDate(date: String): VacationDay?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVacationDay(day: VacationDay)

    // Habit streaks: every routine item that came from a template item, and which days had a routine at all
    @Query("SELECT r.date AS dateKey, i.templateItemId AS templateItemId, i.isDone AS done FROM daily_routine_items i JOIN daily_routines r ON r.id = i.dailyRoutineId WHERE i.templateItemId IS NOT NULL")
    fun getHabitDaysFlow(): Flow<List<HabitDayRow>>

    @Query("SELECT date FROM daily_routines")
    fun getRoutineDatesFlow(): Flow<List<String>>

    // Routine Templates
    @Query("SELECT * FROM routine_templates")
    suspend fun getRoutineTemplates(): List<RoutineTemplate>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutineTemplate(template: RoutineTemplate)

    @Query("SELECT * FROM routine_template_items WHERE templateId = :templateId ORDER BY orderIndex ASC")
    suspend fun getRoutineTemplateItems(templateId: String): List<RoutineTemplateItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutineTemplateItems(items: List<RoutineTemplateItem>)

    @Query("SELECT * FROM routine_templates ORDER BY name")
    fun getRoutineTemplatesFlow(): Flow<List<RoutineTemplate>>

    @Query("SELECT * FROM routine_template_items WHERE templateId = :templateId ORDER BY orderIndex ASC")
    fun getRoutineTemplateItemsFlow(templateId: String): Flow<List<RoutineTemplateItem>>

    @Query("SELECT * FROM routine_template_items")
    suspend fun getAllRoutineTemplateItems(): List<RoutineTemplateItem>

    @Query("DELETE FROM routine_templates WHERE id = :id")
    suspend fun deleteRoutineTemplate(id: String)

    @Query("DELETE FROM routine_templates WHERE isStarter = 1")
    suspend fun deleteStarterTemplates()

    @Query("DELETE FROM routine_template_items WHERE id = :id")
    suspend fun deleteRoutineTemplateItem(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyRoutineItem(item: DailyRoutineItem)

    @Query("DELETE FROM daily_routine_items WHERE id = :id")
    suspend fun deleteDailyRoutineItem(id: String)

    @Query("DELETE FROM routine_templates")
    suspend fun clearRoutineTemplates()

    // Daily Routines
    @Query("SELECT * FROM daily_routines WHERE date = :date LIMIT 1")
    suspend fun getDailyRoutineDirectly(date: String): DailyRoutine?

    @Query("SELECT * FROM daily_routines WHERE date = :date LIMIT 1")
    fun getDailyRoutine(date: String): Flow<DailyRoutine?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyRoutine(routine: DailyRoutine)

    @Update
    suspend fun updateDailyRoutine(routine: DailyRoutine)

    @Query("DELETE FROM daily_routine_items WHERE dailyRoutineId IN (SELECT id FROM daily_routines WHERE date = :date)")
    suspend fun clearDailyRoutineItemsForDate(date: String)

    @Query("DELETE FROM daily_routines WHERE date = :date")
    suspend fun clearDailyRoutineForDate(date: String)

    // Daily Routine Items
    @Query("SELECT * FROM daily_routine_items WHERE dailyRoutineId = (SELECT id FROM daily_routines WHERE date = :date LIMIT 1) ORDER BY orderIndex ASC")
    fun getDailyRoutineItemsForDateFlow(date: String): Flow<List<DailyRoutineItem>>

    @Query("SELECT * FROM daily_routine_items WHERE dailyRoutineId = :dailyRoutineId ORDER BY orderIndex ASC")
    suspend fun getDailyRoutineItems(dailyRoutineId: String): List<DailyRoutineItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyRoutineItems(items: List<DailyRoutineItem>)

    @Update
    suspend fun updateDailyRoutineItem(item: DailyRoutineItem)

    @Query("UPDATE workout_session_exercises SET targetRepsMax = :reps, targetWeightKg = :weight WHERE id = :id")
    suspend fun updateWorkoutSessionExerciseTargets(id: String, reps: Int, weight: Double)

    // Workout Templates
    // Exercise Library
    @Query("SELECT * FROM exercise_library")
    suspend fun getExerciseLibrary(): List<ExerciseLibraryItem>

    @Query("SELECT * FROM exercise_library")
    fun getExerciseLibraryFlow(): Flow<List<ExerciseLibraryItem>>

    @Query("DELETE FROM exercise_library")
    suspend fun clearExerciseLibrary()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExerciseLibrary(items: List<ExerciseLibraryItem>)

    // Exercise Progression
    @Query("SELECT * FROM exercise_progression_states")
    fun getExerciseProgressionsFlow(): Flow<List<ExerciseProgressionState>>

    @Query("SELECT * FROM exercise_progression_states WHERE exerciseId = :exerciseId LIMIT 1")
    suspend fun getExerciseProgression(exerciseId: String): ExerciseProgressionState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExerciseProgression(state: ExerciseProgressionState)

    // Workout Sessions
    @Query("SELECT * FROM workout_sessions WHERE isDeleted = 0 ORDER BY date DESC")
    fun getWorkoutSessions(): Flow<List<WorkoutSession>>

    @Query("SELECT * FROM workout_sessions WHERE date = :dateKey AND isDeleted = 0")
    suspend fun getWorkoutSessionsByDate(dateKey: String): List<WorkoutSession>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutSession(session: WorkoutSession)

    // Workout Session Exercises
    @Query("SELECT * FROM workout_session_exercises WHERE sessionId = :sessionId ORDER BY orderIndex ASC")
    fun getWorkoutSessionExercisesFlow(sessionId: String): Flow<List<WorkoutSessionExercise>>

    @Query("SELECT * FROM workout_session_exercises WHERE sessionId = :sessionId ORDER BY orderIndex ASC")
    suspend fun getWorkoutSessionExercises(sessionId: String): List<WorkoutSessionExercise>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutSessionExercises(exercises: List<WorkoutSessionExercise>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutSessionExercise(exercise: WorkoutSessionExercise)

    @Delete
    suspend fun deleteWorkoutSessionExercise(exercise: WorkoutSessionExercise)

    @Query("DELETE FROM workout_set_logs WHERE sessionExerciseId = :sessionExerciseId")
    suspend fun deleteSetLogsForExercise(sessionExerciseId: String)

    // Workout Set Logs
    @Query("SELECT * FROM workout_set_logs WHERE sessionExerciseId = :sessionExerciseId ORDER BY setIndex ASC")
    fun getWorkoutSetLogsFlow(sessionExerciseId: String): Flow<List<WorkoutSetLog>>

    @Query("SELECT * FROM workout_set_logs WHERE sessionExerciseId = :sessionExerciseId ORDER BY setIndex ASC")
    suspend fun getWorkoutSetLogs(sessionExerciseId: String): List<WorkoutSetLog>

    @Query("""
        SELECT s.* FROM workout_set_logs s
        JOIN workout_session_exercises e ON s.sessionExerciseId = e.id
        JOIN workout_sessions sess ON e.sessionId = sess.id
        WHERE e.exerciseId = :exerciseId AND s.isCompleted = 1 AND sess.isDeleted = 0
        ORDER BY sess.date DESC, s.setIndex DESC LIMIT 1
    """)
    suspend fun getLastCompletedSetLogForExercise(exerciseId: String): WorkoutSetLog?

    // Workout screen: history, "previous" column and personal bests. Only finished, kept sessions count.
    @Query("SELECT * FROM workout_sessions WHERE isDeleted = 0 AND endedAt IS NOT NULL ORDER BY date DESC, startedAt DESC")
    fun getFinishedSessionsFlow(): Flow<List<WorkoutSession>>

    @Query("""
        SELECT sess.id AS sessionId, sess.date AS date, sess.startedAt AS startedAt, e.exerciseId AS exerciseId, s.weightKg AS weightKg, s.reps AS reps
        FROM workout_set_logs s
        JOIN workout_session_exercises e ON s.sessionExerciseId = e.id
        JOIN workout_sessions sess ON e.sessionId = sess.id
        WHERE s.isCompleted = 1 AND sess.isDeleted = 0 AND sess.endedAt IS NOT NULL
    """)
    fun getFinishedSetRowsFlow(): Flow<List<FinishedSetRow>>

    @Query("""
        SELECT s.* FROM workout_set_logs s
        JOIN workout_session_exercises e ON s.sessionExerciseId = e.id
        JOIN workout_sessions sess ON e.sessionId = sess.id
        WHERE e.exerciseId = :exerciseId AND s.isCompleted = 1 AND sess.isDeleted = 0 AND sess.endedAt IS NOT NULL AND sess.id != :excludeSessionId
        ORDER BY s.weightKg DESC, s.reps DESC LIMIT 1
    """)
    suspend fun getBestFinishedSetForExercise(exerciseId: String, excludeSessionId: String): WorkoutSetLog?

    @Query("""
        SELECT s.* FROM workout_set_logs s
        JOIN workout_session_exercises e ON s.sessionExerciseId = e.id
        JOIN workout_sessions sess ON e.sessionId = sess.id
        WHERE e.exerciseId = :exerciseId AND s.isCompleted = 1 AND sess.isDeleted = 0 AND sess.endedAt IS NOT NULL AND sess.id != :excludeSessionId
        AND sess.id = (
            SELECT x.id FROM workout_sessions x
            JOIN workout_session_exercises xe ON xe.sessionId = x.id
            JOIN workout_set_logs xs ON xs.sessionExerciseId = xe.id
            WHERE xe.exerciseId = :exerciseId AND xs.isCompleted = 1 AND x.isDeleted = 0 AND x.endedAt IS NOT NULL AND x.id != :excludeSessionId
            ORDER BY x.date DESC, x.startedAt DESC LIMIT 1
        )
        ORDER BY s.setIndex ASC
    """)
    suspend fun getPreviousSessionSetsForExercise(exerciseId: String, excludeSessionId: String): List<WorkoutSetLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutSetLogs(logs: List<WorkoutSetLog>)

    @Delete
    suspend fun deleteWorkoutSetLog(log: WorkoutSetLog)

    @Update
    suspend fun updateWorkoutSetLog(log: WorkoutSetLog)

    // Tracker Sessions
    @Query("SELECT * FROM tracker_sessions WHERE dateKey = :dateKey AND isDeleted = 0")
    fun getTrackerSessions(dateKey: String): Flow<List<TrackerSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrackerSession(session: TrackerSession)

    // Supplement Logs
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplementLog(log: SupplementLog)

    // Water Logs
    @Query("SELECT * FROM water_logs WHERE dateKey = :dateKey AND isDeleted = 0")
    fun getWaterLogs(dateKey: String): Flow<List<WaterLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaterLog(log: WaterLog)

    // Steps Logs
    @Query("SELECT * FROM steps_logs WHERE dateKey = :dateKey LIMIT 1")
    fun getStepsLogFlow(dateKey: String): Flow<StepsLog?>

    @Query("SELECT * FROM steps_logs WHERE dateKey = :dateKey LIMIT 1")
    suspend fun getStepsLog(dateKey: String): StepsLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStepsLog(log: StepsLog)

    // Weight Logs
    @Query("SELECT * FROM weight_logs WHERE isDeleted = 0 ORDER BY dateKey DESC")
    fun getWeightLogs(): Flow<List<WeightLog>>

    @Query("SELECT * FROM weight_logs WHERE dateKey = :dateKey AND isDeleted = 0 LIMIT 1")
    suspend fun getWeightLogByDate(dateKey: String): WeightLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeightLog(log: WeightLog)

    // Daily Analytics
    @Query("SELECT * FROM daily_analytics WHERE dateKey = :dateKey LIMIT 1")
    suspend fun getDailyAnalyticsDirectly(dateKey: String): DailyAnalytics?

    @Query("SELECT * FROM daily_analytics WHERE dateKey = :dateKey LIMIT 1")
    fun getDailyAnalytics(dateKey: String): Flow<DailyAnalytics?>

    // Streaks need the whole history, not the latest rows
    @Query("SELECT * FROM daily_analytics ORDER BY dateKey ASC")
    fun getAllDailyAnalyticsFlow(): Flow<List<DailyAnalytics>>

    // Discarded sessions are stored with isDeleted = 1, so they never count
    @Query("SELECT date FROM workout_sessions WHERE isDeleted = 0 AND endedAt IS NOT NULL")
    fun getFinishedSessionDatesFlow(): Flow<List<String>>

    @Query("SELECT date FROM workout_sessions WHERE isDeleted = 0 AND endedAt IS NOT NULL")
    suspend fun getFinishedSessionDates(): List<String>

    @Query("SELECT * FROM daily_analytics WHERE dateKey BETWEEN :from AND :to ORDER BY dateKey")
    fun getAnalyticsBetween(from: String, to: String): Flow<List<DailyAnalytics>>

    @Query("SELECT date FROM workout_sessions WHERE isDeleted = 0 AND endedAt IS NOT NULL AND date >= :from")
    suspend fun getFinishedSessionDatesSince(from: String): List<String>

    @Query("SELECT COUNT(*) FROM daily_analytics WHERE routineCompletionPct >= 0.8")
    fun countQualifyingDays(): Flow<Int>

    @Query("SELECT * FROM workout_sessions WHERE id = :id LIMIT 1")
    suspend fun getWorkoutSessionById(id: String): WorkoutSession?

    @Query("UPDATE workout_sessions SET isDeleted = 1 WHERE endedAt IS NULL AND id != :keepId")
    suspend fun softDeleteUnfinishedSessionsExcept(keepId: String)

    @Query("UPDATE workout_sessions SET isDeleted = 1 WHERE id = :id")
    suspend fun softDeleteWorkoutSession(id: String)

    // Routine items done / total per day, for the heatmap tap text
    @Query("SELECT r.date AS dateKey, COALESCE(SUM(i.isDone), 0) AS done, COUNT(i.id) AS total FROM daily_routines r JOIN daily_routine_items i ON i.dailyRoutineId = r.id WHERE r.date BETWEEN :from AND :to GROUP BY r.date")
    fun getDayCountsBetween(from: String, to: String): Flow<List<DayCount>>

    // Medals: any tick ever, and the longest focus session of each day
    @Query("SELECT COUNT(*) FROM daily_routine_items WHERE isDone = 1")
    fun getTickedItemCountFlow(): Flow<Int>

    @Query("SELECT dateKey AS dateKey, COALESCE(MAX(durationMin), 0) AS maxMinutes FROM tracker_sessions WHERE activityType IN ('STUDY', 'WORK') AND isDeleted = 0 GROUP BY dateKey")
    fun getFocusDaysFlow(): Flow<List<FocusDayRow>>

    // Per category and day over all history, for badges
    @Query("SELECT r.date AS dateKey, i.category AS category, COALESCE(SUM(i.isDone), 0) AS done, COUNT(i.id) AS total FROM daily_routines r JOIN daily_routine_items i ON i.dailyRoutineId = r.id GROUP BY r.date, i.category")
    fun getCategoryDayCountsFlow(): Flow<List<CategoryDayCount>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyAnalytics(analytics: DailyAnalytics)

    // Todos — FEATURE 11: split by goalType
    @Query("SELECT * FROM todos WHERE isDeleted = 0 AND goalType = 'SHORT_TERM' ORDER BY isCompleted ASC, priority DESC, createdAt DESC")
    fun getActiveShortTermGoals(): Flow<List<Todo>>

    @Query("SELECT * FROM todos WHERE isDeleted = 0 AND goalType = 'LONG_TERM' ORDER BY isCompleted ASC, priority DESC, createdAt DESC")
    fun getActiveLongTermGoals(): Flow<List<Todo>>

    // Keep original for backward compat
    @Query("SELECT * FROM todos WHERE isDeleted = 0 ORDER BY priority DESC, createdAt DESC")
    fun getActiveTodos(): Flow<List<Todo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodo(todo: Todo)

    @Update
    suspend fun updateTodo(todo: Todo)

    // FEATURE 11: Auto-reset completed goals
    // Cardio finisher entries; the flow only returns those of finished, kept sessions
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCardioEntry(entry: CardioEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCardioEntries(entries: List<CardioEntry>)

    @Query("SELECT * FROM cardio_entries")
    suspend fun getAllCardioEntries(): List<CardioEntry>

    @Query("SELECT c.* FROM cardio_entries c JOIN workout_sessions s ON c.sessionId = s.id WHERE s.isDeleted = 0 AND s.endedAt IS NOT NULL")
    fun getCardioEntriesFlow(): Flow<List<CardioEntry>>

    // Goal history: one row per goal per day, ignored when it already exists
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGoalCompletion(row: GoalCompletion)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGoalCompletions(rows: List<GoalCompletion>)

    @Query("DELETE FROM goal_completions WHERE todoId = :todoId AND completedOn = :dateKey")
    suspend fun deleteGoalCompletion(todoId: String, dateKey: String)

    @Query("SELECT * FROM goal_completions WHERE completedOn BETWEEN :from AND :to")
    fun getGoalCompletionsBetween(from: String, to: String): Flow<List<GoalCompletion>>

    @Query("SELECT * FROM goal_completions ORDER BY completedOn")
    suspend fun getAllGoalCompletions(): List<GoalCompletion>

    // Exercise Custom Notes — FEATURE 10
    @Query("SELECT * FROM exercise_custom_notes WHERE exerciseId = :exerciseId LIMIT 1")
    suspend fun getExerciseCustomNote(exerciseId: String): ExerciseCustomNote?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExerciseCustomNote(note: ExerciseCustomNote)

    @Query("DELETE FROM exercise_custom_notes WHERE exerciseId = :exerciseId")
    suspend fun deleteExerciseCustomNote(exerciseId: String)

    // Sync Queue
    @Query("SELECT * FROM sync_queue WHERE status = 'PENDING'")
    suspend fun getPendingSyncItems(): List<SyncQueueItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncQueueItem(item: SyncQueueItem)

    // ── Backup & Restore Queries ──
    @Query("SELECT * FROM workout_sessions WHERE isDeleted = 0")
    suspend fun getAllWorkoutSessions(): List<WorkoutSession>

    @Query("SELECT * FROM workout_session_exercises")
    suspend fun getAllWorkoutSessionExercises(): List<WorkoutSessionExercise>

    @Query("SELECT * FROM workout_set_logs")
    suspend fun getAllWorkoutSetLogs(): List<WorkoutSetLog>

    @Query("SELECT * FROM exercise_progression_states")
    suspend fun getAllExerciseProgressions(): List<ExerciseProgressionState>

    @Query("SELECT * FROM tracker_sessions WHERE isDeleted = 0")
    suspend fun getAllTrackerSessions(): List<TrackerSession>

    @Query("SELECT * FROM supplement_logs")
    suspend fun getAllSupplementLogs(): List<SupplementLog>

    @Query("SELECT * FROM water_logs WHERE isDeleted = 0")
    suspend fun getAllWaterLogs(): List<WaterLog>

    @Query("SELECT * FROM steps_logs")
    suspend fun getAllStepsLogs(): List<StepsLog>

    @Query("SELECT * FROM weight_logs WHERE isDeleted = 0")
    suspend fun getAllWeightLogs(): List<WeightLog>

    @Query("SELECT * FROM daily_analytics")
    suspend fun getAllDailyAnalytics(): List<DailyAnalytics>

    @Query("SELECT * FROM exercise_custom_notes")
    suspend fun getAllExerciseCustomNotes(): List<ExerciseCustomNote>

    @Query("SELECT * FROM daily_routines")
    suspend fun getAllDailyRoutines(): List<DailyRoutine>

    @Query("SELECT * FROM daily_routine_items")
    suspend fun getAllDailyRoutineItems(): List<DailyRoutineItem>

    @Query("SELECT * FROM reminders WHERE isDeleted = 0")
    suspend fun getAllReminders(): List<Reminder>

    @Query("SELECT * FROM vacation_days WHERE isDeleted = 0")
    suspend fun getAllVacationDays(): List<VacationDay>

    @Query("SELECT * FROM todos WHERE isDeleted = 0")
    suspend fun getAllTodos(): List<Todo>

    // ── Backup & Restore Batch Inserts ──
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutSessions(sessions: List<WorkoutSession>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExerciseProgressions(states: List<ExerciseProgressionState>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrackerSessions(sessions: List<TrackerSession>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplementLogs(logs: List<SupplementLog>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaterLogs(logs: List<WaterLog>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStepsLogs(logs: List<StepsLog>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeightLogs(logs: List<WeightLog>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyAnalyticsList(analytics: List<DailyAnalytics>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExerciseCustomNotes(notes: List<ExerciseCustomNote>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyRoutines(routines: List<DailyRoutine>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminders(reminders: List<Reminder>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVacationDays(days: List<VacationDay>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodos(todos: List<Todo>)

    // Journey (see JourneyRules); one state row, goals with dated progress, stamps and notes
    @Query("SELECT * FROM journey_state WHERE id = 1")
    suspend fun getJourneyState(): JourneyState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertJourneyState(state: JourneyState)

    @Query("SELECT * FROM journey_state WHERE id = 1")
    fun getJourneyStateFlow(): Flow<JourneyState?>

    @Query("SELECT * FROM goals ORDER BY windowEnd")
    fun getGoalsFlow(): Flow<List<Goal>>

    @Query("SELECT * FROM goals")
    suspend fun getAllGoals(): List<Goal>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoals(goals: List<Goal>)

    @Query("SELECT * FROM goal_progress")
    suspend fun getAllGoalProgress(): List<GoalProgress>

    @Query("SELECT * FROM goal_progress WHERE goalId = :goalId")
    suspend fun getGoalProgress(goalId: String): List<GoalProgress>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoalProgressEntry(entry: GoalProgress)

    @Query("SELECT * FROM tracker_sessions WHERE goalId = :goalId AND isDeleted = 0 AND durationMin IS NOT NULL")
    suspend fun getFocusSessionsForGoal(goalId: String): List<TrackerSession>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoalProgress(entries: List<GoalProgress>)

    @Query("SELECT * FROM passport")
    suspend fun getAllPassportStamps(): List<PassportStamp>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPassportStamps(stamps: List<PassportStamp>)

    @Query("SELECT * FROM journey_notes")
    suspend fun getAllJourneyNotes(): List<JourneyNote>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJourneyNotes(notes: List<JourneyNote>)
}
