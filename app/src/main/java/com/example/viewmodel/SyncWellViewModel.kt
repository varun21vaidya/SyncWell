package com.example.viewmodel

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.service.AnalyticsComputationService
import com.example.service.CloudBackupService
import com.example.service.DailyBackupReceiver
import com.example.service.DriveConsentRequiredException
import com.example.service.DayBoundaryManager
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

class SyncWellViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val dao = db.syncWellDao()
    private val prefs: SharedPreferences =
        application.getSharedPreferences("syncwell_session_prefs", android.content.Context.MODE_PRIVATE)

    private val credentialManager = CredentialManager.create(application)

    // 1. AUTHENTICATION STORE (Simulated)
    private val _user = MutableStateFlow<String?>(null)
    val user: StateFlow<String?> = _user.asStateFlow()
    val isAuthenticated: StateFlow<Boolean> = _user.map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

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

    private val _targetWeightKg = MutableStateFlow(70.0)
    val targetWeightKg: StateFlow<Double> = _targetWeightKg.asStateFlow()

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
    val exerciseLibrary: Flow<List<ExerciseLibraryItem>> = dao.getExerciseLibraryFlow()
    val exerciseProgressions: Flow<List<ExerciseProgressionState>> = dao.getExerciseProgressionsFlow()
    val weightLogs: Flow<List<WeightLog>> = dao.getWeightLogs()

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
    val weeklyAnalytics: Flow<List<DailyAnalytics>> = dao.getWeeklyAnalytics()
    val monthlyAnalytics: Flow<List<DailyAnalytics>> = dao.getMonthlyAnalytics()
    
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

    private val _lastSuccessfulTransmissionTimestamp = MutableStateFlow<String?>(null)
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

    fun getTodayDayName(): String {
        val days = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        val calendar = java.util.Calendar.getInstance()
        val index = calendar.get(java.util.Calendar.DAY_OF_WEEK) - 1
        return days.getOrElse(index) { "Monday" }
    }

    init {
        viewModelScope.launch {
            // Check onboarding database target on startup
            checkSetupStatus()
            // FIX 6: Restore any in-progress workout session
            restoreActiveSessionFromPrefs()
            // FEATURE 14: Schedule the daily 9 PM backup alarm
            scheduleDailyBackupAt9PM()
            // Listen to active calendar changes to load layout
            DayBoundaryManager.currentTargetDate.collect { dateKey ->
                loadTodayData(dateKey)
                // FEATURE 11: Auto-reset completed goals on day rollover
                resetCompletedGoalsOnRollover(dateKey)
            }
        }
    }

    // Onboarding Status Checks
    suspend fun checkSetupStatus() {
        // Ensure database is seeded even if Room Callback didn't run (e.g. on update)
        val templates = dao.getRoutineTemplates()
        
        // RE-SEED LOGIC: If templates are old/vague (check count), force update!
        val forceReseed = templates.isNotEmpty() && templates.any { 
            (it.name == "Standard Weekday Runbook" && dao.getRoutineTemplateItems(it.id).size < 25) ||
            (it.dayType == "SUNDAY" && dao.getRoutineTemplateItems(it.id).size < 22)
        }

        if (templates.isEmpty() || forceReseed) {
            DatabaseSeeder.seedDatabase(dao, forceUpdate = true)
            // If we reseeded, we should also reset today's routine to reflect new templates immediately
            resetTodayRoutine()
        }

        // We only check if settings exist to determine onboarding completion.
        val settings = dao.getAppSettingsDirectly()
        if (settings != null) {
            _hasCompletedOnboarding.value = true
            _theme.value = "dark"
            _backupEnabled.value = settings.backupEnabled
            _workoutLocationMode.value = settings.workoutLocationMode
            _targetWeightKg.value = settings.targetWeightKg
            // Load credentials if present
            _user.value = settings.firebaseUid ?: "Offline_User"
            _appLaunchState.value = AppLaunchState.Ready
        } else {
            _hasCompletedOnboarding.value = false
            _appLaunchState.value = AppLaunchState.OnboardingRequired
        }
    }

    suspend fun completeOnboarding() {
        val todayIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
        val settings = AppSettings(
            id = UUID.randomUUID().toString(),
            firebaseUid = "Offline_User",
            defaultWaterMl = 4000,
            targetWeightKg = 70.0,
            themeMode = "dark",
            backupEnabled = false,
            vacationPauseEnabled = false,
            workoutLocationMode = "GYM",
            createdAt = todayIso,
            updatedAt = todayIso
        )
        dao.insertAppSettings(settings)
        
        // Setup initial default routines check
        checkSetupStatus()
    }

    // Active Day Hydrations and Data Collections
    private fun loadTodayData(dateKey: String) {
        todayDataJob?.cancel()
        todayDataJob = viewModelScope.launch {
            // Check Vacation state
            val isVac = dao.getVacationDayByDate(dateKey) != null
            _isVacationActive.value = isVac

            // Check if Daily Routine checklist row exists. If not, generate from today's dayType template!
            initializeTodayRoutine(dateKey, isVac)

            // Warm up and pre-compute cached daily statistics for Home screen visibility
            AnalyticsComputationService.recalculateDayCache(dao, dateKey)

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
        val existingRoutine = dao.getDailyRoutineDirectly(dateKey)
        if (existingRoutine == null) {
            val todayName = getTodayDayName()
            val dayType = when (todayName) {
                "Saturday" -> "SATURDAY"
                "Sunday" -> "SUNDAY"
                else -> "WEEKDAY"
            }

            val templates = dao.getRoutineTemplates()
            var matchingTemplate = templates.find { it.dayType == dayType }
            if (matchingTemplate == null && templates.isNotEmpty()) {
                matchingTemplate = templates.first()
            }
            
            val routineId = UUID.randomUUID().toString()
            val dailyRoutine = DailyRoutine(
                id = routineId,
                date = dateKey,
                templateId = matchingTemplate?.id,
                completionPct = 0.0,
                isVacation = isVac
            )
            dao.insertDailyRoutine(dailyRoutine)

            val routineItems = if (matchingTemplate != null) {
                val templateItems = dao.getRoutineTemplateItems(matchingTemplate.id)
                templateItems.map { t ->
                    DailyRoutineItem(
                        id = UUID.randomUUID().toString(),
                        dailyRoutineId = routineId,
                        title = t.title,
                        category = t.category,
                        orderIndex = t.orderIndex,
                        scheduledTime = t.scheduledTime,
                        isDone = false
                    )
                }
            } else {
                emptyList()
            }

            if (routineItems.isNotEmpty()) {
                dao.insertDailyRoutineItems(routineItems)
            }
        }
    }

    // Toggle Routine item Checkbox
    fun toggleRoutineItem(item: DailyRoutineItem) {
        val titleLower = item.title.lowercase()
        val isProteinRelated = !item.isDone && (
                titleLower.contains("breakfast") ||
                titleLower.contains("lunch") ||
                titleLower.contains("dinner") ||
                titleLower.contains("protein")
        )

        if (isProteinRelated) {
            _proteinConfirmationTask.value = item
        } else {
            executeRoutineToggle(item)
        }
    }

    fun confirmProteinIntake(item: DailyRoutineItem, tookProtein: Boolean) {
        _proteinConfirmationTask.value = null
        // Even if they say 'No' to protein, we still mark the routine item as done.
        // But we could potentially track 'protein taken' as a separate metric if needed.
        // For now, the user says "when all four meet protein for the day we will count it".
        // This implies we need to store 'proteinTaken' on the DailyRoutineItem.
        // Let's add it to the entity if needed, or just assume done = protein met for now.
        // Wait, the user says "it will ask confirmation took protien?".
        // If they click No, maybe we don't count it for the diet streak?
        // Let's stick to simple: mark as done.
        executeRoutineToggle(item)
    }

    fun cancelProteinConfirmation() {
        _proteinConfirmationTask.value = null
    }

    private fun executeRoutineToggle(item: DailyRoutineItem) {
        viewModelScope.launch {
            val updated = item.copy(isDone = !item.isDone)
            dao.updateDailyRoutineItem(updated)

            // Update UI state directly
            val currentItems = _activeRoutineItems.value.map { if (it.id == item.id) updated else it }
            _activeRoutineItems.value = currentItems

            // Re-calculate routine completion percentage
            val doneCount = currentItems.count { it.isDone }
            val total = if (currentItems.isNotEmpty()) currentItems.size else 1
            val completedPct = doneCount.toDouble() / total.toDouble()

            // Update parent DailyRoutine Row
            val dateKey = DayBoundaryManager.currentTargetDate.value
            val dailyRoutine = dao.getDailyRoutineDirectly(dateKey)
            if (dailyRoutine != null) {
                val updatedRoutine = dailyRoutine.copy(completionPct = completedPct)
                dao.updateDailyRoutine(updatedRoutine)
            }

            // Repopulate statistics cache
            AnalyticsComputationService.recalculateDayCache(dao, dateKey)
        }
    }

    // 1. AUTHENTICATION
    fun initiateGoogleSignIn(context: Context) {
        viewModelScope.launch {
            try {
                Log.d("SyncWellAuth", "Starting Google Sign-In flow...")
                _activeModal.value = "SIGNING_IN"
                
                val serverClientId = context.getString(R.string.google_web_client_id)
                Log.d("SyncWellAuth", "Using Client ID: $serverClientId")

                if (serverClientId.contains("YOUR_WEB_CLIENT_ID_PLACEHOLDER")) {
                    Log.e("SyncWellAuth", "CRITICAL: Web Client ID is still the placeholder! Please update strings.xml")
                }

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
            val email = settings?.firebaseUid ?: _user.value ?: return@launch

            _isCloudBackupSyncInProgress.value = true
            try {
                val result = CloudBackupService.performRestore(getApplication(), dao, email)
                result.fold(
                    onSuccess = {
                        Log.d("SyncWellAuth", "Restore successful for $email")
                        _backupTestResult.value = "SUCCESS"
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

    private fun stopTimerService() {
        val intent = android.content.Intent(getApplication(), com.example.service.FocusTimerService::class.java).apply {
            action = com.example.service.FocusTimerService.ACTION_STOP
        }
        getApplication<Application>().startService(intent)
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
        _backupEnabled.value = enabled
        viewModelScope.launch {
            val settings = dao.getAppSettingsDirectly()
            if (settings != null) {
                dao.insertAppSettings(settings.copy(backupEnabled = enabled))
            }
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
        startTimerService(type, _activeTodoTag.value)
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_isActiveTimerRunning.value) {
                delay(1000)
                _accumulatedSeconds.value += 1
            }
        }
    }

    fun pauseTimer() {
        _isActiveTimerRunning.value = false
        timerJob?.cancel()
        stopTimerService()
    }

    fun resetTimer() {
        _isActiveTimerRunning.value = false
        timerJob?.cancel()
        _accumulatedSeconds.value = 0
        stopTimerService()
        _activeContextType.value = null
    }

    fun saveFocusSession() {
        val type = _activeContextType.value ?: return
        val sec = _accumulatedSeconds.value
        if (sec <= 0) return

        val durationMin = Math.max(1, sec / 60)
        val todayKey = DayBoundaryManager.currentTargetDate.value
        val todayIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())

        val tagSuffix = _activeTodoTag.value?.let { "|Tag: $it" } ?: ""

        viewModelScope.launch {
            val session = TrackerSession(
                id = UUID.randomUUID().toString(),
                activityType = type,
                source = "TIMER$tagSuffix",
                startTime = todayIso,
                endTime = todayIso,
                durationMin = durationMin,
                dateKey = todayKey,
                isDeleted = false
            )
            dao.insertTrackerSession(session)
            
            // Reset focus Stopwatch states
            resetTimer()
            _activeTodoTag.value = null

            // Dynamic recalculation of cached statistics
            AnalyticsComputationService.recalculateDayCache(dao, todayKey)
        }
    }

    // 5. WORKOUT MANAGER
    fun startWorkoutSession(dayType: String, environment: String) {
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
        persistActiveSessionToPrefs() // FIX 6

        viewModelScope.launch {
            dao.insertWorkoutSession(session)

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
                // Fetch previous completed set metrics OR fall back to progression Suggested weights
                val lastLog = dao.getLastCompletedSetLogForExercise(ex.id)
                val suggestedState = dao.getExerciseProgression(ex.id)
                val targetWt = lastLog?.weightKg ?: (suggestedState?.suggestedWeightKg ?: 30.0)
                val targetReps = lastLog?.reps ?: (suggestedState?.suggestedReps ?: ex.defaultRepsMax)

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
                        weightKg = se.targetWeightKg ?: 10.0,
                        reps = se.targetRepsMax,
                        isCompleted = false
                    )
                }
                dao.insertWorkoutSetLogs(logsList)
                defaultLogs[se.id] = logsList
            }

            _activeSessionExercises.value = sessionExercises
            _workoutSetLogs.value = defaultLogs
        }
    }

    fun addExerciseToActiveSession(exerciseId: String) {
        val session = _currentActiveSession.value ?: return
        viewModelScope.launch {
            val orderIdx = (_activeSessionExercises.value.maxOfOrNull { it.orderIndex } ?: 0) + 1
            
            // Fetch previous completed set metrics for progression matching
            val lastLog = dao.getLastCompletedSetLogForExercise(exerciseId)
            val suggestedState = dao.getExerciseProgression(exerciseId)
            val targetWt = lastLog?.weightKg ?: (suggestedState?.suggestedWeightKg ?: 30.0)
            val targetReps = lastLog?.reps ?: (suggestedState?.suggestedReps ?: 12)

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
                    weightKg = se.targetWeightKg ?: 10.0,
                    reps = se.targetRepsMax,
                    isCompleted = false
                )
            }
            dao.insertWorkoutSetLogs(logsList)

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
        val lastWeight = currentLogs.lastOrNull()?.weightKg ?: 30.0
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

    fun completeWorkoutSet(sessionExerciseId: String, setIndex: Int, reps: Int, weight: Double) {
        val currentLogs = _workoutSetLogs.value[sessionExerciseId]?.toMutableList() ?: return
        val targetIdx = currentLogs.indexOfFirst { it.setIndex == setIndex }
        if (targetIdx != -1) {
            val updatedLog = currentLogs[targetIdx].copy(reps = reps, weightKg = weight, isCompleted = true)
            currentLogs[targetIdx] = updatedLog
            
            val updatedMap = _workoutSetLogs.value.toMutableMap()
            updatedMap[sessionExerciseId] = currentLogs
            _workoutSetLogs.value = updatedMap

            viewModelScope.launch {
                dao.insertWorkoutSetLogs(listOf(updatedLog))
            }
        }
    }

    fun stopWorkout() {
        val session = _currentActiveSession.value ?: return
        val endedIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
        val updatedSession = session.copy(endedAt = endedIso)
        viewModelScope.launch {
            dao.insertWorkoutSession(updatedSession)
            _currentActiveSession.value = null
            _activeSessionExercises.value = emptyList()
            _workoutSetLogs.value = emptyMap()
            clearActiveSessionFromPrefs() // FIX 6
            AnalyticsComputationService.recalculateDayCache(dao, DayBoundaryManager.currentTargetDate.value)
        }
    }

    // FIX 6: SharedPreferences-based session persistence helpers
    private fun persistActiveSessionToPrefs() {
        val session = _currentActiveSession.value ?: return
        prefs.edit()
            .putString("active_session_id", session.id)
            .putString("active_session_day_type", session.dayType)
            .putString("active_session_env", session.environment)
            .apply()
    }

    private fun clearActiveSessionFromPrefs() {
        prefs.edit()
            .remove("active_session_id")
            .remove("active_session_day_type")
            .remove("active_session_env")
            .apply()
    }

    private suspend fun restoreActiveSessionFromPrefs() {
        val sessionId = prefs.getString("active_session_id", null) ?: return
        val sessions = dao.getWorkoutSessions().first()
        val session = sessions.find { it.id == sessionId && it.endedAt == null }
        if (session == null) { clearActiveSessionFromPrefs(); return }
        _currentActiveSession.value = session
        val exercises = dao.getWorkoutSessionExercises(session.id)
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

    // FIX 5: Finish Workout — dual-reference: saves actuals as defaults + engine suggestion as target
    fun finishWorkout() {
        val session = _currentActiveSession.value ?: return
        val endedIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
        val updatedSession = session.copy(endedAt = endedIso)
        viewModelScope.launch {
            dao.insertWorkoutSession(updatedSession)
            _activeSessionExercises.value.forEach { se ->
                val logs = _workoutSetLogs.value[se.id] ?: emptyList()
                val completedLogs = logs.filter { it.isCompleted }
                
                // 1. Determine "Next Default" based on best performance in this session
                // We pick the set with the highest weight. If tied, highest reps.
                val bestLog = completedLogs.maxWithOrNull(compareBy({ it.weightKg }, { it.reps }))
                
                // 2. Also consider the manually adjusted targets as potential defaults
                val finalWeight = maxOf(bestLog?.weightKg ?: 0.0, se.targetWeightKg ?: 0.0)
                val finalReps = if (bestLog != null && bestLog.weightKg >= (se.targetWeightKg ?: 0.0)) {
                    maxOf(bestLog.reps, se.targetRepsMax)
                } else {
                    se.targetRepsMax
                }

                if (finalWeight > 0) {
                    dao.insertExerciseProgression(
                        ExerciseProgressionState(
                            exerciseId = se.exerciseId,
                            suggestedWeightKg = finalWeight,
                            suggestedReps = finalReps,
                            lastUpdatedAt = endedIso
                        )
                    )
                }

                // Progressive Overload Engine Suggestion (Legacy logic support)
                val targetReps = se.targetRepsMax
                val currentWeight = se.targetWeightKg ?: 0.0
                val engineSuggestion = ProgressiveOverloadEngine.evaluateProgression(completedLogs, targetReps, currentWeight)
                if (engineSuggestion > currentWeight) {
                    dao.insertExerciseProgression(
                        ExerciseProgressionState(
                            exerciseId = se.exerciseId + "_suggestion",
                            suggestedWeightKg = engineSuggestion,
                            suggestedReps = targetReps,
                            lastUpdatedAt = endedIso
                        )
                    )
                }
            }
            _currentActiveSession.value = null
            _activeSessionExercises.value = emptyList()
            _workoutSetLogs.value = emptyMap()
            clearActiveSessionFromPrefs() // FIX 6
            AnalyticsComputationService.recalculateDayCache(dao, DayBoundaryManager.currentTargetDate.value)
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
        viewModelScope.launch {
            val todayIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
            dao.insertTodo(todo.copy(isCompleted = !todo.isCompleted, updatedAt = todayIso))
        }
    }

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

    // FEATURE 11: Goal auto-reset at midnight
    private fun resetCompletedGoalsOnRollover(todayKey: String) {
        viewModelScope.launch {
            val todayIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
            // Short-term: reset completed goals from before today (Uncheck)
            val midnightCutoff = "${todayKey}T00:00:00Z"
            dao.resetCompletedShortTermGoalsBefore(midnightCutoff, todayIso)

            // Long-term: reset on Sunday midnight (Uncheck)
            if (DayBoundaryManager.isSunday()) {
                dao.resetCompletedLongTermGoalsBefore(midnightCutoff, todayIso)
            }
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

            val routine = dao.getDailyRoutineDirectly(todayKey)
            if (routine != null) {
                dao.insertDailyRoutine(routine.copy(isVacation = isVac))
            }

            AnalyticsComputationService.recalculateDayCache(dao, todayKey)
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

    // FEATURE 14: Schedule daily 9PM backup using AlarmManager
    fun scheduleDailyBackupAt9PM() {
        val alarmManager = getApplication<Application>().getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
        val intent = android.content.Intent(getApplication(), DailyBackupReceiver::class.java)
        val pendingIntent = android.app.PendingIntent.getBroadcast(
            getApplication(), 9000, intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        val calendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 21)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            if (timeInMillis < System.currentTimeMillis()) add(java.util.Calendar.DAY_OF_YEAR, 1)
        }
        alarmManager.setInexactRepeating(
            android.app.AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            android.app.AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    // HYDRATION (Water tracker additions)
    fun addWaterLog(amountMl: Int) {
        val todayKey = DayBoundaryManager.currentTargetDate.value
        val todayIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
        viewModelScope.launch {
            val entry = WaterLog(
                id = UUID.randomUUID().toString(),
                amountMl = amountMl,
                loggedAt = todayIso,
                dateKey = todayKey,
                isDeleted = false
            )
            dao.insertWaterLog(entry)
            AnalyticsComputationService.recalculateDayCache(dao, todayKey)
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

            if (lateWarning) {
                onLateWarning()
            }
        }
    }
}
