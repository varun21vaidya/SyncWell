package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey val id: String,
    val firebaseUid: String?,
    val defaultWaterMl: Int = 4000,
    val themeMode: String = "light",
    val backupEnabled: Boolean = false,
    val vacationPauseEnabled: Boolean = false,
    val workoutLocationMode: String = "GYM", // 'GYM' or 'HOME'
    val createdAt: String,
    val updatedAt: String
)

@Entity(tableName = "notification_settings")
data class NotificationSettings(
    @PrimaryKey val id: String,
    val waterRemindersEnabled: Boolean = true,
    val waterReminderIntervalMin: Int = 120,
    val medicineRemindersEnabled: Boolean = true,
    val createdAt: String,
    val updatedAt: String
)

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey val id: String,
    val title: String,
    val category: String,
    val scheduledTime: String, // HH:MM
    val daysOfWeek: String = "[1,2,3,4,5,6,0]", // Comma separated, e.g., "1,2,3,4,5,6,0"
    val notificationId: String?,
    val isDeleted: Boolean = false,
    val createdAt: String
)

@Entity(
    tableName = "vacation_days",
    indices = [Index(value = ["date"], unique = true)]
)
data class VacationDay(
    @PrimaryKey val id: String,
    val date: String, // YYYY-MM-DD
    val isDeleted: Boolean = false,
    val createdAt: String
)

@Entity(tableName = "routine_templates")
data class RoutineTemplate(
    @PrimaryKey val id: String,
    val name: String,
    val dayType: String, // WEEKDAY, SATURDAY, SUNDAY
    val isActive: Boolean = true
)

@Entity(
    tableName = "routine_template_items",
    foreignKeys = [
        ForeignKey(
            entity = RoutineTemplate::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class RoutineTemplateItem(
    @PrimaryKey val id: String,
    val templateId: String,
    val title: String,
    val category: String,
    val orderIndex: Int,
    val scheduledTime: String?,
    val isRequired: Boolean = true
)

@Entity(
    tableName = "daily_routines",
    indices = [Index(value = ["date"], unique = true)]
)
data class DailyRoutine(
    @PrimaryKey val id: String,
    val date: String, // YYYY-MM-DD
    val templateId: String?,
    val completionPct: Double = 0.0,
    val isVacation: Boolean = false
)

@Entity(
    tableName = "daily_routine_items",
    foreignKeys = [
        ForeignKey(
            entity = DailyRoutine::class,
            parentColumns = ["id"],
            childColumns = ["dailyRoutineId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["dailyRoutineId", "isDone"])]
)
data class DailyRoutineItem(
    @PrimaryKey val id: String,
    val dailyRoutineId: String,
    val title: String,
    val category: String,
    val orderIndex: Int,
    val scheduledTime: String?,
    val isDone: Boolean = false
)

@Entity(tableName = "workout_templates")
data class WorkoutTemplate(
    @PrimaryKey val id: String,
    val name: String,
    val dayType: String // MONDAY, TUESDAY, etc.
)

@Entity(
    tableName = "exercise_library",
    indices = [Index(value = ["name"], unique = true)]
)
data class ExerciseLibraryItem(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val defaultSets: Int,
    val defaultRepsMin: Int,
    val defaultRepsMax: Int,
    val defaultRestSec: Int,
    val homeAlternativeId: String?, // references another exercise id in library
    val alternatingGroupId: String?,
    val environmentType: String? = null // GYM (null) or HOME
)

@Entity(
    tableName = "exercise_progression_states",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseLibraryItem::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ExerciseProgressionState(
    @PrimaryKey val exerciseId: String,
    val suggestedWeightKg: Double,
    val suggestedReps: Int,
    val lastUpdatedAt: String
)

@Entity(tableName = "workout_sessions")
data class WorkoutSession(
    @PrimaryKey val id: String,
    val date: String, // YYYY-MM-DD
    val dayType: String,
    val environment: String, // GYM, HOME
    val startedAt: String,
    val endedAt: String?,
    val isDeleted: Boolean = false
)

@Entity(
    tableName = "workout_session_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ExerciseLibraryItem::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"]
        )
    ]
)
data class WorkoutSessionExercise(
    @PrimaryKey val id: String,
    val sessionId: String,
    val exerciseId: String,
    val orderIndex: Int,
    val targetSets: Int,
    val targetRepsMax: Int,
    val targetWeightKg: Double?
)

@Entity(
    tableName = "workout_set_logs",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionExercise::class,
            parentColumns = ["id"],
            childColumns = ["sessionExerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["sessionExerciseId"])]
)
data class WorkoutSetLog(
    @PrimaryKey val id: String,
    val sessionExerciseId: String,
    val setIndex: Int,
    val weightKg: Double,
    val reps: Int,
    val isCompleted: Boolean = true
)

@Entity(
    tableName = "tracker_sessions",
    indices = [Index(value = ["activityType", "startTime"])]
)
data class TrackerSession(
    @PrimaryKey val id: String,
    val activityType: String, // 'STUDY' | 'WORK' | 'TIMEPASS' | 'SPORT' | 'SLEEP'
    val source: String = "TIMER", // TIMER, MANUAL
    val startTime: String,
    val endTime: String?,
    val durationMin: Int?,
    val dateKey: String, // YYYY-MM-DD
    val isDeleted: Boolean = false
)

@Entity(tableName = "supplement_logs")
data class SupplementLog(
    @PrimaryKey val id: String,
    val supplementName: String,
    val loggedAt: String,
    val lateLogWarningShown: Boolean = false,
    val dateKey: String // YYYY-MM-DD
)

@Entity(
    tableName = "water_logs",
    indices = [Index(value = ["dateKey"])]
)
data class WaterLog(
    @PrimaryKey val id: String,
    val amountMl: Int,
    val loggedAt: String,
    val dateKey: String,
    val isDeleted: Boolean = false
)

@Entity(
    tableName = "steps_logs",
    indices = [Index(value = ["dateKey"], unique = true)]
)
data class StepsLog(
    @PrimaryKey val id: String,
    val dateKey: String,
    val stepCount: Int,
    val source: String, // MANUAL, HEALTH_CONNECT
    val updatedAt: String
)

@Entity(
    tableName = "weight_logs",
    indices = [Index(value = ["dateKey"], unique = true)]
)
data class WeightLog(
    @PrimaryKey val id: String,
    val dateKey: String,
    val weightKg: Double,
    val isDeleted: Boolean = false,
    val loggedAt: String
)

@Entity(tableName = "daily_analytics")
data class DailyAnalytics(
    @PrimaryKey val dateKey: String,
    val routineCompletionPct: Double = 0.0,
    val totalStudyMinutes: Int = 0,
    val workMin: Int = 0,
    val timepassMin: Int = 0,
    val totalWorkoutMinutes: Int = 0,
    val totalWaterMl: Int = 0,
    val totalSteps: Int = 0,
    val sleepMin: Int = 0,
    val weightKg: Double?,
    val streakValue: Int = 0,
    val gymStreakValue: Int = 0,
    val dietStreakValue: Int = 0,
    val dietProteinPoints: Int = 0, // 0-4
    val isGymCompleted: Boolean = false,
    val isDietCompleted: Boolean = false,
    val isVacation: Boolean = false,
    val updatedAt: String
)

@Entity(tableName = "todos")
data class Todo(
    @PrimaryKey val id: String,
    val title: String,
    val priority: String = "MEDIUM",   // HIGH, MEDIUM, LOW
    val goalType: String = "SHORT_TERM", // SHORT_TERM | LONG_TERM  — FEATURE 11
    val isCompleted: Boolean = false,
    val isDeleted: Boolean = false,
    val createdAt: String,
    val updatedAt: String
)

@Entity(tableName = "sync_queue")
data class SyncQueueItem(
    @PrimaryKey val id: String,
    val entityType: String,
    val entityId: String,
    val operation: String,
    val payloadJson: String,
    val status: String = "PENDING", // PENDING, SYNCED, FAILED
    val retryCount: Int = 0,
    val lastAttemptAt: String?,
    val errorMessage: String?,
    val createdAt: String
)

// FEATURE 10: Custom exercise notes
@Entity(
    tableName = "exercise_custom_notes",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseLibraryItem::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ExerciseCustomNote(
    @PrimaryKey val exerciseId: String,
    val customNote: String,
    val updatedAt: String
)
