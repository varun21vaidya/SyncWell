package com.example.service

import android.content.Context
import android.util.Log
import com.example.data.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupSummary(
    val todosCount: Int,
    val weightsCount: Int,
    val workoutSessionsCount: Int,
    val workoutSessionExercisesCount: Int,
    val workoutSetLogsCount: Int,
    val exerciseProgressionsCount: Int,
    val trackerSessionsCount: Int,
    val supplementLogsCount: Int,
    val waterLogsCount: Int,
    val stepsLogsCount: Int,
    val dailyAnalyticsCount: Int,
    val exerciseNotesCount: Int,
    val dailyRoutinesCount: Int,
    val dailyRoutineItemsCount: Int,
    val remindersCount: Int,
    val vacationDaysCount: Int,
    val timestampIso: String
)

object CloudBackupService {
    private const val TAG = "CloudBackupService"
    private const val PREFS_SESSION = "syncwell_session_prefs"

    suspend fun hasCloudBackup(context: Context, userEmail: String): Boolean =
        DriveBackupClient.hasBackup(DriveBackupClient.accessToken(context, userEmail))

    suspend fun performBackup(context: Context, dao: SyncWellDao, userEmail: String): Result<BackupSummary> {
        return try {
            val root = JSONObject()
            root.put("version", 3)
            val iso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
            root.put("backupTimestamp", iso)

            // 1. Todos
            val todos = dao.getAllTodos()
            val todosArr = JSONArray()
            todos.forEach { t ->
                todosArr.put(JSONObject().apply {
                    put("id", t.id)
                    put("title", t.title)
                    put("priority", t.priority)
                    put("goalType", t.goalType)
                    put("isCompleted", t.isCompleted)
                    put("isDeleted", t.isDeleted)
                    put("createdAt", t.createdAt)
                    put("updatedAt", t.updatedAt)
                })
            }
            root.put("todos", todosArr)

            // 2. Weight Logs
            val weights = dao.getAllWeightLogs()
            val weightsArr = JSONArray()
            weights.forEach { w ->
                weightsArr.put(JSONObject().apply {
                    put("id", w.id)
                    put("dateKey", w.dateKey)
                    put("weightKg", w.weightKg)
                    put("isDeleted", w.isDeleted)
                    put("loggedAt", w.loggedAt)
                })
            }
            root.put("weights", weightsArr)

            // 3. Workout Sessions
            val workoutSessions = dao.getAllWorkoutSessions()
            val workoutSessionsArr = JSONArray()
            workoutSessions.forEach { ws ->
                workoutSessionsArr.put(JSONObject().apply {
                    put("id", ws.id)
                    put("date", ws.date)
                    put("dayType", ws.dayType)
                    put("environment", ws.environment)
                    put("startedAt", ws.startedAt)
                    putOpt("endedAt", ws.endedAt)
                    put("isDeleted", ws.isDeleted)
                })
            }
            root.put("workoutSessions", workoutSessionsArr)

            // 4. Workout Session Exercises
            val workoutSessionExercises = dao.getAllWorkoutSessionExercises()
            val wseArr = JSONArray()
            workoutSessionExercises.forEach { wse ->
                wseArr.put(JSONObject().apply {
                    put("id", wse.id)
                    put("sessionId", wse.sessionId)
                    put("exerciseId", wse.exerciseId)
                    put("orderIndex", wse.orderIndex)
                    put("targetSets", wse.targetSets)
                    put("targetRepsMax", wse.targetRepsMax)
                    putOpt("targetWeightKg", wse.targetWeightKg)
                })
            }
            root.put("workoutSessionExercises", wseArr)

            // 5. Workout Set Logs
            val workoutSetLogs = dao.getAllWorkoutSetLogs()
            val wslArr = JSONArray()
            workoutSetLogs.forEach { sl ->
                wslArr.put(JSONObject().apply {
                    put("id", sl.id)
                    put("sessionExerciseId", sl.sessionExerciseId)
                    put("setIndex", sl.setIndex)
                    put("weightKg", sl.weightKg)
                    put("reps", sl.reps)
                    put("isCompleted", sl.isCompleted)
                })
            }
            root.put("workoutSetLogs", wslArr)

            // 6. Exercise Progression States
            val exerciseProgressions = dao.getAllExerciseProgressions()
            val epArr = JSONArray()
            exerciseProgressions.forEach { ep ->
                epArr.put(JSONObject().apply {
                    put("exerciseId", ep.exerciseId)
                    put("suggestedWeightKg", ep.suggestedWeightKg)
                    put("suggestedReps", ep.suggestedReps)
                    put("lastUpdatedAt", ep.lastUpdatedAt)
                })
            }
            root.put("exerciseProgressions", epArr)

            // 7. Tracker Sessions (Focus)
            val trackerSessions = dao.getAllTrackerSessions()
            val tsArr = JSONArray()
            trackerSessions.forEach { ts ->
                tsArr.put(JSONObject().apply {
                    put("id", ts.id)
                    put("activityType", ts.activityType)
                    put("source", ts.source)
                    put("startTime", ts.startTime)
                    putOpt("endTime", ts.endTime)
                    putOpt("durationMin", ts.durationMin)
                    put("dateKey", ts.dateKey)
                    put("isDeleted", ts.isDeleted)
                })
            }
            root.put("trackerSessions", tsArr)

            // 8. Supplement Logs
            val supplementLogs = dao.getAllSupplementLogs()
            val supArr = JSONArray()
            supplementLogs.forEach { sl ->
                supArr.put(JSONObject().apply {
                    put("id", sl.id)
                    put("supplementName", sl.supplementName)
                    put("loggedAt", sl.loggedAt)
                    put("lateLogWarningShown", sl.lateLogWarningShown)
                    put("dateKey", sl.dateKey)
                })
            }
            root.put("supplementLogs", supArr)

            // 9. Water Logs
            val waterLogs = dao.getAllWaterLogs()
            val wlArr = JSONArray()
            waterLogs.forEach { wl ->
                wlArr.put(JSONObject().apply {
                    put("id", wl.id)
                    put("amountMl", wl.amountMl)
                    put("loggedAt", wl.loggedAt)
                    put("dateKey", wl.dateKey)
                    put("isDeleted", wl.isDeleted)
                })
            }
            root.put("waterLogs", wlArr)

            // 10. Steps Logs
            val stepsLogs = dao.getAllStepsLogs()
            val stArr = JSONArray()
            stepsLogs.forEach { st ->
                stArr.put(JSONObject().apply {
                    put("id", st.id)
                    put("dateKey", st.dateKey)
                    put("stepCount", st.stepCount)
                    put("source", st.source)
                    put("updatedAt", st.updatedAt)
                })
            }
            root.put("stepsLogs", stArr)

            // 11. Daily Analytics
            val dailyAnalytics = dao.getAllDailyAnalytics()
            val daArr = JSONArray()
            dailyAnalytics.forEach { da ->
                daArr.put(JSONObject().apply {
                    put("dateKey", da.dateKey)
                    put("routineCompletionPct", da.routineCompletionPct)
                    put("totalStudyMinutes", da.totalStudyMinutes)
                    put("workMin", da.workMin)
                    put("timepassMin", da.timepassMin)
                    put("totalWorkoutMinutes", da.totalWorkoutMinutes)
                    put("totalWaterMl", da.totalWaterMl)
                    put("totalSteps", da.totalSteps)
                    put("sleepMin", da.sleepMin)
                    putOpt("weightKg", da.weightKg)
                    put("streakValue", da.streakValue)
                    put("gymStreakValue", da.gymStreakValue)
                    put("dietStreakValue", da.dietStreakValue)
                    put("dietProteinPoints", da.dietProteinPoints)
                    put("isGymCompleted", da.isGymCompleted)
                    put("isDietCompleted", da.isDietCompleted)
                    put("isVacation", da.isVacation)
                    put("updatedAt", da.updatedAt)
                })
            }
            root.put("dailyAnalytics", daArr)

            // 12. Exercise Custom Notes
            val notes = dao.getAllExerciseCustomNotes()
            val notesArr = JSONArray()
            notes.forEach { n ->
                notesArr.put(JSONObject().apply {
                    put("exerciseId", n.exerciseId)
                    put("customNote", n.customNote)
                    put("updatedAt", n.updatedAt)
                })
            }
            root.put("exerciseCustomNotes", notesArr)

            // 13. Daily Routines
            val routines = dao.getAllDailyRoutines()
            val drArr = JSONArray()
            routines.forEach { r ->
                drArr.put(JSONObject().apply {
                    put("id", r.id)
                    put("date", r.date)
                    putOpt("templateId", r.templateId)
                    put("completionPct", r.completionPct)
                    put("isVacation", r.isVacation)
                })
            }
            root.put("dailyRoutines", drArr)

            // 14. Daily Routine Items
            val routineItems = dao.getAllDailyRoutineItems()
            val driArr = JSONArray()
            routineItems.forEach { ri ->
                driArr.put(JSONObject().apply {
                    put("id", ri.id)
                    put("dailyRoutineId", ri.dailyRoutineId)
                    put("title", ri.title)
                    put("category", ri.category)
                    put("orderIndex", ri.orderIndex)
                    putOpt("scheduledTime", ri.scheduledTime)
                    put("isDone", ri.isDone)
                })
            }
            root.put("dailyRoutineItems", driArr)

            // 15. Reminders
            val reminders = dao.getAllReminders()
            val remArr = JSONArray()
            reminders.forEach { rm ->
                remArr.put(JSONObject().apply {
                    put("id", rm.id)
                    put("title", rm.title)
                    put("category", rm.category)
                    put("scheduledTime", rm.scheduledTime)
                    put("daysOfWeek", rm.daysOfWeek)
                    putOpt("notificationId", rm.notificationId)
                    put("isDeleted", rm.isDeleted)
                    put("createdAt", rm.createdAt)
                })
            }
            root.put("reminders", remArr)

            // 16. Vacation Days
            val vacationDays = dao.getAllVacationDays()
            val vdArr = JSONArray()
            vacationDays.forEach { vd ->
                vdArr.put(JSONObject().apply {
                    put("id", vd.id)
                    put("date", vd.date)
                    put("isDeleted", vd.isDeleted)
                    put("createdAt", vd.createdAt)
                })
            }
            root.put("vacationDays", vdArr)

            // 17. App settings
            dao.getAppSettingsDirectly()?.let { st ->
                root.put("appSettings", JSONObject().apply {
                    put("id", st.id)
                    put("firebaseUid", st.firebaseUid ?: JSONObject.NULL)
                    put("defaultWaterMl", st.defaultWaterMl)
                    put("targetWeightKg", st.targetWeightKg)
                    put("themeMode", st.themeMode)
                    put("backupEnabled", st.backupEnabled)
                    put("vacationPauseEnabled", st.vacationPauseEnabled)
                    put("workoutLocationMode", st.workoutLocationMode)
                    put("createdAt", st.createdAt)
                    put("updatedAt", st.updatedAt)
                    put("wakeTime", st.wakeTime)
                    put("sleepTime", st.sleepTime)
                    put("workoutDays", st.workoutDays)
                })
            }

            // 18. Notification settings
            dao.getNotificationSettingsDirectly()?.let { ns ->
                root.put("notificationSettings", JSONObject().apply {
                    put("id", ns.id)
                    put("waterRemindersEnabled", ns.waterRemindersEnabled)
                    put("waterReminderIntervalMin", ns.waterReminderIntervalMin)
                    put("medicineRemindersEnabled", ns.medicineRemindersEnabled)
                    put("createdAt", ns.createdAt)
                    put("updatedAt", ns.updatedAt)
                })
            }

            // 19. Routine templates + items
            root.put("routineTemplates", JSONArray().also { arr ->
                dao.getRoutineTemplates().forEach { t ->
                    arr.put(JSONObject().apply {
                        put("id", t.id)
                        put("name", t.name)
                        put("isActive", t.isActive)
                        put("repeatType", t.repeatType)
                        put("daysOfWeek", t.daysOfWeek)
                        putOpt("onceDate", t.onceDate)
                        put("isStarter", t.isStarter)
                    })
                }
            })
            root.put("routineTemplateItems", JSONArray().also { arr ->
                dao.getAllRoutineTemplateItems().forEach { i ->
                    arr.put(JSONObject().apply {
                        put("id", i.id)
                        put("templateId", i.templateId)
                        put("title", i.title)
                        put("category", i.category)
                        put("orderIndex", i.orderIndex)
                        putOpt("scheduledTime", i.scheduledTime)
                        put("isRequired", i.isRequired)
                    })
                }
            })

            // 20. Custom exercises (built-in ones are re-seeded)
            root.put("customExercises", JSONArray().also { arr ->
                dao.getExerciseLibrary().filter { !it.id.startsWith("ex_") && !it.id.startsWith("home_") }.forEach { e ->
                    arr.put(JSONObject().apply {
                        put("id", e.id)
                        put("name", e.name)
                        put("category", e.category)
                        put("defaultSets", e.defaultSets)
                        put("defaultRepsMin", e.defaultRepsMin)
                        put("defaultRepsMax", e.defaultRepsMax)
                        put("defaultRestSec", e.defaultRestSec)
                        putOpt("homeAlternativeId", e.homeAlternativeId)
                        putOpt("alternatingGroupId", e.alternatingGroupId)
                        putOpt("environmentType", e.environmentType)
                    })
                }
            })

            // Upload to the user's Google Drive (hidden app data folder)
            val token = DriveBackupClient.accessToken(context, userEmail)
            DriveBackupClient.upload(token, root.toString())

            // Update local session timestamp
            val sessionPrefs = context.getSharedPreferences(PREFS_SESSION, Context.MODE_PRIVATE)
            sessionPrefs.edit().putString("last_backup_iso", iso).apply()

            val summary = BackupSummary(
                todosCount = todos.size,
                weightsCount = weights.size,
                workoutSessionsCount = workoutSessions.size,
                workoutSessionExercisesCount = workoutSessionExercises.size,
                workoutSetLogsCount = workoutSetLogs.size,
                exerciseProgressionsCount = exerciseProgressions.size,
                trackerSessionsCount = trackerSessions.size,
                supplementLogsCount = supplementLogs.size,
                waterLogsCount = waterLogs.size,
                stepsLogsCount = stepsLogs.size,
                dailyAnalyticsCount = dailyAnalytics.size,
                exerciseNotesCount = notes.size,
                dailyRoutinesCount = routines.size,
                dailyRoutineItemsCount = routineItems.size,
                remindersCount = reminders.size,
                vacationDaysCount = vacationDays.size,
                timestampIso = iso
            )
            Log.d(TAG, "Backup succeeded for $userEmail: ${summary.workoutSessionsCount} workouts, ${summary.todosCount} todos, ${summary.trackerSessionsCount} tracker sessions")
            Result.success(summary)
        } catch (e: Exception) {
            Log.e(TAG, "Backup failed", e)
            Result.failure(e)
        }
    }

    suspend fun performRestore(context: Context, dao: SyncWellDao, userEmail: String): Result<String> {
        return try {
            val token = DriveBackupClient.accessToken(context, userEmail)
            val jsonString = DriveBackupClient.download(token)
                ?: return Result.failure(IllegalStateException("No cloud backup found for $userEmail"))

            val root = JSONObject(jsonString)

            // 0. Settings, routines and custom exercises (before the data that references them)
            root.optJSONObject("appSettings")?.let { o ->
                val existing = dao.getAppSettingsDirectly()
                dao.insertAppSettings(AppSettings(
                    id = existing?.id ?: o.getString("id"),
                    firebaseUid = if (o.isNull("firebaseUid")) null else o.getString("firebaseUid"),
                    defaultWaterMl = o.optInt("defaultWaterMl", 2500),
                    targetWeightKg = o.optDouble("targetWeightKg", 0.0),
                    themeMode = o.optString("themeMode", "dark"),
                    backupEnabled = o.optBoolean("backupEnabled", false),
                    vacationPauseEnabled = o.optBoolean("vacationPauseEnabled", false),
                    workoutLocationMode = o.optString("workoutLocationMode", "GYM"),
                    createdAt = o.getString("createdAt"),
                    updatedAt = o.getString("updatedAt"),
                    wakeTime = o.optString("wakeTime", "07:00"),
                    sleepTime = o.optString("sleepTime", "23:00"),
                    workoutDays = o.optString("workoutDays", "[1,2,3,4,5]"),
                    onboardingAnswered = true
                ))
            }
            root.optJSONObject("notificationSettings")?.let { o ->
                val existing = dao.getNotificationSettingsDirectly()
                dao.insertNotificationSettings(NotificationSettings(
                    id = existing?.id ?: o.getString("id"),
                    waterRemindersEnabled = o.optBoolean("waterRemindersEnabled", true),
                    waterReminderIntervalMin = o.optInt("waterReminderIntervalMin", 120),
                    medicineRemindersEnabled = o.optBoolean("medicineRemindersEnabled", true),
                    createdAt = o.getString("createdAt"),
                    updatedAt = o.getString("updatedAt")
                ))
            }
            root.optJSONArray("routineTemplates")?.let { arr ->
                // A v3 backup replaces the device's routines (v2 backups have none, so the starters stay)
                dao.clearRoutineTemplates()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    dao.insertRoutineTemplate(RoutineTemplate(
                        id = o.getString("id"), name = o.getString("name"),
                        isActive = o.optBoolean("isActive", true),
                        repeatType = o.optString("repeatType", "WEEKLY"),
                        daysOfWeek = o.optString("daysOfWeek", "[1,2,3,4,5,6,0]"),
                        onceDate = if (o.isNull("onceDate")) null else o.optString("onceDate"),
                        isStarter = o.optBoolean("isStarter", false)
                    ))
                }
            }
            root.optJSONArray("routineTemplateItems")?.let { arr ->
                val list = (0 until arr.length()).map { arr.getJSONObject(it) }.map { o ->
                    RoutineTemplateItem(
                        id = o.getString("id"), templateId = o.getString("templateId"),
                        title = o.getString("title"), category = o.getString("category"),
                        orderIndex = o.getInt("orderIndex"),
                        scheduledTime = if (o.isNull("scheduledTime")) null else o.optString("scheduledTime"),
                        isRequired = o.optBoolean("isRequired", true)
                    )
                }
                if (list.isNotEmpty()) dao.insertRoutineTemplateItems(list)
            }
            root.optJSONArray("customExercises")?.let { arr ->
                val list = (0 until arr.length()).map { arr.getJSONObject(it) }.map { o ->
                    ExerciseLibraryItem(
                        id = o.getString("id"), name = o.getString("name"), category = o.getString("category"),
                        defaultSets = o.getInt("defaultSets"), defaultRepsMin = o.getInt("defaultRepsMin"),
                        defaultRepsMax = o.getInt("defaultRepsMax"), defaultRestSec = o.getInt("defaultRestSec"),
                        homeAlternativeId = if (o.isNull("homeAlternativeId")) null else o.optString("homeAlternativeId"),
                        alternatingGroupId = if (o.isNull("alternatingGroupId")) null else o.optString("alternatingGroupId"),
                        environmentType = if (o.isNull("environmentType")) null else o.optString("environmentType")
                    )
                }
                if (list.isNotEmpty()) dao.insertExerciseLibrary(list)
            }

            // 1. Todos
            root.optJSONArray("todos")?.let { arr ->
                val list = mutableListOf<Todo>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(Todo(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        priority = obj.optString("priority", "MEDIUM"),
                        goalType = obj.optString("goalType", "SHORT_TERM"),
                        isCompleted = obj.optBoolean("isCompleted", false),
                        isDeleted = obj.optBoolean("isDeleted", false),
                        createdAt = obj.getString("createdAt"),
                        updatedAt = obj.optString("updatedAt", obj.getString("createdAt"))
                    ))
                }
                if (list.isNotEmpty()) dao.insertTodos(list)
            }

            // 2. Weights
            root.optJSONArray("weights")?.let { arr ->
                val list = mutableListOf<WeightLog>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(WeightLog(
                        id = obj.getString("id"),
                        dateKey = obj.getString("dateKey"),
                        weightKg = obj.getDouble("weightKg"),
                        isDeleted = obj.optBoolean("isDeleted", false),
                        loggedAt = obj.getString("loggedAt")
                    ))
                }
                if (list.isNotEmpty()) dao.insertWeightLogs(list)
            }

            // 3. Workout Sessions (Must be inserted before exercises & set logs for FK constraints)
            root.optJSONArray("workoutSessions")?.let { arr ->
                val list = mutableListOf<WorkoutSession>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(WorkoutSession(
                        id = obj.getString("id"),
                        date = obj.getString("date"),
                        dayType = obj.getString("dayType"),
                        environment = obj.optString("environment", "GYM"),
                        startedAt = obj.getString("startedAt"),
                        endedAt = if (obj.has("endedAt") && !obj.isNull("endedAt")) obj.getString("endedAt") else null,
                        isDeleted = obj.optBoolean("isDeleted", false)
                    ))
                }
                if (list.isNotEmpty()) dao.insertWorkoutSessions(list)
            }

            // 4. Workout Session Exercises
            root.optJSONArray("workoutSessionExercises")?.let { arr ->
                val list = mutableListOf<WorkoutSessionExercise>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(WorkoutSessionExercise(
                        id = obj.getString("id"),
                        sessionId = obj.getString("sessionId"),
                        exerciseId = obj.getString("exerciseId"),
                        orderIndex = obj.getInt("orderIndex"),
                        targetSets = obj.getInt("targetSets"),
                        targetRepsMax = obj.getInt("targetRepsMax"),
                        targetWeightKg = if (obj.has("targetWeightKg") && !obj.isNull("targetWeightKg")) obj.getDouble("targetWeightKg") else null
                    ))
                }
                if (list.isNotEmpty()) dao.insertWorkoutSessionExercises(list)
            }

            // 5. Workout Set Logs
            root.optJSONArray("workoutSetLogs")?.let { arr ->
                val list = mutableListOf<WorkoutSetLog>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(WorkoutSetLog(
                        id = obj.getString("id"),
                        sessionExerciseId = obj.getString("sessionExerciseId"),
                        setIndex = obj.getInt("setIndex"),
                        weightKg = obj.getDouble("weightKg"),
                        reps = obj.getInt("reps"),
                        isCompleted = obj.optBoolean("isCompleted", true)
                    ))
                }
                if (list.isNotEmpty()) dao.insertWorkoutSetLogs(list)
            }

            // 6. Exercise Progression States
            root.optJSONArray("exerciseProgressions")?.let { arr ->
                val list = mutableListOf<ExerciseProgressionState>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(ExerciseProgressionState(
                        exerciseId = obj.getString("exerciseId"),
                        suggestedWeightKg = obj.getDouble("suggestedWeightKg"),
                        suggestedReps = obj.getInt("suggestedReps"),
                        lastUpdatedAt = obj.getString("lastUpdatedAt")
                    ))
                }
                if (list.isNotEmpty()) dao.insertExerciseProgressions(list)
            }

            // 7. Tracker Sessions (Focus)
            root.optJSONArray("trackerSessions")?.let { arr ->
                val list = mutableListOf<TrackerSession>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(TrackerSession(
                        id = obj.getString("id"),
                        activityType = obj.getString("activityType"),
                        source = obj.optString("source", "TIMER"),
                        startTime = obj.getString("startTime"),
                        endTime = if (obj.has("endTime") && !obj.isNull("endTime")) obj.getString("endTime") else null,
                        durationMin = if (obj.has("durationMin") && !obj.isNull("durationMin")) obj.getInt("durationMin") else null,
                        dateKey = obj.getString("dateKey"),
                        isDeleted = obj.optBoolean("isDeleted", false)
                    ))
                }
                if (list.isNotEmpty()) dao.insertTrackerSessions(list)
            }

            // 8. Supplement Logs
            root.optJSONArray("supplementLogs")?.let { arr ->
                val list = mutableListOf<SupplementLog>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(SupplementLog(
                        id = obj.getString("id"),
                        supplementName = obj.getString("supplementName"),
                        loggedAt = obj.getString("loggedAt"),
                        lateLogWarningShown = obj.optBoolean("lateLogWarningShown", false),
                        dateKey = obj.getString("dateKey")
                    ))
                }
                if (list.isNotEmpty()) dao.insertSupplementLogs(list)
            }

            // 9. Water Logs
            root.optJSONArray("waterLogs")?.let { arr ->
                val list = mutableListOf<WaterLog>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(WaterLog(
                        id = obj.getString("id"),
                        amountMl = obj.getInt("amountMl"),
                        loggedAt = obj.getString("loggedAt"),
                        dateKey = obj.getString("dateKey"),
                        isDeleted = obj.optBoolean("isDeleted", false)
                    ))
                }
                if (list.isNotEmpty()) dao.insertWaterLogs(list)
            }

            // 10. Steps Logs
            root.optJSONArray("stepsLogs")?.let { arr ->
                val list = mutableListOf<StepsLog>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(StepsLog(
                        id = obj.getString("id"),
                        dateKey = obj.getString("dateKey"),
                        stepCount = obj.getInt("stepCount"),
                        source = obj.optString("source", "MANUAL"),
                        updatedAt = obj.getString("updatedAt")
                    ))
                }
                if (list.isNotEmpty()) dao.insertStepsLogs(list)
            }

            // 11. Daily Analytics
            root.optJSONArray("dailyAnalytics")?.let { arr ->
                val list = mutableListOf<DailyAnalytics>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(DailyAnalytics(
                        dateKey = obj.getString("dateKey"),
                        routineCompletionPct = obj.optDouble("routineCompletionPct", 0.0),
                        totalStudyMinutes = obj.optInt("totalStudyMinutes", 0),
                        workMin = obj.optInt("workMin", 0),
                        timepassMin = obj.optInt("timepassMin", 0),
                        totalWorkoutMinutes = obj.optInt("totalWorkoutMinutes", 0),
                        totalWaterMl = obj.optInt("totalWaterMl", 0),
                        totalSteps = obj.optInt("totalSteps", 0),
                        sleepMin = obj.optInt("sleepMin", 0),
                        weightKg = if (obj.has("weightKg") && !obj.isNull("weightKg")) obj.getDouble("weightKg") else null,
                        streakValue = obj.optInt("streakValue", 0),
                        gymStreakValue = obj.optInt("gymStreakValue", 0),
                        dietStreakValue = obj.optInt("dietStreakValue", 0),
                        dietProteinPoints = obj.optInt("dietProteinPoints", 0),
                        isGymCompleted = obj.optBoolean("isGymCompleted", false),
                        isDietCompleted = obj.optBoolean("isDietCompleted", false),
                        isVacation = obj.optBoolean("isVacation", false),
                        updatedAt = obj.optString("updatedAt", "")
                    ))
                }
                if (list.isNotEmpty()) dao.insertDailyAnalyticsList(list)
            }

            // 12. Exercise Custom Notes
            root.optJSONArray("exerciseCustomNotes")?.let { arr ->
                val list = mutableListOf<ExerciseCustomNote>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(ExerciseCustomNote(
                        exerciseId = obj.getString("exerciseId"),
                        customNote = obj.getString("customNote"),
                        updatedAt = obj.getString("updatedAt")
                    ))
                }
                if (list.isNotEmpty()) dao.insertExerciseCustomNotes(list)
            }

            // 13. Daily Routines
            root.optJSONArray("dailyRoutines")?.let { arr ->
                val list = mutableListOf<DailyRoutine>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(DailyRoutine(
                        id = obj.getString("id"),
                        date = obj.getString("date"),
                        templateId = if (obj.has("templateId") && !obj.isNull("templateId")) obj.getString("templateId") else null,
                        completionPct = obj.optDouble("completionPct", 0.0),
                        isVacation = obj.optBoolean("isVacation", false)
                    ))
                }
                if (list.isNotEmpty()) dao.insertDailyRoutines(list)
            }

            // 14. Daily Routine Items
            root.optJSONArray("dailyRoutineItems")?.let { arr ->
                val list = mutableListOf<DailyRoutineItem>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(DailyRoutineItem(
                        id = obj.getString("id"),
                        dailyRoutineId = obj.getString("dailyRoutineId"),
                        title = obj.getString("title"),
                        category = obj.getString("category"),
                        orderIndex = obj.getInt("orderIndex"),
                        scheduledTime = if (obj.has("scheduledTime") && !obj.isNull("scheduledTime")) obj.getString("scheduledTime") else null,
                        isDone = obj.optBoolean("isDone", false)
                    ))
                }
                if (list.isNotEmpty()) dao.insertDailyRoutineItems(list)
            }

            // 15. Reminders
            root.optJSONArray("reminders")?.let { arr ->
                val list = mutableListOf<Reminder>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(Reminder(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        category = obj.getString("category"),
                        scheduledTime = obj.getString("scheduledTime"),
                        daysOfWeek = obj.optString("daysOfWeek", "[1,2,3,4,5,6,0]"),
                        notificationId = if (obj.has("notificationId") && !obj.isNull("notificationId")) obj.getString("notificationId") else null,
                        isDeleted = obj.optBoolean("isDeleted", false),
                        createdAt = obj.getString("createdAt")
                    ))
                }
                if (list.isNotEmpty()) dao.insertReminders(list)
            }

            // 16. Vacation Days
            root.optJSONArray("vacationDays")?.let { arr ->
                val list = mutableListOf<VacationDay>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(VacationDay(
                        id = obj.getString("id"),
                        date = obj.getString("date"),
                        isDeleted = obj.optBoolean("isDeleted", false),
                        createdAt = obj.getString("createdAt")
                    ))
                }
                if (list.isNotEmpty()) dao.insertVacationDays(list)
            }

            Log.d(TAG, "Restore successful for $userEmail")
            Result.success("RESTORE_SUCCESS")
        } catch (e: Exception) {
            Log.e(TAG, "Restore failed", e)
            Result.failure(e)
        }
    }
}
