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

    @Query("SELECT * FROM vacation_days WHERE date = :date LIMIT 1")
    suspend fun getVacationDayByDate(date: String): VacationDay?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVacationDay(day: VacationDay)

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

    @Query("SELECT * FROM daily_routine_items WHERE dailyRoutineId = :dailyRoutineId ORDER BY orderIndex ASC")
    fun getDailyRoutineItemsFlow(dailyRoutineId: String): Flow<List<DailyRoutineItem>>

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
    @Query("SELECT * FROM supplement_logs WHERE dateKey = :dateKey")
    fun getSupplementLogs(dateKey: String): Flow<List<SupplementLog>>

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

    @Query("SELECT * FROM daily_analytics ORDER BY dateKey DESC LIMIT 7")
    fun getWeeklyAnalytics(): Flow<List<DailyAnalytics>>

    @Query("SELECT * FROM daily_analytics ORDER BY dateKey DESC LIMIT 30")
    fun getMonthlyAnalytics(): Flow<List<DailyAnalytics>>

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
    @Query("UPDATE todos SET isCompleted = 0, updatedAt = :nowIso WHERE goalType = 'SHORT_TERM' AND isCompleted = 1 AND updatedAt < :cutoffIso")
    suspend fun resetCompletedShortTermGoalsBefore(cutoffIso: String, nowIso: String)

    @Query("UPDATE todos SET isCompleted = 0, updatedAt = :nowIso WHERE goalType = 'LONG_TERM' AND isCompleted = 1 AND updatedAt < :cutoffIso")
    suspend fun resetCompletedLongTermGoalsBefore(cutoffIso: String, nowIso: String)

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
}
