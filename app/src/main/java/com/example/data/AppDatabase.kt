package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        AppSettings::class,
        NotificationSettings::class,
        Reminder::class,
        VacationDay::class,
        RoutineTemplate::class,
        RoutineTemplateItem::class,
        DailyRoutine::class,
        DailyRoutineItem::class,
        WorkoutTemplate::class,
        ExerciseLibraryItem::class,
        ExerciseProgressionState::class,
        WorkoutSession::class,
        WorkoutSessionExercise::class,
        WorkoutSetLog::class,
        TrackerSession::class,
        SupplementLog::class,
        WaterLog::class,
        StepsLog::class,
        WeightLog::class,
        DailyAnalytics::class,
        Todo::class,
        GoalCompletion::class,
        CardioEntry::class,
        SyncQueueItem::class,
        ExerciseCustomNote::class,
        JourneyState::class,
        Goal::class,
        GoalProgress::class,
        PassportStamp::class,
        JourneyNote::class
    ],
    version = 13,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun syncWellDao(): SyncWellDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE routine_templates ADD COLUMN repeatType TEXT NOT NULL DEFAULT 'WEEKLY'")
                db.execSQL("ALTER TABLE routine_templates ADD COLUMN daysOfWeek TEXT NOT NULL DEFAULT '[1,2,3,4,5,6,0]'")
                db.execSQL("ALTER TABLE routine_templates ADD COLUMN onceDate TEXT")
                db.execSQL("ALTER TABLE routine_templates ADD COLUMN isStarter INTEGER NOT NULL DEFAULT 1")
                db.execSQL("UPDATE routine_templates SET daysOfWeek = '[1,2,3,4,5]' WHERE dayType = 'WEEKDAY'")
                db.execSQL("UPDATE routine_templates SET daysOfWeek = '[6]' WHERE dayType = 'SATURDAY'")
                db.execSQL("UPDATE routine_templates SET daysOfWeek = '[0]' WHERE dayType = 'SUNDAY'")
                db.execSQL("ALTER TABLE app_settings ADD COLUMN wakeTime TEXT NOT NULL DEFAULT '07:00'")
                db.execSQL("ALTER TABLE app_settings ADD COLUMN sleepTime TEXT NOT NULL DEFAULT '23:00'")
                db.execSQL("ALTER TABLE app_settings ADD COLUMN workoutDays TEXT NOT NULL DEFAULT '[1,2,3,4,5]'")
                db.execSQL("ALTER TABLE app_settings ADD COLUMN onboardingAnswered INTEGER NOT NULL DEFAULT 1")
            }
        }

        internal val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE daily_routine_items ADD COLUMN proteinAnswer TEXT")
                // Before this column a ticked meal counted as protein met, so keep those days as they were.
                db.execSQL("UPDATE daily_routine_items SET proteinAnswer = 'YES' WHERE category = 'DIET' AND isDone = 1")
                db.execSQL("ALTER TABLE app_settings ADD COLUMN vacationSince TEXT")
                db.execSQL("ALTER TABLE app_settings ADD COLUMN weeklyGymTarget INTEGER NOT NULL DEFAULT 3")
                db.execSQL("ALTER TABLE app_settings ADD COLUMN restTimerEnabled INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE app_settings ADD COLUMN restSeconds INTEGER NOT NULL DEFAULT 90")
                db.execSQL("ALTER TABLE workout_sessions ADD COLUMN activeSeconds INTEGER NOT NULL DEFAULT 0")
            }
        }

        internal val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE todos ADD COLUMN weeklyTarget INTEGER")
                db.execSQL("CREATE TABLE IF NOT EXISTS goal_completions (id TEXT NOT NULL, todoId TEXT NOT NULL, title TEXT NOT NULL, goalType TEXT NOT NULL, completedOn TEXT NOT NULL, PRIMARY KEY(id))")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_goal_completions_todoId_completedOn ON goal_completions (todoId, completedOn)")
                // goals ticked before this update get a history row for the day they were ticked
                db.execSQL("INSERT OR IGNORE INTO goal_completions (id, todoId, title, goalType, completedOn) SELECT 'mig-' || id, id, title, goalType, substr(updatedAt, 1, 10) FROM todos WHERE isCompleted = 1 AND isDeleted = 0")
            }
        }

        internal val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE daily_routine_items ADD COLUMN templateItemId TEXT")
                db.execSQL("ALTER TABLE vacation_days ADD COLUMN kind TEXT NOT NULL DEFAULT 'VACATION'")
                // best effort: link existing days to the template item with the same title and category, so habit streaks have history
                db.execSQL("UPDATE daily_routine_items SET templateItemId = (SELECT t.id FROM routine_template_items t WHERE t.title = daily_routine_items.title AND t.category = daily_routine_items.category ORDER BY t.id LIMIT 1)")
            }
        }

        internal val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS cardio_entries (id TEXT NOT NULL, sessionId TEXT NOT NULL, type TEXT NOT NULL, minutes INTEGER NOT NULL, distanceKm REAL, inclinePct REAL, effort TEXT NOT NULL, PRIMARY KEY(id))")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_cardio_entries_sessionId ON cardio_entries (sessionId)")
            }
        }

        internal val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tracker_sessions ADD COLUMN goalId TEXT")
                db.execSQL("CREATE TABLE IF NOT EXISTS journey_state (id INTEGER NOT NULL, enabled INTEGER NOT NULL, characterId TEXT NOT NULL, regionIndex INTEGER NOT NULL, stepInRegion INTEGER NOT NULL, furthestRegion INTEGER NOT NULL, freezeUsedWeekStart TEXT, lastProcessedDate TEXT, ambitionText TEXT, identityText TEXT, ambitionStart TEXT, targetDate TEXT, bigRewardText TEXT, bigRewardUnlockedAt TEXT, PRIMARY KEY(id))")
                db.execSQL("CREATE TABLE IF NOT EXISTS goals (id TEXT NOT NULL, title TEXT NOT NULL, kind TEXT NOT NULL, mandatory INTEGER NOT NULL, measure TEXT NOT NULL, target REAL NOT NULL, targetLabel TEXT, focusLinked INTEGER NOT NULL, windowStart TEXT NOT NULL, windowEnd TEXT NOT NULL, status TEXT NOT NULL, regionIndex INTEGER, rewardText TEXT, rewardClaimedAt TEXT, planText TEXT, planTime TEXT, ifThenText TEXT, PRIMARY KEY(id))")
                db.execSQL("CREATE TABLE IF NOT EXISTS goal_progress (id TEXT NOT NULL, goalId TEXT NOT NULL, date TEXT NOT NULL, amount REAL NOT NULL, note TEXT, PRIMARY KEY(id), FOREIGN KEY(goalId) REFERENCES goals(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_goal_progress_goalId ON goal_progress (goalId)")
                db.execSQL("CREATE TABLE IF NOT EXISTS passport (id TEXT NOT NULL, kind TEXT NOT NULL, regionIndex INTEGER NOT NULL, earnedAt TEXT NOT NULL, label TEXT NOT NULL, PRIMARY KEY(id))")
                db.execSQL("CREATE TABLE IF NOT EXISTS journey_notes (id TEXT NOT NULL, date TEXT NOT NULL, kind TEXT NOT NULL, text TEXT NOT NULL, PRIMARY KEY(id))")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "syncwell_db"
                )
                    .addMigrations(MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
