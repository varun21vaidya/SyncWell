package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

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
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun syncWellDao(): SyncWellDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "syncwell_db"
                )
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                            super.onCreate(db)
                            applicationScope.launch {
                                getDatabase(context).syncWellDao().let { dao ->
                                    DatabaseSeeder.seedDatabase(dao, forceUpdate = true)
                                }
                            }
                        }
                    })
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
