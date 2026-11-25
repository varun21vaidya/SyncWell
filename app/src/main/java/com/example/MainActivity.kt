package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.domain.LinkEvent
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.ui.AppNavigationBar
import com.example.ui.BadgeStampOverlay
import com.example.ui.Destination
import com.example.ui.Tab
import com.example.ui.Overlay
import com.example.ui.AppNavHost
import com.example.ui.OverlayScreen
import com.example.ui.ShareCard
import com.example.ui.AnalyticsScreen
import com.example.ui.HomeScreen
import com.example.ui.OnboardingScreen
import com.example.ui.RoutinesScreen
import com.example.ui.SettingsScreen
import com.example.ui.TodosScreen
import com.example.ui.TrackerScreen
import com.example.ui.WorkoutScreen
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Emerald600
import com.example.ui.theme.OnProgress
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.Slate400
import com.example.ui.theme.OledBlack
import com.example.ui.theme.SurfaceTier1
import com.example.ui.theme.EmeraldGlow
import com.example.ui.theme.EmeraldDim
import com.example.ui.theme.TextMuted
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.size
import com.example.ui.SyncWellSplashScreen
import com.example.viewmodel.AppLaunchState
import com.example.viewmodel.SyncWellViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: SyncWellViewModel by viewModels()

    // Set by a notification tap or link ("destination" extra); consumed by the navigation container
    private val deepLink = mutableStateOf<Destination?>(null)

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLink.value = Destination.parse(intent.getStringExtra(EXTRA_DESTINATION))
    }

    companion object { const val EXTRA_DESTINATION = "destination" }

    private val driveConsentLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        viewModel.onDriveConsentResult(result.resultCode == RESULT_OK)
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        deepLink.value = Destination.parse(intent?.getStringExtra(EXTRA_DESTINATION))
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    // asked in context: when reminders are switched on or a focus timer starts, never at launch
                    viewModel.notificationPermissionRequest.collect {
                        if (android.os.Build.VERSION.SDK_INT >= 33 &&
                            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
                        ) notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                launch { viewModel.message.collect { android.widget.Toast.makeText(this@MainActivity, it, android.widget.Toast.LENGTH_SHORT).show() } }
                viewModel.driveConsentRequest.collect { pendingIntent ->
                    driveConsentLauncher.launch(
                        androidx.activity.result.IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                    )
                }
            }
        }
        setContent {
            val theme by viewModel.theme.collectAsState()
            val appLaunchState by viewModel.appLaunchState.collectAsState()
            val activeModal by viewModel.activeModal.collectAsState()
            val linkPrompt by viewModel.linkPrompt.collectAsState()
            val proteinTask by viewModel.proteinConfirmationTask.collectAsState()
            val unlockedBadge by viewModel.unlockedBadge.collectAsState()

            MyApplicationTheme(darkTheme = true) {
                Box(modifier = Modifier.fillMaxSize()) {
                    when (appLaunchState) {
                        is AppLaunchState.Loading -> {
                            // Full-screen splash — fills entire viewport immediately
                            SyncWellSplashScreen()
                        }
                        is AppLaunchState.OnboardingRequired -> {
                            OnboardingScreen(viewModel = viewModel)
                        }
                        is AppLaunchState.Ready -> {
                            // The container's own Scaffold applies the system-bar insets once; a second Scaffold here doubled them
                            MainNavigationContainer(viewModel = viewModel, modifier = Modifier.fillMaxSize(), deepLink = deepLink.value, onDeepLinkHandled = { deepLink.value = null })
                        }
                    }

            // Protein Intake Confirmation Dialog
            proteinTask?.let { item ->
                AlertDialog(
                    onDismissRequest = { viewModel.cancelProteinConfirmation() },
                    title = {
                        Text(
                            text = "Protein Intake Verification",
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                            fontSize = 18.sp
                        )
                    },
                    text = {
                        Text(
                            text = "Did you ensure adequate high-quality protein was consumed for this block (${item.title})?",
                            fontSize = 14.sp
                        )
                    },
                    confirmButton = {
                        androidx.compose.material3.Button(
                            onClick = { viewModel.confirmProteinIntake(item, true) },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Emerald600)
                        ) {
                            Text("Yes, protein taken", color = OnProgress)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { viewModel.confirmProteinIntake(item, false) }
                        ) {
                            Text("No / Skip Protein", color = Slate400)
                        }
                    },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface
                )
            }

                    unlockedBadge?.let { b ->
                        val context = androidx.compose.ui.platform.LocalContext.current
                        key(b.id) {
                            BadgeStampOverlay(b, onShare = { ShareCard.shareBadge(context, b) }, onKeepGoing = { viewModel.dismissUnlock() })
                        }
                    }

                    linkPrompt?.let { p ->
                        AlertDialog(
                            onDismissRequest = { viewModel.answerLinkPrompt(false) },
                            title = {
                                Text(
                                    when (p.event) {
                                        LinkEvent.WORKOUT -> "Workout finished"
                                        LinkEvent.FOCUS -> "Focus block saved"
                                        LinkEvent.WATER -> "Water goal reached"
                                        LinkEvent.SUPPLEMENT -> "Supplement logged"
                                    },
                                    fontWeight = FontWeight.Black
                                )
                            },
                            text = { Text("Tick \"${p.item.title}\" in today's routine?") },
                            confirmButton = {
                                TextButton(onClick = { viewModel.answerLinkPrompt(true) }) {
                                    Text("Yes, tick it", color = Emerald600, fontWeight = FontWeight.Black)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { viewModel.answerLinkPrompt(false) }) { Text("Not now", color = Slate400) }
                            },
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface
                        )
                    }

                    if (activeModal == "DRIVE_CONSENT_INFO") {
                        AlertDialog(
                            onDismissRequest = { viewModel.cancelDriveConsent() },
                            title = { Text("Back up to Google Drive", fontWeight = FontWeight.Black) },
                            text = { Text("Your backup is stored privately in your own Google Drive. SyncWell can't see your other files.") },
                            confirmButton = {
                                TextButton(onClick = { viewModel.continueDriveConsent() }) {
                                    Text("Continue", color = Emerald600, fontWeight = FontWeight.Black)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { viewModel.cancelDriveConsent() }) {
                                    Text("Cancel", color = Slate400)
                                }
                            },
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface
                        )
                    }

                    // Global Restore Modal
                    if (activeModal == "RESTORE_CONFIRMATION") {
                        AlertDialog(
                            onDismissRequest = { viewModel.setActiveModal(null) },
                            title = { Text("Cloud Backup Found", fontWeight = androidx.compose.ui.text.font.FontWeight.Black) },
                            text = { Text("We found an existing backup linked to your account. Would you like to restore your data (Todos and Weight Logs) now?") },
                            confirmButton = {
                                TextButton(onClick = { viewModel.restoreDataFromCloud() }) {
                                    Text("Restore now", color = com.example.ui.theme.Emerald600, fontWeight = androidx.compose.ui.text.font.FontWeight.Black)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { viewModel.setActiveModal(null) }) {
                                    Text("Cancel", color = com.example.ui.theme.Slate400)
                                }
                            },
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MainNavigationContainer(
    viewModel: SyncWellViewModel,
    modifier: Modifier = Modifier,
    deepLink: Destination? = null,
    onDeepLinkHandled: () -> Unit = {}
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val isTrackerFocused by viewModel.isTrackerScreenFocused.collectAsState()

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                // If the app is backgrounded and tracker was NOT focused, trigger check
                // If it WAS focused, do nothing (Scenario A)
                if (!isTrackerFocused) {
                    viewModel.scheduleAttentionCheck()
                }
            } else if (event == Lifecycle.Event.ON_RESUME) {
                // Return to app -> cancel any pending checks
                viewModel.cancelAttentionCheck()
                // The process can outlive midnight, so re-read today's date on every resume
                com.example.service.DayBoundaryManager.refreshTargetDate()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Onboarding's "Import my own routine" asks for the Routine tab, where the import opens by itself
    val importRequested by viewModel.routineImportRequested.collectAsState()
    AppNavHost(
        modifier = modifier,
        deepLink = deepLink ?: if (importRequested) Destination(Tab.ROUTINE) else null,
        onDeepLinkHandled = onDeepLinkHandled,
        home = { onNavigate -> HomeScreen(viewModel = viewModel, onNavigate = onNavigate) },
        routine = { RoutinesScreen(viewModel = viewModel) },
        workout = { WorkoutScreen(viewModel = viewModel) },
        focus = { TrackerScreen(viewModel = viewModel) },
        overlayContent = { overlay, onBack -> OverlayScreen(overlay, viewModel, onBack) }
    )
}
