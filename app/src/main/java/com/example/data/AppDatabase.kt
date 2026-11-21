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
        SyncQueueItem::class,
        ExerciseCustomNote::class
    ],
    version = 9,
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

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "syncwell_db"
                )
                    .addMigrations(MIGRATION_7_8, MIGRATION_8_9)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
