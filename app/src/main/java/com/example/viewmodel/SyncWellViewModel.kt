package com.example.viewmodel

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import androidx.room.withTransaction
import com.example.domain.*
import com.example.service.AnalyticsComputationService
import com.example.service.CloudBackupService
import com.example.service.DailyBackupReceiver
import com.example.service.DriveConsentRequiredException
import com.example.service.JourneyReminderScheduler
import com.example.service.DayBoundaryManager
import com.example.service.GoalService
import com.example.service.ReminderScheduler
import com.example.service.TodayWidget
import com.example.service.HealthConnectService
import com.example.service.ProgressiveOverloadEngine
import com.example.service.SupplementValidator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import org.json.JSONArray
import org.json.JSONObject

sealed class AppLaunchState {
    object Loading : AppLaunchState()
    object OnboardingRequired : AppLaunchState()
    object Ready : AppLaunchState()
}

data class Streaks(
    val routine: StreakResult = StreakResult(0, 0, true),
    val diet: StreakResult = StreakResult(0, 0, true),
    val gym: GymStreakResult = GymStreakResult(0, 0, 0, 1)
)

class SyncWellViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val dao = db.syncWellDao()
    private val prefs: SharedPreferences =
        application.getSharedPreferences("syncwell_session_prefs", android.content.Context.MODE_PRIVATE)

    private val credentialManager = CredentialManager.create(application)

    // 1. AUTHENTICATION STORE (Simulated)
    private val _user = MutableStateFlow<String?>(null)
    val user: StateFlow<String?> = _user.asStateFlow()

    // 2. UI STORE
    private val _activeModal = MutableStateFlow<String?>(null)
    val activeModal: StateFlow<String?> = _activeModal.asStateFlow()

    // LAUNCH STATE — controls full-screen splash vs onboarding vs main app
    private val _appLaunchState = MutableStateFlow<AppLaunchState>(AppLaunchState.Loading)
    val appLaunchState: StateFlow<AppLaunchState> = _appLaunchState.asStateFlow()

    // 3. SETTINGS STORE
    private val _theme = MutableStateFlow("dark")
    val theme: StateFlow<String> = _theme.asStateFlow()

    private val _backupEnabled = MutableStateFlow(false)
    val backupEnabled: StateFlow<Boolean> = _backupEnabled.asStateFlow()

    private val _hasCompletedOnboarding = MutableStateFlow(false)
    val hasCompletedOnboarding: StateFlow<Boolean> = _hasCompletedOnboarding.asStateFlow()

    private val _workoutLocationMode = MutableStateFlow("GYM")
    val workoutLocationMode: StateFlow<String> = _workoutLocationMode.asStateFlow()

    private val _waterGoalMl = MutableStateFlow(2500)
    val waterGoalMl: StateFlow<Int> = _waterGoalMl.asStateFlow()

    private val _targetWeightKg = MutableStateFlow(0.0) // raw goal, 0.0 = not set
    // shortcut: when no goal is set, charts use the latest logged weight (else 70) as the reference line; hide it properly if this ships to many users
    val targetWeightKg: StateFlow<Double> = combine(_targetWeightKg, dao.getWeightLogs()) { goal, logs ->
        if (goal > 0.0) goal else logs.firstOrNull()?.weightKg ?: 70.0
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 70.0)

    // 4. ROUTINE STORE
    private val _activeRoutineItems = MutableStateFlow<List<DailyRoutineItem>>(emptyList())
    val activeRoutineItems: StateFlow<List<DailyRoutineItem>> = _activeRoutineItems.asStateFlow()

    // 5. WORKOUT STORE
    private val _currentActiveSession = MutableStateFlow<WorkoutSession?>(null)
    val currentActiveSession: StateFlow<WorkoutSession?> = _currentActiveSession.asStateFlow()

    private val _activeSessionExercises = MutableStateFlow<List<WorkoutSessionExercise>>(emptyList())
    val activeSessionExercises: StateFlow<List<WorkoutSessionExercise>> = _activeSessionExercises.asStateFlow()

    private val _workoutSetLogs = MutableStateFlow<Map<String, List<WorkoutSetLog>>>(emptyMap())
    val workoutSetLogs: StateFlow<Map<String, List<WorkoutSetLog>>> = _workoutSetLogs.asStateFlow()

    // Helper Flow to select all exercises library
    val exerciseLibrary: StateFlow<List<ExerciseLibraryItem>> = dao.getExerciseLibraryFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val weightLogs: Flow<List<WeightLog>> = dao.getWeightLogs()

    // WORKOUT SCREEN STATE (Milestone 9): settings, history, start screen, clock, rest timer, summary
    val workoutSettings: StateFlow<AppSettings?> = dao.getAppSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val finishedSetRows: StateFlow<List<FinishedSetRow>> = dao.getFinishedSetRowsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val workoutHistory: StateFlow<List<WorkoutHistoryItem>> = combine(dao.getFinishedSessionsFlow(), finishedSetRows, dao.getCardioEntriesFlow()) { sessions, rows, cardio ->
        val cardioBySession = cardio.groupBy { it.sessionId }
        val newBests = PersonalBestRule.newBestsBySession(rows.map { HistoricSet(it.sessionId, it.date + "|" + it.startedAt, it.exerciseId, it.weightKg, it.reps) })
        val bySession = rows.groupBy { it.sessionId }
        sessions.map { s ->
            val r = bySession[s.id].orEmpty()
            WorkoutHistoryItem(
                session = s, sets = r.size, exercises = r.map { it.exerciseId }.toSet().size, newBests = newBests[s.id] ?: 0,
                activeSeconds = s.activeSeconds.takeIf { it > 0 } ?: fallbackSeconds(s.startedAt, s.endedAt),
                cardio = cardioBySession[s.id].orEmpty().map { CardioLine(it.type, it.minutes, it.distanceKm, it.inclinePct, it.effort) }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val workoutStart: StateFlow<WorkoutStartState> = combine(
        workoutHistory, finishedSetRows, workoutSettings, exerciseLibrary, DayBoundaryManager.currentTargetDate
    ) { history, rows, settings, library, today ->
        val next = NextSplitRule.next(history.map { it.session.dayType })
        val lastOfNext = history.firstOrNull { NextSplitRule.splitOf(it.session.dayType) == next }
        val lastBest = lastOfNext?.let { item ->
            rows.filter { it.sessionId == item.session.id }.maxWithOrNull(compareBy({ it.weightKg }, { it.reps }))
        }?.let { r -> "${library.firstOrNull { it.id == r.exerciseId }?.name ?: "Best set"}  ${formatKg(r.weightKg)} × ${r.reps}" }
        val weekDates = WeekKey.range(WeekKey.of(today), WeekKey.addDays(WeekKey.of(today), 6))
        val weekDone = history.map { it.session.date }.filter { it in weekDates }.toSet()
        WorkoutStartState(
            next = next, nextLastDate = lastOfNext?.session?.date, nextLastBest = lastBest,
            lastDone = NextSplitRule.lastDone(history.map { it.session.dayType to it.session.date }),
            weekDates = weekDates, weekDone = weekDone,
            thisWeekCount = history.count { it.session.date in weekDates },
            cardioMinutesThisWeek = history.filter { it.session.date in weekDates }.sumOf { h -> h.cardio.sumOf { it.minutes } },
            weeklyTarget = settings?.weeklyGymTarget ?: 3, today = today, recent = history.take(5)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WorkoutStartState())

    // Active time clock. Persisted with the saved active session, so a pause survives leaving the tab and restarting the app.
    private val _sessionClock = MutableStateFlow(SessionClock())
    val sessionClock: StateFlow<SessionClock> = _sessionClock.asStateFlow()

    private val _rest = MutableStateFlow<RestState?>(null)
    val rest: StateFlow<RestState?> = _rest.asStateFlow()
    private var restJob: Job? = null

    private val _exerciseHistory = MutableStateFlow<Map<String, ExerciseHistory>>(emptyMap())
    val exerciseHistory: StateFlow<Map<String, ExerciseHistory>> = _exerciseHistory.asStateFlow()

    // Optional cardio finisher being filled in during the session
    private val _cardioDraft = MutableStateFlow<CardioLine?>(null)
    val cardioDraft: StateFlow<CardioLine?> = _cardioDraft.asStateFlow()
    fun setCardioDraft(c: CardioLine?) { _cardioDraft.value = c }

    private val _workoutSummary = MutableStateFlow<WorkoutSummary?>(null)
    val workoutSummary: StateFlow<WorkoutSummary?> = _workoutSummary.asStateFlow()
    private var finishingWorkout = false

    // 6. TRACKER STORE (Focus stopwatch clock)
    private val _isActiveTimerRunning = MutableStateFlow(false)
    val isActiveTimerRunning: StateFlow<Boolean> = _isActiveTimerRunning.asStateFlow()

    private val _accumulatedSeconds = MutableStateFlow(0)
    val accumulatedSeconds: StateFlow<Int> = _accumulatedSeconds.asStateFlow()

    private val _activeContextType = MutableStateFlow<String?>(null) // STUDY, WORK, TIMEPASS, SPORT, SLEEP
    val activeContextType: StateFlow<String?> = _activeContextType.asStateFlow()

    private val _todayFocusSessions = MutableStateFlow<List<TrackerSession>>(emptyList())
    val todayFocusSessions: StateFlow<List<TrackerSession>> = _todayFocusSessions.asStateFlow()

    private val _activeTodoTag = MutableStateFlow<String?>(null)
    val activeTodoTag: StateFlow<String?> = _activeTodoTag.asStateFlow()

    private val _activeGoalId = MutableStateFlow<String?>(null) // the Journey goal this focus time counts toward
    val activeGoalId: StateFlow<String?> = _activeGoalId.asStateFlow()

    private val _isTrackerScreenFocused = MutableStateFlow(false)
    val isTrackerScreenFocused: StateFlow<Boolean> = _isTrackerScreenFocused.asStateFlow()

    private val _isSessionPausedByAttentionCheck = MutableStateFlow(false)
    val isSessionPausedByAttentionCheck: StateFlow<Boolean> = _isSessionPausedByAttentionCheck.asStateFlow()

    private var timerJob: Job? = null

    // 7. TODO STORE
    val todoCollectionList: StateFlow<List<Todo>> = dao.getActiveTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // FEATURE 11: Goal flows split
    val shortTermGoals: StateFlow<List<Todo>> = dao.getActiveShortTermGoals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val longTermGoals: StateFlow<List<Todo>> = dao.getActiveLongTermGoals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // FEATURE 10: Custom exercise notes flow
    private val _exerciseCustomNotes = MutableStateFlow<Map<String, String>>(emptyMap())
    val exerciseCustomNotes: StateFlow<Map<String, String>> = _exerciseCustomNotes.asStateFlow()

    // 8. ANALYTICS STORE
    // Last 30 days up to today; re-queries when the day changes
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val monthlyAnalytics: StateFlow<List<DailyAnalytics>> = DayBoundaryManager.currentTargetDate
        .flatMapLatest { today -> dao.getAnalyticsBetween(WeekKey.addDays(today, -29), today) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Current and best streaks, read from the whole history so a 12-day streak still shows 12 before today qualifies
    val streaks: StateFlow<Streaks> = combine(
        dao.getAllDailyAnalyticsFlow(), dao.getFinishedSessionDatesFlow(), dao.getAppSettings(), DayBoundaryManager.currentTargetDate
    ) { rows, sessionDates, settings, today ->
        val past = rows.filter { it.dateKey <= today }
        Streaks(
            routine = StreakRule.compute(past.map { StreakDay(it.dateKey, DayCompletion.qualifies(it.routineCompletionPct), it.isVacation) }, today),
            diet = StreakRule.compute(past.map { StreakDay(it.dateKey, it.isDietCompleted, it.isVacation) }, today),
            gym = WeeklyGymStreak.compute(sessionDates, settings?.weeklyGymTarget ?: 3, today)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Streaks())
    
    // Stats: the last 12 weeks for the heatmap, and this week against last week
    data class StatsData(val days: List<HeatmapDay> = emptyList(), val comparison: WeekComparison? = null)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val statsData: StateFlow<StatsData> = DayBoundaryManager.currentTargetDate.flatMapLatest { today ->
        val from = HeatmapGrid.firstDay(today)
        combine(dao.getAnalyticsBetween(from, today), dao.getDayCountsBetween(from, today)) { rows, counts ->
            val countByDate = counts.associateBy { it.dateKey }
            StatsData(
                days = rows.map { r ->
                    val c = countByDate[r.dateKey]
                    HeatmapDay(r.dateKey, r.routineCompletionPct, c?.done ?: 0, c?.total ?: 0, r.isVacation)
                },
                comparison = WeekCompare.of(rows.map { DayStat(it.dateKey, it.routineCompletionPct, it.isGymCompleted, it.totalStudyMinutes + it.workMin) }, today)
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsData())

    // Medals: the state of each one, earned once and kept. The first run, and the first run after the catalogue grows,
    // record existing history silently; later unlocks pop up, at most 3 in a row.
    private val _badgeStates = MutableStateFlow<List<BadgeState>>(emptyList())
    val badgeStates: StateFlow<List<BadgeState>> = _badgeStates.asStateFlow()
    val nextStamp: StateFlow<BadgeState?> = _badgeStates.map { BadgeRules.nextStamp(it) }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _badgeDates = MutableStateFlow(
        (prefs.getStringSet("badge_dates", null) ?: emptySet()).mapNotNull { e -> e.split("=", limit = 2).takeIf { it.size == 2 }?.let { it[0] to it[1] } }.toMap()
    )
    val badgeDates: StateFlow<Map<String, String>> = _badgeDates.asStateFlow() // medal id -> day it was earned (absent for history recorded silently)

    private val unlockQueue = MutableStateFlow<List<String>>(emptyList())
    val unlockedBadge: StateFlow<Badge?> = unlockQueue.map { q -> q.firstOrNull()?.let { id -> BadgeRules.ALL.firstOrNull { it.id == id } } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun dismissUnlock() { unlockQueue.value = unlockQueue.value.drop(1) }

    private fun onBadgesEvaluated(states: List<BadgeState>) {
        _badgeStates.value = states
        val earned = states.filter { it.earned }.map { it.badge.id }.toSet()
        val celebrated = prefs.getStringSet("badges_earned", null)?.toSet() ?: emptySet()
        val silent = !prefs.getBoolean("badges_initialized", false) || prefs.getInt("badge_catalogue_version", 0) < BADGE_CATALOGUE_VERSION
        val plan = BadgeCelebration.plan(earned, celebrated, silent, BadgeRules.ALL.map { it.id })
        if (plan.celebrated == celebrated && !silent) return
        val today = DayBoundaryManager.currentTargetDate.value
        val dates = if (silent) _badgeDates.value else _badgeDates.value + (earned - celebrated).associateWith { today }
        prefs.edit().putStringSet("badges_earned", plan.celebrated)
            .putStringSet("badge_dates", dates.map { "${it.key}=${it.value}" }.toSet())
            .putBoolean("badges_initialized", true).putInt("badge_catalogue_version", BADGE_CATALOGUE_VERSION).apply()
        _badgeDates.value = dates
        unlockQueue.value = unlockQueue.value + plan.queue
    }

    // Weekly review: Sunday only, once per week until dismissed
    private val reviewDismissedWeek = MutableStateFlow(prefs.getString("review_dismissed_week", null))
    val showWeeklyReview: StateFlow<Boolean> = combine(DayBoundaryManager.currentTargetDate, reviewDismissedWeek) { d, w -> WeeklyReview.shouldShow(d, w) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun dismissWeeklyReview() {
        val week = WeekKey.of(DayBoundaryManager.currentTargetDate.value)
        prefs.edit().putString("review_dismissed_week", week).apply()
        reviewDismissedWeek.value = week
    }

    // Habit streaks per template item (for example sunscreen, minoxidil)
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val habitStreaks: StateFlow<Map<String, Int>> = combine(
        dao.getHabitDaysFlow(), dao.getRoutineDatesFlow(), dao.getActiveVacationDays(), DayBoundaryManager.currentTargetDate
    ) { rows, routineDates, vacations, today ->
        HabitStreaks.current(rows.map { HabitDay(it.dateKey, it.templateItemId, it.done) }, routineDates.toSet(), vacations.map { it.date }.toSet(), today)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Streak freeze: one protected day per week, for today only
    val freezeState: StateFlow<FreezeState> = combine(dao.getActiveVacationDays(), DayBoundaryManager.currentTargetDate) { rows, today ->
        val freezes = rows.filter { it.kind == "FREEZE" }.map { it.date }
        FreezeState(frozenToday = today in freezes, canFreeze = FreezeRule.canFreeze(today, freezes))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FreezeState())

    data class FreezeState(val frozenToday: Boolean = false, val canFreeze: Boolean = true)

    fun useFreezeToday() {
        val today = DayBoundaryManager.currentTargetDate.value
        viewModelScope.launch {
            val freezes = dao.getAllVacationDays().filter { it.kind == "FREEZE" }.map { it.date }
            if (!FreezeRule.canFreeze(today, freezes)) return@launch
            val iso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
            dao.insertVacationDay(VacationDay(UUID.randomUUID().toString(), today, false, iso, "FREEZE"))
            AnalyticsComputationService.recalculateDayCache(dao, today)
        }
    }

    /** Undo today's freeze, which makes the week's freeze available again. */
    fun cancelFreezeToday() {
        val today = DayBoundaryManager.currentTargetDate.value
        viewModelScope.launch {
            dao.getAllVacationDays().firstOrNull { it.date == today && it.kind == "FREEZE" }?.let {
                dao.insertVacationDay(it.copy(isDeleted = true))
                AnalyticsComputationService.recalculateDayCache(dao, today)
            }
        }
    }

    // Monday-to-Sunday marks for the Home streak card
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val weekMarks: StateFlow<List<WeekMark>> = DayBoundaryManager.currentTargetDate.flatMapLatest { today ->
        dao.getAnalyticsBetween(WeekKey.of(today), today).map { rows ->
            WeekBar.of(today, rows.filter { DayCompletion.qualifies(it.routineCompletionPct) }.map { it.dateKey }.toSet())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), List(7) { WeekMark.EMPTY })

    // Level from every qualifying day (80% or more) in the history
    val level: StateFlow<LevelResult> = dao.countQualifyingDays().map { LevelRule.of(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LevelRule.of(0))

    private val _todayAnalytics = MutableStateFlow<DailyAnalytics?>(null)
    val todayAnalytics: StateFlow<DailyAnalytics?> = _todayAnalytics.asStateFlow()

    // FEATURE 13: Analytics refresh tracking
    private val _analyticsLastRefreshed = MutableStateFlow<Long?>(null)
    val analyticsLastRefreshed: StateFlow<Long?> = _analyticsLastRefreshed.asStateFlow()

    // 9. VACATION STORE
    private val _isVacationActive = MutableStateFlow(false)
    val isVacationActive: StateFlow<Boolean> = _isVacationActive.asStateFlow()

    // 10. SYNC STORE
    private val _isCloudBackupSyncInProgress = MutableStateFlow(false)
    val isCloudBackupSyncInProgress: StateFlow<Boolean> = _isCloudBackupSyncInProgress.asStateFlow()

    // also written by the 9 PM background backup, so read it back on start
    private val _lastSuccessfulTransmissionTimestamp = MutableStateFlow(prefs.getString("last_backup_iso", null))
    val lastSuccessfulTransmissionTimestamp: StateFlow<String?> = _lastSuccessfulTransmissionTimestamp.asStateFlow()

    // FEATURE 14: Backup test result feedback
    private val _backupTestResult = MutableStateFlow<String?>(null)
    val backupTestResult: StateFlow<String?> = _backupTestResult.asStateFlow()

    // Other Reactive Flow parameters for active dashboard
    private val _todayWaterLogs = MutableStateFlow<List<WaterLog>>(emptyList())
    val todayWaterLogs: StateFlow<List<WaterLog>> = _todayWaterLogs.asStateFlow()

    private val _todayStepsLog = MutableStateFlow<StepsLog?>(null)
    val todayStepsLog: StateFlow<StepsLog?> = _todayStepsLog.asStateFlow()

    // FEATURE: Protein Confirmation state
    private val _proteinConfirmationTask = MutableStateFlow<DailyRoutineItem?>(null)
    val proteinConfirmationTask: StateFlow<DailyRoutineItem?> = _proteinConfirmationTask.asStateFlow()

    private var todayDataJob: Job? = null

    init {
        viewModelScope.launch {
            combine(
                combine(dao.getAllDailyAnalyticsFlow(), dao.getCategoryDayCountsFlow(), dao.getAppSettings(), dao.getFinishedSessionDatesFlow(), DayBoundaryManager.currentTargetDate) { rows, cats, settings, sessions, today ->
                    BadgeInput(
                        days = rows.map { BadgeDay(it.dateKey, it.routineCompletionPct, it.isDietCompleted, it.totalWaterMl) },
                        categoryDays = cats.map { CategoryDay(it.dateKey, it.category, it.done, it.total) },
                        waterGoalMl = settings?.defaultWaterMl ?: 0,
                        bestStreak = StreakRule.compute(rows.filter { it.dateKey <= today }.map { StreakDay(it.dateKey, DayCompletion.qualifies(it.routineCompletionPct), it.isVacation) }, today).best,
                        finishedWorkouts = sessions.size,
                        gymBestWeeks = WeeklyGymStreak.compute(sessions, settings?.weeklyGymTarget ?: 3, today).bestWeeks,
                        vacationDates = rows.filter { it.isVacation }.map { it.dateKey }.toSet()
                    )
                },
                dao.getTickedItemCountFlow(), dao.getFocusDaysFlow()
            ) { base, ticks, focus ->
                BadgeRules.evaluate(base.copy(tickedItems = ticks, focusDays = focus.map { FocusDay(it.dateKey, it.maxMinutes) }))
            }.collect { onBadgesEvaluated(it) }
        }
        viewModelScope.launch {
            // Check onboarding database target on startup
            checkSetupStatus()
            // FIX 6: Restore any in-progress workout session
            restoreActiveSessionFromPrefs()
            restoreFocusFromPrefs()
            ReminderScheduler.scheduleAll(getApplication())
            // FEATURE 14: Schedule the daily 9 PM backup alarm
            scheduleDailyBackupAt9PM()
            // Listen to active calendar changes to load layout
            DayBoundaryManager.currentTargetDate.collect { dateKey ->
                catchUpVacation(dateKey)
                loadTodayData(dateKey)
                // FEATURE 11: Auto-reset completed goals on day rollover
                resetCompletedGoalsOnRollover(dateKey)
            }
        }
    }

    // Onboarding Status Checks
    suspend fun checkSetupStatus() {
        DatabaseSeeder.seedExercises(dao) // insert-if-missing; custom exercises are kept

        // We only check if settings exist to determine onboarding completion.
        val settings = dao.getAppSettingsDirectly()
        if (settings != null) {
            _hasCompletedOnboarding.value = true
            _theme.value = "dark"
            _backupEnabled.value = settings.backupEnabled
            _workoutLocationMode.value = settings.workoutLocationMode
            _targetWeightKg.value = settings.targetWeightKg
            _waterGoalMl.value = settings.defaultWaterMl
            // Load credentials if present
            _user.value = settings.firebaseUid ?: "Offline_User"
            _appLaunchState.value = AppLaunchState.Ready
        } else {
            _hasCompletedOnboarding.value = false
            _appLaunchState.value = AppLaunchState.OnboardingRequired
        }
    }

    /** Saves the onboarding answers, builds the starter routines from them and enters the app. */
    suspend fun completeOnboarding(
        answers: SetupAnswers = SetupAnswers(),
        waterMl: Int = 2500,
        location: String = "GYM",
        currentWeightKg: Double? = null,
        goalWeightKg: Double? = null
    ) {
        val todayIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
        val existing = dao.getAppSettingsDirectly()
        val settings = (existing ?: AppSettings(id = UUID.randomUUID().toString(), firebaseUid = pendingEmail ?: "Offline_User", createdAt = todayIso, updatedAt = todayIso)).copy(
            defaultWaterMl = waterMl,
            targetWeightKg = goalWeightKg ?: 0.0,
            workoutLocationMode = location,
            wakeTime = answers.wakeTime,
            sleepTime = answers.sleepTime,
            workoutDays = answers.workoutDays.joinToString(",", "[", "]"),
            onboardingAnswered = true,
            updatedAt = todayIso
        )
        dao.insertAppSettings(settings)
        DatabaseSeeder.seedStarterRoutines(dao, answers)
        if (currentWeightKg != null && currentWeightKg > 0) {
            val dateKey = DayBoundaryManager.currentTargetDate.value
            dao.insertWeightLog(WeightLog(UUID.randomUUID().toString(), dateKey, currentWeightKg, false, todayIso))
        }
        checkSetupStatus()
        rebuildTodayRoutine()
    }

    /** Re-run setup: rebuilds only the starter routines and updates the stored answers. */
    fun rerunSetup(answers: SetupAnswers, waterMl: Int, location: String, goalWeightKg: Double?) {
        viewModelScope.launch {
            val s = dao.getAppSettingsDirectly() ?: return@launch
            dao.insertAppSettings(s.copy(
                defaultWaterMl = waterMl, workoutLocationMode = location,
                targetWeightKg = goalWeightKg ?: s.targetWeightKg,
                wakeTime = answers.wakeTime, sleepTime = answers.sleepTime,
                workoutDays = answers.workoutDays.joinToString(",", "[", "]")
            ))
            _workoutLocationMode.value = location
            _waterGoalMl.value = waterMl
            goalWeightKg?.let { _targetWeightKg.value = it }
            DatabaseSeeder.seedStarterRoutines(dao, answers)
            rebuildTodayRoutine()
            ReminderScheduler.scheduleAll(getApplication()) // wake and bedtime may have changed
        }
    }

    private var pendingEmail: String? = null

    // Active Day Hydrations and Data Collections
    private fun loadTodayData(dateKey: String) {
        todayDataJob?.cancel()
        todayDataJob = viewModelScope.launch {
            // Check Vacation state
            val isVac = dao.getVacationDayByDate(dateKey)?.kind == "VACATION"
            _isVacationActive.value = isVac

            // Check if Daily Routine checklist row exists. If not, generate from today's dayType template!
            initializeTodayRoutine(dateKey, isVac)

            // Warm up and pre-compute cached daily statistics for Home screen visibility
            AnalyticsComputationService.recalculateDayCache(dao, dateKey)
            TodayWidget.refresh(getApplication())

            // Dynamic reactive sub-flows matching dateKey under the cancelled job
            launch {
                dao.getDailyRoutineItemsForDateFlow(dateKey).collect { items ->
                    _activeRoutineItems.value = items
                }
            }
            launch {
                dao.getWaterLogs(dateKey).collect {
                    _todayWaterLogs.value = it
                }
            }
            launch {
                dao.getStepsLogFlow(dateKey).collect {
                    _todayStepsLog.value = it
                }
            }
            launch {
                dao.getDailyAnalytics(dateKey).collect {
                    _todayAnalytics.value = it
                }
            }
            launch {
                dao.getTrackerSessions(dateKey).collect {
                    _todayFocusSessions.value = it
                }
            }
        }
    }

    private suspend fun initializeTodayRoutine(dateKey: String, isVac: Boolean) {
        // Nothing to generate until onboarding has created the starter routines
        if (dao.getAppSettingsDirectly() == null) return
        if (dao.getDailyRoutineDirectly(dateKey) == null) generateRoutine(dateKey, isVac)
    }

    /** Builds the day's routine from every template whose schedule matches [dateKey]. */
    private suspend fun generateRoutine(
        dateKey: String,
        isVac: Boolean,
        doneTitles: Set<String> = emptySet(),
        proteinAnswers: Map<String, String?> = emptyMap(),
        doneTemplateIds: Set<String> = emptySet()
    ) {
        val matching = RoutineSchedule.templatesForDate(dao.getRoutineTemplates(), dateKey)
        val routineId = UUID.randomUUID().toString()
        dao.insertDailyRoutine(DailyRoutine(routineId, dateKey, matching.firstOrNull()?.id, 0.0, isVac))
        val templateItems = matching.flatMap { dao.getRoutineTemplateItems(it.id) }
        val items = RoutineSchedule.buildDayItems(routineId, templateItems, doneTitles, dao.getAppSettingsDirectly()?.wakeTime ?: "00:00", proteinAnswers, doneTemplateIds)
        if (items.isNotEmpty()) dao.insertDailyRoutineItems(items)
    }

    /** Regenerates today's list from the templates, keeping items already ticked (matched by title). */
    fun rebuildTodayRoutine() {
        val dateKey = DayBoundaryManager.currentTargetDate.value
        viewModelScope.launch {
            val done = dao.getDailyRoutineDirectly(dateKey)?.let { r ->
                dao.getDailyRoutineItems(r.id).filter { it.isDone }
            } ?: emptyList()
            dao.clearDailyRoutineItemsForDate(dateKey)
            dao.clearDailyRoutineForDate(dateKey)
            val legacy = done.filter { it.templateItemId == null } // rows from before items were linked still match by title
            generateRoutine(
                dateKey, _isVacationActive.value,
                doneTitles = legacy.map { it.title }.toSet(),
                proteinAnswers = done.associate { (it.templateItemId ?: it.title) to it.proteinAnswer },
                doneTemplateIds = done.mapNotNull { it.templateItemId }.toSet()
            )
            loadTodayData(dateKey)
        }
    }

    // ASK-FIRST PROMPTS: a tracker event may offer to tick a routine item; only a Yes ticks it, through the normal tick path
    data class LinkPrompt(val event: LinkEvent, val item: DailyRoutineItem, val dateKey: String)

    private val _linkEnabled = MutableStateFlow(LinkEvent.values().filter { prefs.getBoolean("link_${it.name}", true) }.toSet())
    val linkEnabled: StateFlow<Set<LinkEvent>> = _linkEnabled.asStateFlow()

    fun setLinkEnabled(event: LinkEvent, on: Boolean) {
        prefs.edit().putBoolean("link_${event.name}", on).apply()
        _linkEnabled.value = if (on) _linkEnabled.value + event else _linkEnabled.value - event
    }

    private val linkQueue = MutableStateFlow<List<LinkPrompt>>(emptyList())
    val linkPrompt: StateFlow<LinkPrompt?> = linkQueue.map { it.firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private fun promptedToday(dateKey: String): Set<String> =
        if (prefs.getString("prompt_date", null) == dateKey) prefs.getStringSet("prompt_ids", null) ?: emptySet() else emptySet()

    private suspend fun offerLink(event: LinkEvent, supplementName: String? = null) {
        if (event !in _linkEnabled.value) return
        val dateKey = DayBoundaryManager.currentTargetDate.value
        val routine = dao.getDailyRoutineDirectly(dateKey) ?: return
        val items = dao.getDailyRoutineItems(routine.id)
        val shown = promptedToday(dateKey)
        val match = LinkMatchRule.match(event, items.map { LinkItem(it.id, it.title, it.category, it.isDone) }, shown, supplementName) ?: return
        prefs.edit().putString("prompt_date", dateKey).putStringSet("prompt_ids", shown + match.id).apply() // once per item per day, answered or not
        linkQueue.value = linkQueue.value + LinkPrompt(event, items.first { it.id == match.id }, dateKey)
    }

    fun answerLinkPrompt(yes: Boolean) {
        val p = linkQueue.value.firstOrNull() ?: return
        linkQueue.value = linkQueue.value.drop(1)
        if (!yes) return
        viewModelScope.launch {
            val fresh = dao.getDailyRoutineItems(p.item.dailyRoutineId).firstOrNull { it.id == p.item.id } ?: return@launch
            if (!fresh.isDone) executeRoutineToggle(fresh, dateKey = p.dateKey) // same path as a manual tick
        }
    }

    // Toggle Routine item Checkbox
    fun toggleRoutineItem(item: DailyRoutineItem) {
        val isProteinRelated = !item.isDone && item.category == "DIET"

        if (isProteinRelated) {
            _proteinConfirmationTask.value = item
        } else {
            executeRoutineToggle(item)
        }
    }

    fun confirmProteinIntake(item: DailyRoutineItem, tookProtein: Boolean) {
        _proteinConfirmationTask.value = null
        // The meal is ticked either way (it counts toward the 80%); only a Yes counts toward the diet streak.
        executeRoutineToggle(item, if (tookProtein) DietRule.YES else DietRule.NO)
    }

    fun cancelProteinConfirmation() {
        _proteinConfirmationTask.value = null
    }

    private fun executeRoutineToggle(
        item: DailyRoutineItem,
        proteinAnswer: String? = null,
        dateKey: String = DayBoundaryManager.currentTargetDate.value // read at tap time, so a tick at midnight lands on the day it was made
    ) {
        viewModelScope.launch {
            // Unticking clears the answer, so the next tick asks again
            val updated = item.copy(isDone = !item.isDone, proteinAnswer = if (item.isDone) null else proteinAnswer)
            dao.updateDailyRoutineItem(updated)

            // Update UI state directly
            val currentItems = _activeRoutineItems.value.map { if (it.id == item.id) updated else it }
            _activeRoutineItems.value = currentItems

            // Writes the day's percent and the statistics cache
            AnalyticsComputationService.recalculateDayCache(dao, dateKey).onFailure { _message.tryEmit("Could not update stats") }
            TodayWidget.refresh(getApplication())

            // 80% reached: a small "Today counts" once a day
            if (TodayCounts.shouldShow(currentItems.count { it.isDone }, currentItems.size, prefs.getString("counts_toast_date", null), dateKey, _isVacationActive.value)) {
                prefs.edit().putString("counts_toast_date", dateKey).apply()
                _message.tryEmit(TodayCounts.MESSAGE)
            }
        }
    }

    // 1. AUTHENTICATION
    fun initiateGoogleSignIn(context: Context) {
        viewModelScope.launch {
            try {
                Log.d("SyncWellAuth", "Starting Google Sign-In flow...")
                _activeModal.value = "SIGNING_IN"
                
                val serverClientId = context.getString(R.string.google_web_client_id)


                val googleIdOption = GetSignInWithGoogleOption.Builder(serverClientId)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                Log.d("SyncWellAuth", "Calling getCredential...")
                val result = credentialManager.getCredential(
                    request = request,
                    context = context
                )
                
                Log.d("SyncWellAuth", "getCredential successful. Credential type: ${result.credential.type}")
                handleSignIn(result.credential)
                _activeModal.value = null
            } catch (e: GetCredentialException) {
                Log.e("SyncWellAuth", "Credential Manager Error (${e::class.java.simpleName}): ${e.message}", e)
                _activeModal.value = null
            } catch (e: Exception) {
                Log.e("SyncWellAuth", "General Auth Error: ${e.message}", e)
                _activeModal.value = null
            }
        }
    }

    private fun handleSignIn(credential: androidx.credentials.Credential) {
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            try {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val displayName = googleIdTokenCredential.displayName ?: googleIdTokenCredential.id
                val email = googleIdTokenCredential.id
                
                authorizeGoogleSignIn(displayName, email)
            } catch (e: GoogleIdTokenParsingException) {
                Log.e("SyncWellAuth", "Received an invalid google id token response", e)
            }
        } else {
            Log.e("SyncWellAuth", "Unexpected type of credential: ${credential.type}")
        }
    }

    fun authorizeGoogleSignIn(name: String, email: String) {
        _user.value = name
        pendingEmail = email
        viewModelScope.launch {
            val settings = dao.getAppSettingsDirectly()
            if (settings != null) {
                val updated = settings.copy(firebaseUid = email)
                dao.insertAppSettings(updated)
            }
            
            // NEW: Check for existing cloud backup upon login
            checkForCloudBackup(email)
        }
    }

    /** One-time messages for the UI (shown as a toast). */
    private val _message = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val message: SharedFlow<String> = _message.asSharedFlow()

    // Drive consent: the Activity launches this and reports back via onDriveConsentResult()
    private val _driveConsentRequest = MutableSharedFlow<android.app.PendingIntent>(extraBufferCapacity = 1)
    val driveConsentRequest: SharedFlow<android.app.PendingIntent> = _driveConsentRequest.asSharedFlow()
    private var actionAfterDriveConsent: (() -> Unit)? = null
    private var pendingDriveConsentIntent: android.app.PendingIntent? = null

    /** If [e] means Drive access is still needed, shows an explainer dialog first, then Google's consent screen; retries [retry] once granted. */
    private fun requestDriveConsentIfNeeded(e: Throwable, retry: () -> Unit): Boolean {
        if (e !is DriveConsentRequiredException) return false
        actionAfterDriveConsent = retry
        pendingDriveConsentIntent = e.pendingIntent
        _activeModal.value = "DRIVE_CONSENT_INFO"
        return true
    }

    fun continueDriveConsent() {
        _activeModal.value = null
        pendingDriveConsentIntent?.let { _driveConsentRequest.tryEmit(it) }
    }

    fun cancelDriveConsent() {
        _activeModal.value = null
        onDriveConsentResult(false)
    }

    fun onDriveConsentResult(granted: Boolean) {
        val retry = actionAfterDriveConsent
        actionAfterDriveConsent = null
        if (granted) retry?.invoke() else _backupTestResult.value = "FAILED: Google Drive access was not granted"
    }

    private fun checkForCloudBackup(email: String) {
        viewModelScope.launch {
            try {
                if (CloudBackupService.hasCloudBackup(getApplication(), email)) {
                    Log.d("SyncWellAuth", "Cloud backup found for $email")
                    _activeModal.value = "RESTORE_CONFIRMATION"
                } else {
                    Log.d("SyncWellAuth", "No cloud backup found for $email")
                }
            } catch (e: Exception) {
                if (!requestDriveConsentIfNeeded(e) { checkForCloudBackup(email) }) {
                    Log.e("SyncWellAuth", "Cloud backup check failed", e)
                }
            }
        }
    }

    fun restoreDataFromCloud() {
        viewModelScope.launch {
            val settings = dao.getAppSettingsDirectly()
            val email = pendingEmail ?: settings?.firebaseUid ?: _user.value ?: return@launch

            _isCloudBackupSyncInProgress.value = true
            try {
                val result = CloudBackupService.performRestore(getApplication(), dao, email)
                result.fold(
                    onSuccess = {
                        Log.d("SyncWellAuth", "Restore successful for $email")
                        _backupTestResult.value = "SUCCESS"
                        viewModelScope.launch {
                            checkSetupStatus() // enters the app when the restore supplied the settings
                            rebuildTodayRoutine()
                        }
                    },
                    onFailure = { e ->
                        if (!requestDriveConsentIfNeeded(e) { restoreDataFromCloud() }) {
                            Log.e("SyncWellAuth", "Restore failed", e)
                            _backupTestResult.value = "FAILED: ${e.message}"
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e("SyncWellAuth", "Restore failed", e)
                _backupTestResult.value = "FAILED: ${e.message}"
            } finally {
                _isCloudBackupSyncInProgress.value = false
                if (_activeModal.value == "RESTORE_CONFIRMATION") _activeModal.value = null
            }
        }
    }

    fun clearAuthentication() {
        _user.value = null
        viewModelScope.launch {
            val settings = dao.getAppSettingsDirectly()
            if (settings != null) {
                dao.insertAppSettings(settings.copy(firebaseUid = null))
            }
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.e("SyncWellAuth", "Error clearing credential state", e)
            }
        }
    }

    // 2. UI MODAL CONTROLLERS
    fun setActiveModal(modalId: String?) {
        _activeModal.value = modalId
    }

    // --- FOCUS TRACKER SERVICE CONTROLS ---
    private fun startTimerService(category: String, tag: String?) {
        val intent = android.content.Intent(getApplication(), com.example.service.FocusTimerService::class.java).apply {
            action = com.example.service.FocusTimerService.ACTION_START
            putExtra(com.example.service.FocusTimerService.EXTRA_CATEGORY, category)
            putExtra(com.example.service.FocusTimerService.EXTRA_TAG, tag)
            putExtra(com.example.service.FocusTimerService.EXTRA_START_TIME, System.currentTimeMillis() - (_accumulatedSeconds.value * 1000L))
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            getApplication<Application>().startForegroundService(intent)
        } else {
            getApplication<Application>().startService(intent)
        }
    }

    // stopService is allowed from the background (startService is not) and does nothing when the service is not running
    private fun stopTimerService() {
        getApplication<Application>().stopService(android.content.Intent(getApplication(), com.example.service.FocusTimerService::class.java))
    }

    fun scheduleAttentionCheck() {
        val intent = android.content.Intent(getApplication(), com.example.service.FocusTimerService::class.java).apply {
            action = com.example.service.FocusTimerService.ACTION_SCHEDULE_CHECK
        }
        getApplication<Application>().startService(intent)
    }

    fun cancelAttentionCheck() {
        val intent = android.content.Intent(getApplication(), com.example.service.FocusTimerService::class.java).apply {
            action = com.example.service.FocusTimerService.ACTION_CANCEL_CHECK
        }
        getApplication<Application>().startService(intent)
    }

    // Register BroadcastReceiver for auto-pause
    private val focusReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
            if (intent?.action == "com.example.FOCUS_PAUSE") {
                _isSessionPausedByAttentionCheck.value = true
                saveFocusSession()
            }
        }
    }

    init {
        val filter = android.content.IntentFilter("com.example.FOCUS_PAUSE")
        androidx.core.content.ContextCompat.registerReceiver(
            getApplication(),
            focusReceiver,
            filter,
            androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onCleared() {
        getApplication<Application>().unregisterReceiver(focusReceiver)
        super.onCleared()
    }

    // 3. SETTINGS INTERFACES
    fun toggleTheme() {
        // App is locked to high-performance OLED Dark Mode
        _theme.value = "dark"
        viewModelScope.launch {
            val settings = dao.getAppSettingsDirectly()
            if (settings != null) {
                dao.insertAppSettings(settings.copy(themeMode = "dark"))
            }
        }
    }

    fun setBackupEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val settings = dao.getAppSettingsDirectly() ?: return@launch
            val on = enabled && settings.firebaseUid?.contains("@") == true // needs a signed-in Google account
            _backupEnabled.value = on
            dao.insertAppSettings(settings.copy(backupEnabled = on))
        }
    }

    fun setWorkoutLocationMode(mode: String) {
        _workoutLocationMode.value = mode
        viewModelScope.launch {
            val settings = dao.getAppSettingsDirectly()
            if (settings != null) {
                dao.insertAppSettings(settings.copy(workoutLocationMode = mode))
            }
        }
    }

    fun setTargetWeight(weight: Double) {
        if (weight <= 0.0) return
        _targetWeightKg.value = weight
        viewModelScope.launch {
            val settings = dao.getAppSettingsDirectly()
            if (settings != null) {
                dao.insertAppSettings(settings.copy(targetWeightKg = weight))
            }
            AnalyticsComputationService.recalculateDayCache(dao, DayBoundaryManager.currentTargetDate.value)
        }
    }

    fun setActiveTodoTag(tag: String?) {
        _activeTodoTag.value = tag
    }

    fun setActiveGoal(goalId: String?) {
        _activeGoalId.value = goalId
    }

    fun setTrackerScreenFocused(focused: Boolean) {
        _isTrackerScreenFocused.value = focused
        if (focused) {
            cancelAttentionCheck()
        } else if (_isActiveTimerRunning.value) {
            scheduleAttentionCheck()
        }
    }

    fun clearPauseState() {
        _isSessionPausedByAttentionCheck.value = false
    }

    // 6. TRACKER TIMER CONTROLS
    fun bootTimer(type: String) {
        _activeContextType.value = type
        _isActiveTimerRunning.value = true
        requestNotificationPermission() // the focus notification is hidden on Android 13+ until this is granted
        startTimerService(type, _activeTodoTag.value)
        val baseSec = _accumulatedSeconds.value
        val runStartMs = System.currentTimeMillis()
        persistFocus(type, baseSec, runStartMs)
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_isActiveTimerRunning.value) {
                delay(1000)
                _accumulatedSeconds.value = baseSec + ((System.currentTimeMillis() - runStartMs) / 1000).toInt()
            }
        }
    }

    fun pauseTimer() {
        _isActiveTimerRunning.value = false
        timerJob?.cancel()
        stopTimerService()
        _activeContextType.value?.let { persistFocus(it, _accumulatedSeconds.value, null) }
    }

    fun resetTimer() {
        _isActiveTimerRunning.value = false
        timerJob?.cancel()
        _accumulatedSeconds.value = 0
        stopTimerService()
        _activeContextType.value = null
        prefs.edit().remove("focus_type").remove("focus_tag").remove("focus_goal").remove("focus_base_sec").remove("focus_run_start").apply()
    }

    // Focus session survives the app being killed: category, tag, seconds so far, and when the current run started (null = paused)
    private fun persistFocus(type: String, baseSec: Int, runStartMs: Long?) {
        prefs.edit().putString("focus_type", type).putString("focus_tag", _activeTodoTag.value).putString("focus_goal", _activeGoalId.value)
            .putInt("focus_base_sec", baseSec).putLong("focus_run_start", runStartMs ?: 0L).apply()
    }

    /** Restores a focus session left behind by a killed app, paused, so the user can save or reset it. */
    private fun restoreFocusFromPrefs() {
        val type = prefs.getString("focus_type", null) ?: return
        val runStart = prefs.getLong("focus_run_start", 0L)
        // The app was killed, so nobody was watching: count away time only up to the 6-minute auto-pause limit
        val away = if (runStart > 0) ((System.currentTimeMillis() - runStart) / 1000).toInt().coerceIn(0, 360) else 0
        _activeContextType.value = type
        _activeTodoTag.value = prefs.getString("focus_tag", null)
        _activeGoalId.value = prefs.getString("focus_goal", null)
        _accumulatedSeconds.value = prefs.getInt("focus_base_sec", 0) + away
        persistFocus(type, _accumulatedSeconds.value, null)
    }

    fun saveFocusSession() {
        val type = _activeContextType.value ?: return
        val sec = _accumulatedSeconds.value
        if (sec <= 0) return

        val durationMin = Math.max(1, sec / 60)
        val todayKey = DayBoundaryManager.currentTargetDate.value
        val todayIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())

        val tagSuffix = _activeTodoTag.value?.let { "|Tag: $it" } ?: ""
        val goalId = _activeGoalId.value

        viewModelScope.launch {
            val session = TrackerSession(
                id = UUID.randomUUID().toString(),
                activityType = type,
                source = "TIMER$tagSuffix",
                startTime = todayIso,
                endTime = todayIso,
                durationMin = durationMin,
                dateKey = todayKey,
                isDeleted = false,
                goalId = goalId
            )
            dao.insertTrackerSession(session)
            
            // Reset focus Stopwatch states
            resetTimer()
            _activeTodoTag.value = null
            _activeGoalId.value = null

            // Dynamic recalculation of cached statistics
            AnalyticsComputationService.recalculateDayCache(dao, todayKey)
            if (type == "STUDY" || type == "WORK") offerLink(LinkEvent.FOCUS)
        }
    }

    // 5. WORKOUT MANAGER
    private var startingWorkout = false

    fun startWorkoutSession(dayType: String, environment: String) {
        if (startingWorkout) return // a second tap while the first start is still writing
        startingWorkout = true
        val todayKey = DayBoundaryManager.currentTargetDate.value
        val todayIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
        val session = WorkoutSession(
            id = UUID.randomUUID().toString(),
            date = todayKey,
            dayType = dayType,
            environment = environment,
            startedAt = todayIso,
            endedAt = null,
            isDeleted = false
        )
        
        _currentActiveSession.value = session
        _activeSessionExercises.value = emptyList()
        _workoutSetLogs.value = emptyMap()
        _exerciseHistory.value = emptyMap()
        skipRest()
        _sessionClock.value = SessionClock.startedAt(System.currentTimeMillis())
        persistActiveSessionToPrefs() // FIX 6

        viewModelScope.launch {
            dao.insertWorkoutSession(session)
            dao.softDeleteUnfinishedSessionsExcept(session.id) // earlier starts that were never finished or resumed

            // Select relevant exercises based on push/pull/legs muscle categories
            val targetedCategories = when {
                dayType.contains("Push", ignoreCase = true) -> listOf("CHEST", "SHOULDERS", "TRICEPS")
                dayType.contains("Pull", ignoreCase = true) -> listOf("BACK", "BICEPS", "REAR_DELTS", "FOREARMS")
                dayType.contains("Legs", ignoreCase = true) -> listOf("LEGS", "ABS", "CALVES")
                else -> listOf("CHEST")
            }

            val library = dao.getExerciseLibrary()
            val environmentExercises = if (environment == "HOME") {
                library.filter { it.environmentType == "HOME" }
            } else {
                library.filter { it.environmentType == null }
            }

            val orderedIds = if (environment == "HOME") {
                when {
                    dayType.contains("Push", ignoreCase = true) -> listOf(
                        "home_pushups", "home_pike_pushups", "home_diamond_pushups",
                        "home_chair_dips", "home_bottle_lateral_raises"
                    )
                    dayType.contains("Pull", ignoreCase = true) -> listOf(
                        "home_pullups", "home_inverted_rows", "home_backpack_rows",
                        "home_backpack_curls", "home_y_t_raises"
                    )
                    dayType.contains("Legs", ignoreCase = true) -> listOf(
                        "home_bulgarian_squats", "home_bw_squats", "home_single_leg_thrust",
                        "home_step_calf_raises", "home_lying_leg_raises", "home_plank"
                    )
                    else -> emptyList()
                }
            } else {
                when {
                    dayType.contains("Push", ignoreCase = true) -> listOf(
                        "ex_pushups", "ex_flat_bench", "ex_overhead_press", "ex_incline_press",
                        "ex_lateral_raises", "ex_lateral_cable", "ex_pec_fly", "ex_tricep_pushdown",
                        "ex_cable_crossover", "ex_tricep_overhead", "ex_pullover"
                    )
                    dayType.contains("Pull", ignoreCase = true) -> listOf(
                        "ex_pullups", "ex_deadlift", "ex_lat_pulldown", "ex_db_row",
                        "ex_cable_row", "ex_face_pull", "ex_rear_delt_raise", "ex_bicep_curl",
                        "ex_hammer_curl", "ex_preacher_curl", "ex_wrist_curl"
                    )
                    dayType.contains("Legs", ignoreCase = true) -> listOf(
                        "ex_free_squats", "ex_weighted_squat", "ex_leg_press", "ex_rdl",
                        "ex_leg_extension", "ex_leg_curl", "ex_lunges", "ex_calf_raises",
                        "ex_leg_raises", "ex_cable_crunches", "ex_decline_crunches", "ex_russian_twists", "ex_plank"
                    )
                    else -> emptyList()
                }
            }

            val filteredExercises = environmentExercises.filter { it.category in targetedCategories }
                .sortedBy { ex ->
                    val idx = orderedIds.indexOf(ex.id)
                    if (idx != -1) idx else 999
                }
                .filter { it.id in orderedIds } // Only keep exercises defined in the split

            val sessionExercises = filteredExercises.mapIndexed { idx, ex ->
                // The next-time target saved when the last workout finished (what its summary showed); else last logged set
                val suggestedState = dao.getExerciseProgression(ex.id)
                val lastLog = dao.getLastCompletedSetLogForExercise(ex.id)
                val targetWt = suggestedState?.suggestedWeightKg ?: lastLog?.weightKg ?: 0.0
                val targetReps = suggestedState?.suggestedReps ?: lastLog?.reps ?: ex.defaultRepsMax

                WorkoutSessionExercise(
                    id = UUID.randomUUID().toString(),
                    sessionId = session.id,
                    exerciseId = ex.id,
                    orderIndex = idx + 1,
                    targetSets = ex.defaultSets,
                    targetRepsMax = targetReps,
                    targetWeightKg = targetWt
                )
            }
            dao.insertWorkoutSessionExercises(sessionExercises)

            // Automatically pre-populate default checklist logs
            val defaultLogs = mutableMapOf<String, List<WorkoutSetLog>>()
            sessionExercises.forEach { se ->
                val logsList = (1..se.targetSets).map { setIdx ->
                    WorkoutSetLog(
                        id = UUID.randomUUID().toString(),
                        sessionExerciseId = se.id,
                        setIndex = setIdx,
                        weightKg = se.targetWeightKg ?: 0.0,
                        reps = se.targetRepsMax,
                        isCompleted = false
                    )
                }
                dao.insertWorkoutSetLogs(logsList)
                defaultLogs[se.id] = logsList
            }

            loadExerciseHistory(session.id, sessionExercises.map { it.exerciseId })
            _activeSessionExercises.value = sessionExercises
            _workoutSetLogs.value = defaultLogs
            startingWorkout = false
        }
    }

    fun addExerciseToActiveSession(exerciseId: String) {
        val session = _currentActiveSession.value ?: return
        viewModelScope.launch {
            val orderIdx = (_activeSessionExercises.value.maxOfOrNull { it.orderIndex } ?: 0) + 1
            
            // Fetch previous completed set metrics for progression matching
            val lastLog = dao.getLastCompletedSetLogForExercise(exerciseId)
            val suggestedState = dao.getExerciseProgression(exerciseId)
            val targetWt = suggestedState?.suggestedWeightKg ?: lastLog?.weightKg ?: 0.0
            val targetReps = suggestedState?.suggestedReps ?: lastLog?.reps ?: 12

            val library = dao.getExerciseLibrary()
            val exItem = library.find { it.id == exerciseId } ?: return@launch

            val se = WorkoutSessionExercise(
                id = UUID.randomUUID().toString(),
                sessionId = session.id,
                exerciseId = exerciseId,
                orderIndex = orderIdx,
                targetSets = exItem.defaultSets,
                targetRepsMax = targetReps,
                targetWeightKg = targetWt
            )
            dao.insertWorkoutSessionExercise(se)

            val logsList = (1..se.targetSets).map { setIdx ->
                WorkoutSetLog(
                    id = UUID.randomUUID().toString(),
                    sessionExerciseId = se.id,
                    setIndex = setIdx,
                    weightKg = se.targetWeightKg ?: 0.0,
                    reps = se.targetRepsMax,
                    isCompleted = false
                )
            }
            dao.insertWorkoutSetLogs(logsList)
            loadExerciseHistory(session.id, listOf(exerciseId))

            val updatedSpecs = _activeSessionExercises.value.toMutableList().apply { add(se) }
            _activeSessionExercises.value = updatedSpecs

            val updatedSetsMap = _workoutSetLogs.value.toMutableMap()
            updatedSetsMap[se.id] = logsList
            _workoutSetLogs.value = updatedSetsMap
        }
    }

    fun deleteExerciseFromActiveSession(sessionExerciseId: String) {
        val element = _activeSessionExercises.value.find { it.id == sessionExerciseId }
        if (element != null) {
            viewModelScope.launch {
                dao.deleteWorkoutSessionExercise(element)
                dao.deleteSetLogsForExercise(sessionExerciseId)

                val updatedExercises = _activeSessionExercises.value.filter { it.id != sessionExerciseId }
                _activeSessionExercises.value = updatedExercises

                val updatedSetLogs = _workoutSetLogs.value.toMutableMap().apply { remove(sessionExerciseId) }
                _workoutSetLogs.value = updatedSetLogs
            }
        }
    }

    fun deleteSetLogFromSessionExercise(sessionExerciseId: String, setLogId: String) {
        val currentLogs = _workoutSetLogs.value[sessionExerciseId] ?: return
        val targetLog = currentLogs.find { it.id == setLogId } ?: return

        viewModelScope.launch {
            dao.deleteWorkoutSetLog(targetLog)

            val updatedLogs = currentLogs.filter { it.id != setLogId }.toMutableList()
            val reindexedLogs = updatedLogs.mapIndexed { idx, log ->
                val newLog = log.copy(setIndex = idx + 1)
                dao.updateWorkoutSetLog(newLog)
                newLog
            }

            val updatedMap = _workoutSetLogs.value.toMutableMap()
            updatedMap[sessionExerciseId] = reindexedLogs
            _workoutSetLogs.value = updatedMap
        }
    }

    fun addSetToSessionExercise(sessionExerciseId: String) {
        val currentLogs = _workoutSetLogs.value[sessionExerciseId] ?: return
        val nextIdx = (currentLogs.maxOfOrNull { it.setIndex } ?: 0) + 1
        val lastWeight = currentLogs.lastOrNull()?.weightKg ?: 0.0
        val lastReps = currentLogs.lastOrNull()?.reps ?: 12

        viewModelScope.launch {
            val newLog = WorkoutSetLog(
                id = UUID.randomUUID().toString(),
                sessionExerciseId = sessionExerciseId,
                setIndex = nextIdx,
                weightKg = lastWeight,
                reps = lastReps,
                isCompleted = false
            )
            dao.insertWorkoutSetLogs(listOf(newLog))

            val updatedLogs = currentLogs.toMutableList().apply { add(newLog) }
            val updatedMap = _workoutSetLogs.value.toMutableMap()
            updatedMap[sessionExerciseId] = updatedLogs
            _workoutSetLogs.value = updatedMap
        }
    }

    fun deleteLastSetFromSessionExercise(sessionExerciseId: String) {
        val currentLogs = _workoutSetLogs.value[sessionExerciseId] ?: return
        if (currentLogs.isEmpty()) return
        val lastLog = currentLogs.last()

        viewModelScope.launch {
            dao.deleteWorkoutSetLog(lastLog)

            val updatedLogs = currentLogs.toMutableList().apply { removeAt(currentLogs.size - 1) }
            val updatedMap = _workoutSetLogs.value.toMutableMap()
            updatedMap[sessionExerciseId] = updatedLogs
            _workoutSetLogs.value = updatedMap
        }
    }

    fun updateWorkoutSetLogDirectly(logId: String, sessionExerciseId: String, reps: Int, weightKg: Double, isCompleted: Boolean) {
        val currentLogs = _workoutSetLogs.value[sessionExerciseId] ?: return
        val targetIdx = currentLogs.indexOfFirst { it.id == logId }
        if (targetIdx != -1) {
            val updatedLog = currentLogs[targetIdx].copy(reps = reps, weightKg = weightKg, isCompleted = isCompleted)
            viewModelScope.launch {
                dao.updateWorkoutSetLog(updatedLog)

                val updatedLogs = currentLogs.toMutableList()
                updatedLogs[targetIdx] = updatedLog
                val updatedMap = _workoutSetLogs.value.toMutableMap()
                updatedMap[sessionExerciseId] = updatedLogs
                _workoutSetLogs.value = updatedMap
            }
        }
    }

    fun updateExerciseTargets(sessionExerciseId: String, reps: Int, weight: Double) {
        viewModelScope.launch {
            dao.updateWorkoutSessionExerciseTargets(sessionExerciseId, reps, weight)
            // Update local state
            val updatedExercises = _activeSessionExercises.value.map {
                if (it.id == sessionExerciseId) it.copy(targetRepsMax = reps, targetWeightKg = weight) else it
            }
            _activeSessionExercises.value = updatedExercises
        }
    }

    /** Throws the session away: it is stored as deleted so it never counts as a finished workout or as "last time" data. */
    fun discardWorkout(onDone: () -> Unit = {}) {
        val session = _currentActiveSession.value ?: return
        viewModelScope.launch {
            dao.softDeleteWorkoutSession(session.id)
            _currentActiveSession.value = null
            _activeSessionExercises.value = emptyList()
            _workoutSetLogs.value = emptyMap()
            skipRest()
            clearActiveSessionFromPrefs() // FIX 6
            AnalyticsComputationService.recalculateDayCache(dao, session.date)
            onDone()
        }
    }

    // FIX 6: SharedPreferences-based session persistence helpers
    private fun persistActiveSessionToPrefs() {
        val session = _currentActiveSession.value ?: return
        prefs.edit()
            .putString("active_session_id", session.id)
            .putString("active_session_day_type", session.dayType)
            .putString("active_session_env", session.environment)
            .putLong("active_clock_acc_ms", _sessionClock.value.accumulatedMs)
            .putLong("active_clock_since_ms", _sessionClock.value.runningSinceMs ?: -1L)
            .apply()
    }

    private fun clearActiveSessionFromPrefs() {
        _cardioDraft.value = null
        prefs.edit()
            .remove("active_session_id")
            .remove("active_session_day_type")
            .remove("active_session_env")
            .remove("active_clock_acc_ms")
            .remove("active_clock_since_ms")
            .apply()
    }

    fun pauseWorkout() {
        if (_currentActiveSession.value == null) return
        _sessionClock.value = _sessionClock.value.pause(System.currentTimeMillis())
        persistActiveSessionToPrefs()
    }

    fun resumeWorkout() {
        if (_currentActiveSession.value == null) return
        _sessionClock.value = _sessionClock.value.resume(System.currentTimeMillis())
        persistActiveSessionToPrefs()
    }

    private suspend fun loadExerciseHistory(sessionId: String, exerciseIds: Collection<String>) {
        val loaded = _exerciseHistory.value.toMutableMap()
        exerciseIds.filter { it !in loaded }.forEach { id ->
            loaded[id] = ExerciseHistory(
                previous = dao.getPreviousSessionSetsForExercise(id, sessionId).map { SetRef(it.weightKg, it.reps) },
                best = dao.getBestFinishedSetForExercise(id, sessionId)?.let { SetRef(it.weightKg, it.reps) }
            )
        }
        _exerciseHistory.value = loaded
    }

    // --- Workout settings edited from the Workout screen and Config
    fun setWeeklyGymTarget(target: Int) = updateSettings { it.copy(weeklyGymTarget = target.coerceIn(1, 7)) }
    fun setRestTimerEnabled(on: Boolean) = updateSettings { it.copy(restTimerEnabled = on) }
    fun setRestSeconds(seconds: Int) = updateSettings { it.copy(restSeconds = seconds.coerceIn(15, 600)) }

    private fun updateSettings(change: (AppSettings) -> AppSettings) {
        viewModelScope.launch { dao.getAppSettingsDirectly()?.let { dao.insertAppSettings(change(it)) } }
    }

    // --- Rest timer (only when switched on in Config). Counts down only while the workout clock runs.
    private fun startRest(nextLabel: String) {
        restJob?.cancel()
        restJob = viewModelScope.launch {
            val settings = dao.getAppSettingsDirectly() ?: return@launch
            if (!settings.restTimerEnabled) return@launch
            _rest.value = RestState(settings.restSeconds, settings.restSeconds, nextLabel)
            while ((_rest.value?.remaining ?: 0) > 0) {
                delay(1000)
                if (_sessionClock.value.isPaused) continue
                _rest.update { it?.copy(remaining = (it.remaining - 1).coerceAtLeast(0)) }
            }
            delay(4000) // stays at 0 long enough to buzz and read
            _rest.value = null
        }
    }

    /** Adds or removes seconds on the running rest and remembers the new length as the default. */
    fun adjustRest(deltaSeconds: Int) {
        val r = _rest.value ?: return
        if (r.remaining <= 0) return
        val left = (r.remaining + deltaSeconds).coerceAtLeast(0)
        _rest.value = r.copy(remaining = left, total = maxOf(r.total, left))
        updateSettings { it.copy(restSeconds = (it.restSeconds + deltaSeconds).coerceIn(15, 600)) }
    }

    fun skipRest() {
        restJob?.cancel()
        _rest.value = null
    }

    /** Logs a set from the check button, then starts the rest timer when it is switched on. */
    fun completeSet(logId: String, sessionExerciseId: String, reps: Int, weightKg: Double) {
        updateWorkoutSetLogDirectly(logId, sessionExerciseId, reps, weightKg, isCompleted = true)
        val names = exerciseLibrary.value.associate { it.id to it.name }
        val exercises = _activeSessionExercises.value
        val logs = _workoutSetLogs.value
        val here = logs[sessionExerciseId].orEmpty().firstOrNull { !it.isCompleted && it.id != logId }
        val label = if (here != null) {
            val ex = exercises.firstOrNull { it.id == sessionExerciseId }
            "Next: ${names[ex?.exerciseId] ?: "this exercise"}, set ${here.setIndex}"
        } else {
            val after = exercises.firstOrNull { e -> e.id != sessionExerciseId && logs[e.id].orEmpty().any { !it.isCompleted } }
            if (after != null) "Next: ${names[after.exerciseId] ?: "next exercise"}, set 1" else "Last set done"
        }
        startRest(label)
    }

    /** Steps one set's weight or reps. Sets after it that are not logged yet follow, so one change carries down the list. */
    fun setSetValues(sessionExerciseId: String, logId: String, reps: Int, weightKg: Double) {
        val logs = _workoutSetLogs.value[sessionExerciseId] ?: return
        val idx = logs.indexOfFirst { it.id == logId }
        if (idx == -1) return
        val updated = logs.mapIndexed { i, l ->
            if (i == idx || (i > idx && !l.isCompleted)) l.copy(reps = reps.coerceAtLeast(0), weightKg = weightKg.coerceAtLeast(0.0)) else l
        }
        _workoutSetLogs.value = _workoutSetLogs.value.toMutableMap().apply { put(sessionExerciseId, updated) }
        viewModelScope.launch { updated.filterIndexed { i, l -> l != logs[i] }.forEach { dao.updateWorkoutSetLog(it) } }
    }

    fun uncompleteSet(logId: String, sessionExerciseId: String) {
        val log = _workoutSetLogs.value[sessionExerciseId]?.firstOrNull { it.id == logId } ?: return
        updateWorkoutSetLogDirectly(logId, sessionExerciseId, log.reps, log.weightKg, isCompleted = false)
    }

    fun dismissWorkoutSummary() {
        if (_workoutSummary.value == null) return
        _workoutSummary.value = null
        viewModelScope.launch { offerLink(LinkEvent.WORKOUT) } // the ask-first prompt comes after the summary
    }

    private suspend fun restoreActiveSessionFromPrefs() {
        val sessionId = prefs.getString("active_session_id", null) ?: return
        val session = dao.getWorkoutSessionById(sessionId)?.takeIf { it.endedAt == null && !it.isDeleted }
        if (session == null) { clearActiveSessionFromPrefs(); return }
        _currentActiveSession.value = session
        _sessionClock.value = if (prefs.contains("active_clock_acc_ms"))
            SessionClock.fromStored(prefs.getLong("active_clock_acc_ms", 0L), prefs.getLong("active_clock_since_ms", -1L))
        else SessionClock.startedAt(System.currentTimeMillis()) // session saved before the clock existed
        val exercises = dao.getWorkoutSessionExercises(session.id)
        loadExerciseHistory(session.id, exercises.map { it.exerciseId })
        _activeSessionExercises.value = exercises
        val logsMap = mutableMapOf<String, List<WorkoutSetLog>>()
        exercises.forEach { se -> logsMap[se.id] = dao.getWorkoutSetLogs(se.id) }
        _workoutSetLogs.value = logsMap
    }

    // FEATURE 10: Exercise Custom Notes
    fun loadCustomNoteForExercise(exerciseId: String) {
        viewModelScope.launch {
            val note = dao.getExerciseCustomNote(exerciseId)
            if (note != null) {
                val updated = _exerciseCustomNotes.value.toMutableMap()
                updated[exerciseId] = note.customNote
                _exerciseCustomNotes.value = updated
            }
        }
    }

    fun saveCustomNoteForExercise(exerciseId: String, note: String) {
        val iso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
        viewModelScope.launch {
            dao.upsertExerciseCustomNote(ExerciseCustomNote(exerciseId, note, iso))
            val updated = _exerciseCustomNotes.value.toMutableMap()
            updated[exerciseId] = note
            _exerciseCustomNotes.value = updated
        }
    }

    fun resetCustomNoteForExercise(exerciseId: String) {
        viewModelScope.launch {
            dao.deleteExerciseCustomNote(exerciseId)
            val updated = _exerciseCustomNotes.value.toMutableMap()
            updated.remove(exerciseId)
            _exerciseCustomNotes.value = updated
        }
    }

    // Finish Workout: saves the session with its active time, stores next-time targets and shows the summary (see FinishWorkoutUseCase)
    fun finishWorkout() {
        val session = _currentActiveSession.value ?: return
        if (finishingWorkout) return
        finishingWorkout = true
        val endedIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
        val seconds = _sessionClock.value.elapsedSeconds(System.currentTimeMillis())
        val exercises = _activeSessionExercises.value
        val logs = _workoutSetLogs.value
        val names = exerciseLibrary.value.associate { it.id to it.name }
        viewModelScope.launch {
            try {
                val summary = FinishWorkoutUseCase(db, dao)(
                    session, exercises, logs, seconds, endedIso, names, DayBoundaryManager.currentTargetDate.value, cardio = _cardioDraft.value
                )
                _currentActiveSession.value = null
                _activeSessionExercises.value = emptyList()
                _workoutSetLogs.value = emptyMap()
                skipRest()
                clearActiveSessionFromPrefs() // FIX 6
                _workoutSummary.value = summary
                AnalyticsComputationService.recalculateDayCache(dao, session.date)
            } finally {
                finishingWorkout = false
            }
        }
    }

    // 7. TODO STREAM MANAGER
    fun addTodo(title: String, priority: String, goalType: String = "SHORT_TERM") {
        val todayIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
        viewModelScope.launch {
            val item = Todo(
                id = UUID.randomUUID().toString(),
                title = title,
                priority = priority,
                goalType = goalType,          // FEATURE 11
                isCompleted = false,
                isDeleted = false,
                createdAt = todayIso,
                updatedAt = todayIso
            )
            dao.insertTodo(item)
        }
    }

    fun toggleTodo(todo: Todo) {
        val todayKey = DayBoundaryManager.currentTargetDate.value
        viewModelScope.launch {
            val todayIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
            GoalService.setCompleted(dao, todo, !todo.isCompleted, todayKey, todayIso)
        }
    }

    /** Sets or clears (null) the weekly target of a goal. */
    fun setGoalWeeklyTarget(todo: Todo, target: Int?) {
        viewModelScope.launch {
            val iso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
            dao.insertTodo(todo.copy(weeklyTarget = target?.takeIf { it in 1..7 }, updatedAt = iso))
        }
    }

    // Days each goal was completed this week (Monday to Sunday), for progress bars
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val goalWeekCounts: StateFlow<Map<String, Int>> = DayBoundaryManager.currentTargetDate
        .flatMapLatest { today -> WeekKey.of(today).let { dao.getGoalCompletionsBetween(it, WeekKey.addDays(it, 6)) } }
        .map { rows -> rows.groupBy { it.todoId }.mapValues { it.value.size } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun deleteTodo(todo: Todo) {
        viewModelScope.launch {
            val todayIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
            dao.insertTodo(todo.copy(isDeleted = true, updatedAt = todayIso))
        }
    }

    // FEATURE 11: Inline edit support
    fun updateTodoTitle(todo: Todo, newTitle: String) {
        viewModelScope.launch {
            val iso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
            dao.insertTodo(todo.copy(title = newTitle, updatedAt = iso))
        }
    }

    // FEATURE 11: goals reset at the day or week boundary, with their history recorded first
    private fun resetCompletedGoalsOnRollover(todayKey: String) {
        viewModelScope.launch {
            val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
            GoalService.resetAtBoundary(dao, todayKey, nowIso)
        }
    }

    // 9. VACATION SERVICE MODES
    fun toggleVacationMode() {
        val todayKey = DayBoundaryManager.currentTargetDate.value
        val isVac = !_isVacationActive.value
        _isVacationActive.value = isVac
        
        val todayIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())

        viewModelScope.launch {
            val day = VacationDay(
                id = UUID.randomUUID().toString(),
                date = todayKey,
                isDeleted = !isVac,
                createdAt = todayIso
            )
            dao.insertVacationDay(day)
            dao.getAppSettingsDirectly()?.let { dao.insertAppSettings(it.copy(vacationSince = if (isVac) todayKey else null)) }

            val routine = dao.getDailyRoutineDirectly(todayKey)
            if (routine != null) {
                dao.insertDailyRoutine(routine.copy(isVacation = isVac))
            }

            AnalyticsComputationService.recalculateDayCache(dao, todayKey)
        }
    }

    /** While vacation is on, every day since it started gets a vacation row, even days the app was not opened. */
    private suspend fun catchUpVacation(todayKey: String) {
        val since = dao.getAppSettingsDirectly()?.vacationSince ?: return
        val iso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
        val missing = WeekKey.range(since, todayKey).filter { dao.getVacationDayByDate(it) == null }
        if (missing.isEmpty()) return
        dao.insertVacationDays(missing.map { VacationDay(UUID.randomUUID().toString(), it, false, iso) })
        AnalyticsComputationService.recalculateRange(dao, missing.first(), WeekKey.addDays(todayKey, -1))
    }

    // ROUTINE EDITING (templates + today's list). Callers ask "apply to today?" and then call rebuildTodayRoutine().
    val routineTemplates: StateFlow<List<RoutineTemplate>> =
        dao.getRoutineTemplatesFlow().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun templateItems(templateId: String): Flow<List<RoutineTemplateItem>> = dao.getRoutineTemplateItemsFlow(templateId)

    fun saveRoutineTemplate(t: RoutineTemplate) { viewModelScope.launch { dao.insertRoutineTemplate(t) } }

    fun deleteRoutineTemplate(id: String) { viewModelScope.launch { dao.deleteRoutineTemplate(id) } }

    // JOURNEY (see JourneyRules and JourneyUseCase)
    private val journey = JourneyUseCase(db, dao)
    val journeyState: StateFlow<JourneyState?> = dao.getJourneyStateFlow().stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val journeyGoals: StateFlow<List<Goal>> = dao.getGoalsFlow().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** On every resume: applies the days that have ended since last time, then decides goals whose window has passed. A failure is logged and leaves the data as it was. */
    fun syncJourney() {
        viewModelScope.launch {
            val today = DayBoundaryManager.currentTargetDate.value
            try { journey.sync(today); journey.review(today) } catch (e: Exception) { Log.e("Journey", "sync failed", e) }
        }
    }

    fun setJourneyEnabled(on: Boolean, characterId: String = "max") {
        viewModelScope.launch {
            if (on) journey.enable(DayBoundaryManager.currentTargetDate.value, characterId) else journey.disable()
            JourneyReminderScheduler.scheduleAll(getApplication()) // the five reminders follow the Journey on or off
        }
    }

    private val _journeyReminders = MutableStateFlow(JourneyReminderScheduler.settings(application))
    val journeyReminders: StateFlow<JourneyReminderSettings> = _journeyReminders.asStateFlow()

    fun saveJourneyReminders(s: JourneyReminderSettings) {
        _journeyReminders.value = s
        if (s.master) requestNotificationPermission() // the reminders are silent until Android 13+ permission is granted
        viewModelScope.launch { JourneyReminderScheduler.save(getApplication(), s) }
    }

    /** Saves a commitment for the character's current region. Returns what blocks it, or an empty list once saved. */
    suspend fun saveCommitment(draft: CommitmentDraft): List<DraftError> {
        val region = journeyState.value?.regionIndex ?: return listOf(DraftError.WINDOW)
        val existing = journeyGoals.value.count { it.kind == "COMMITMENT" && it.regionIndex == region && it.status != "MISSED" }
        val errors = CommitmentRules.validate(draft, region, existing)
        if (errors.isEmpty()) journey.saveGoal(CommitmentRules.toGoal(draft, UUID.randomUUID().toString(), region, DayBoundaryManager.currentTargetDate.value))
        return errors
    }

    // Per-device UI memory: reviews the user has read, and where the character stood when the Journey was last open (so the walk can animate)
    private val _seenReviews = MutableStateFlow(prefs.getStringSet("journey_seen_reviews", emptySet())?.toSet() ?: emptySet())
    val journeySeenReviews: StateFlow<Set<String>> = _seenReviews.asStateFlow()

    fun markReviewSeen(goalId: String) {
        _seenReviews.value = _seenReviews.value + goalId
        prefs.edit().putStringSet("journey_seen_reviews", _seenReviews.value).apply()
    }

    /** (region, day) from the last time the Journey was open, or null. */
    fun journeyLastSeen(): Pair<Int, Int>? =
        prefs.getString("journey_seen_pos", null)?.split(":")?.takeIf { it.size == 2 }?.let { p -> p[0].toIntOrNull()?.let { it to (p[1].toIntOrNull() ?: 0) } }

    fun markJourneySeen(region: Int, step: Int) { prefs.edit().putString("journey_seen_pos", "$region:$step").apply() }

    fun crossJourneyGate(regionName: String) { viewModelScope.launch { journey.crossGate(DayBoundaryManager.currentTargetDate.value, regionName) } }

    fun saveAmbition(ambition: String?, identity: String?) {
        viewModelScope.launch { journey.saveAmbition(ambition, identity, DayBoundaryManager.currentTargetDate.value) }
    }

    fun setJourneyCharacter(id: String) { viewModelScope.launch { journey.setCharacter(id) } }

    fun claimJourneyReward(goalId: String) { viewModelScope.launch { journey.claimReward(goalId, DayBoundaryManager.currentTargetDate.value) } }

    suspend fun goalProgress(goal: Goal): Double = journey.progressOf(goal)

    fun addGoalProgress(goalId: String, amount: Double, note: String?) {
        viewModelScope.launch { journey.addProgress(goalId, DayBoundaryManager.currentTargetDate.value, amount, note) }
    }

    // ROUTINE IMPORT (see RoutineImport and RoutineImportUseCase)
    private val _routineImportRequested = MutableStateFlow(false)
    /** Set by onboarding's "Import my own routine with AI": the Routine tab opens the import with Replace preselected. */
    val routineImportRequested: StateFlow<Boolean> = _routineImportRequested
    fun requestRoutineImport() { _routineImportRequested.value = true }
    fun clearRoutineImportRequest() { _routineImportRequested.value = false }

    /** Supplement names the user has logged, in the order first logged, so the prompt can match auto-tick by name. */
    suspend fun loggedSupplementNames(): List<String> =
        dao.getAllSupplementLogs().sortedBy { it.loggedAt }.map { it.supplementName }.distinct()

    suspend fun routineImportPrompt(): String = RoutineImport.prompt(loggedSupplementNames())

    suspend fun routineExportText(): String =
        RoutineImport.exportPrompt(loggedSupplementNames(), dao.getRoutineTemplates().sortedBy { it.name }, dao.getAllRoutineTemplateItems())

    /** Saves the previewed groups, rebuilds today's list and returns (groups, items) saved. A failure leaves the old routine untouched. */
    suspend fun importRoutines(groups: List<ImportedGroup>, replace: Boolean): Pair<Int, Int> {
        val saved = RoutineImportUseCase(db, dao)(groups, replace)
        rebuildTodayRoutine()
        return saved
    }

    fun saveTemplateItem(item: RoutineTemplateItem) { viewModelScope.launch { dao.insertRoutineTemplateItems(listOf(item)) } }

    fun deleteTemplateItem(id: String) { viewModelScope.launch { dao.deleteRoutineTemplateItem(id) } }

    private suspend fun refreshTodayCompletion() {
        val dateKey = DayBoundaryManager.currentTargetDate.value
        AnalyticsComputationService.recalculateDayCache(dao, dateKey)
    }

    fun saveTodayItem(item: DailyRoutineItem) {
        viewModelScope.launch {
            dao.insertDailyRoutineItem(item)
            refreshTodayCompletion()
        }
    }

    fun addTodayItem(title: String, category: String, time: String?) {
        viewModelScope.launch {
            val r = dao.getDailyRoutineDirectly(DayBoundaryManager.currentTargetDate.value) ?: return@launch
            val next = (dao.getDailyRoutineItems(r.id).maxOfOrNull { it.orderIndex } ?: 0) + 1
            dao.insertDailyRoutineItem(DailyRoutineItem(UUID.randomUUID().toString(), r.id, title, category, next, time))
            refreshTodayCompletion()
        }
    }

    fun deleteTodayItem(id: String) {
        viewModelScope.launch {
            dao.deleteDailyRoutineItem(id)
            refreshTodayCompletion()
        }
    }

    fun moveTodayItem(index: Int, delta: Int) {
        val items = _activeRoutineItems.value
        val j = index + delta
        if (j !in items.indices) return
        viewModelScope.launch {
            dao.updateDailyRoutineItem(items[index].copy(orderIndex = items[j].orderIndex))
            dao.updateDailyRoutineItem(items[j].copy(orderIndex = items[index].orderIndex))
        }
    }

    suspend fun getAppSettings(): AppSettings? = dao.getAppSettingsDirectly()

    /** Stored onboarding answers; vitamins come from the starter Supplements routine. */
    suspend fun currentSetupAnswers(): SetupAnswers {
        val s = dao.getAppSettingsDirectly()
        val starterIds = dao.getRoutineTemplates().filter { it.isStarter }.map { it.id }.toSet()
        return SetupAnswers(
            wakeTime = s?.wakeTime ?: "07:00",
            sleepTime = s?.sleepTime ?: "23:00",
            workoutDays = RoutineSchedule.parseDays(s?.workoutDays ?: "[1,3,5]"),
            vitamins = dao.getAllRoutineTemplateItems()
                .filter { it.templateId in starterIds && it.category == "SUPPLEMENT" }
                .map { VitaminChoice(it.title, it.scheduledTime) }
                .distinctBy { it.name }
        )
    }

    /** Rebuilds the starter routines from the stored answers; user-created routines stay. */
    fun resetRoutinesToDefaults() {
        viewModelScope.launch {
            DatabaseSeeder.seedStarterRoutines(dao, currentSetupAnswers())
            rebuildTodayRoutine()
        }
    }

    // 10. MAINTENANCE & RECOVERY PATHWAYS
    fun resetTodayRoutine() {
        val dateKey = DayBoundaryManager.currentTargetDate.value
        viewModelScope.launch {
            val existing = dao.getDailyRoutineDirectly(dateKey)
            if (existing != null) {
                dao.clearDailyRoutineItemsForDate(dateKey)
                dao.clearDailyRoutineForDate(dateKey)
            }
            initializeTodayRoutine(dateKey, _isVacationActive.value)
            loadTodayData(dateKey)
        }
    }

    // FEATURE 13: Force-refresh analytics from DB
    fun refreshAnalytics() {
        viewModelScope.launch {
            val dateKey = DayBoundaryManager.currentTargetDate.value
            AnalyticsComputationService.recalculateDayCache(dao, dateKey)
            _analyticsLastRefreshed.value = System.currentTimeMillis()
        }
    }

    // FEATURE 14: Manual backup test with result feedback
    fun triggerTestBackup() {
        if (_isCloudBackupSyncInProgress.value) return
        
        _isCloudBackupSyncInProgress.value = true
        _backupTestResult.value = null
        viewModelScope.launch {
            try {
                val settings = dao.getAppSettingsDirectly()
                val userEmail = settings?.firebaseUid ?: _user.value
                
                if (userEmail == null) {
                    _backupTestResult.value = "FAILED: User not authenticated"
                    return@launch
                }

                val result = CloudBackupService.performBackup(getApplication(), dao, userEmail)
                result.fold(
                    onSuccess = { summary ->
                        _lastSuccessfulTransmissionTimestamp.value = summary.timestampIso
                        _backupTestResult.value = "SUCCESS"
                        Log.d("SyncWellAuth", "Backup successful for $userEmail: ${summary.workoutSessionsCount} workouts, ${summary.todosCount} todos")
                    },
                    onFailure = { e ->
                        if (!requestDriveConsentIfNeeded(e) { triggerTestBackup() }) {
                            Log.e("SyncWellAuth", "Backup failed", e)
                            _backupTestResult.value = "FAILED: ${e.message}"
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e("SyncWellAuth", "Backup failed", e)
                _backupTestResult.value = "FAILED: ${e.message}"
            } finally {
                _isCloudBackupSyncInProgress.value = false
                // Clear result after 3s
                delay(3000)
                _backupTestResult.value = null
            }
        }
    }

    // Keep old name as alias for Settings screen backward compat
    fun forceManualBackup() = triggerTestBackup()

    // FEATURE 14: Schedule daily 9PM backup (also re-set after a restart by BootReceiver)
    fun scheduleDailyBackupAt9PM() = ReminderScheduler.scheduleBackup9PM(getApplication())

    // GENTLE REMINDERS: off until the user turns them on, and the permission is asked for then
    private val _remindersEnabled = MutableStateFlow(ReminderScheduler.isEnabled(application))
    val remindersEnabled: StateFlow<Boolean> = _remindersEnabled.asStateFlow()

    private val _notificationPermissionRequest = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val notificationPermissionRequest: SharedFlow<Unit> = _notificationPermissionRequest.asSharedFlow()

    fun requestNotificationPermission() { _notificationPermissionRequest.tryEmit(Unit) }

    fun setRemindersEnabled(on: Boolean) {
        prefs.edit().putBoolean("reminders_enabled", on).apply()
        _remindersEnabled.value = on
        if (on) requestNotificationPermission()
        viewModelScope.launch { ReminderScheduler.scheduleAll(getApplication()) }
    }

    // HYDRATION (Water tracker additions)
    fun addWaterLog(amountMl: Int) {
        val todayKey = DayBoundaryManager.currentTargetDate.value
        val todayIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
        viewModelScope.launch {
            val before = dao.getWaterLogs(todayKey).first().sumOf { it.amountMl }
            val entry = WaterLog(
                id = UUID.randomUUID().toString(),
                amountMl = amountMl,
                loggedAt = todayIso,
                dateKey = todayKey,
                isDeleted = false
            )
            dao.insertWaterLog(entry)
            AnalyticsComputationService.recalculateDayCache(dao, todayKey)
            val goal = _waterGoalMl.value
            if (before < goal && before + amountMl >= goal) offerLink(LinkEvent.WATER) // only the log that reaches the goal
        }
    }

    fun reduceWaterLog() {
        val todayKey = DayBoundaryManager.currentTargetDate.value
        viewModelScope.launch {
            val logs = _todayWaterLogs.value.filter { !it.isDeleted }
            if (logs.isNotEmpty()) {
                val lastLog = logs.lastOrNull()
                if (lastLog != null) {
                    val updated = lastLog.copy(isDeleted = true)
                    dao.insertWaterLog(updated)
                    AnalyticsComputationService.recalculateDayCache(dao, todayKey)
                }
            }
        }
    }

    fun createNewCustomExerciseAndAdd(name: String, category: String) {
        val uniqueId = "custom_" + UUID.randomUUID().toString().take(8)
        val newItem = ExerciseLibraryItem(
            id = uniqueId,
            name = name,
            category = category.uppercase(),
            defaultSets = 3,
            defaultRepsMin = 8,
            defaultRepsMax = 12,
            defaultRestSec = 90,
            homeAlternativeId = null,
            alternatingGroupId = null
        )
        viewModelScope.launch {
            dao.insertExerciseLibrary(listOf(newItem))
            addExerciseToActiveSession(uniqueId)
        }
    }

    // WEIGHT LOG LOGGER
    fun logWeight(weight: Double) {
        val todayKey = DayBoundaryManager.currentTargetDate.value
        val todayIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
        viewModelScope.launch {
            val log = WeightLog(
                id = UUID.randomUUID().toString(),
                dateKey = todayKey,
                weightKg = weight,
                isDeleted = false,
                loggedAt = todayIso
            )
            dao.insertWeightLog(log)
            AnalyticsComputationService.recalculateDayCache(dao, todayKey)
        }
    }

    // INJECT MANUAL STEPS (OR SYNC PLATFORM HEALTH CONNECT)
    fun syncStepsWithHealthConnect() {
        val todayKey = DayBoundaryManager.currentTargetDate.value
        viewModelScope.launch {
            val steps = HealthConnectService.readDailySteps(todayKey)
            val todayIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
            val stepsLog = StepsLog(
                id = UUID.randomUUID().toString(),
                dateKey = todayKey,
                stepCount = steps,
                source = "HEALTH_CONNECT",
                updatedAt = todayIso
            )
            dao.insertStepsLog(stepsLog)
            AnalyticsComputationService.recalculateDayCache(dao, todayKey)
        }
    }

    fun saveManualSteps(steps: Int) {
        val todayKey = DayBoundaryManager.currentTargetDate.value
        val todayIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
        viewModelScope.launch {
            val stepsLog = StepsLog(
                id = UUID.randomUUID().toString(),
                dateKey = todayKey,
                stepCount = steps,
                source = "MANUAL",
                updatedAt = todayIso
            )
            dao.insertStepsLog(stepsLog)
            AnalyticsComputationService.recalculateDayCache(dao, todayKey)
        }
    }

    // ADD SUPPLEMENT LOG WITH CIRCADIAN CUTOFF VERIFICATIONS
    fun addSupplement(name: String, onLateWarning: () -> Unit) {
        val todayKey = DayBoundaryManager.currentTargetDate.value
        val nowMs = System.currentTimeMillis()
        val todayIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())

        viewModelScope.launch {
            var lateWarning = false
            if (name.contains("D3", ignoreCase = true) || name.contains("K2", ignoreCase = true)) {
                if (!SupplementValidator.processD3K2Log(nowMs)) {
                    lateWarning = true
                }
            }

            val entry = SupplementLog(
                id = UUID.randomUUID().toString(),
                supplementName = name,
                loggedAt = todayIso,
                lateLogWarningShown = lateWarning,
                dateKey = todayKey
            )
            dao.insertSupplementLog(entry)
            offerLink(LinkEvent.SUPPLEMENT, name)

            if (lateWarning) {
                onLateWarning()
            }
        }
    }
}

// Bump when medals are added: the first run after a bump records what is already earned without popups
private const val BADGE_CATALOGUE_VERSION = 2
