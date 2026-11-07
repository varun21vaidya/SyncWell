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

    private val driveConsentLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        viewModel.onDriveConsentResult(result.resultCode == RESULT_OK)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
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
                            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                                MainNavigationContainer(
                                    viewModel = viewModel,
                                    modifier = Modifier.padding(innerPadding)
                                )
                            }
                        }
                    }

                    if (activeModal == "DRIVE_CONSENT_INFO") {
                        AlertDialog(
                            onDismissRequest = { viewModel.cancelDriveConsent() },
                            title = { Text("Back up to Google Drive", fontWeight = FontWeight.Black) },
                            text = { Text("Your backup is stored privately in your own Google Drive. SyncWell can't see your other files.") },
                            confirmButton = {
                                TextButton(onClick = { viewModel.continueDriveConsent() }) {
                                    Text("CONTINUE", color = Emerald600, fontWeight = FontWeight.Black)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { viewModel.cancelDriveConsent() }) {
                                    Text("CANCEL", color = Slate400)
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
                                    Text("RESTORE NOW", color = com.example.ui.theme.Emerald600, fontWeight = androidx.compose.ui.text.font.FontWeight.Black)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { viewModel.setActiveModal(null) }) {
                                    Text("CANCEL", color = com.example.ui.theme.Slate400)
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
fun MainNavigationContainer(viewModel: SyncWellViewModel, modifier: Modifier = Modifier) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
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
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = OledBlack,
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceTier1,
                tonalElevation = 0.dp,
                modifier = Modifier.testTag("app_navigation_bar")
            ) {
                val items = listOf(
                    Triple("Home", Icons.Default.Home, 0),
                    Triple("Routine", Icons.Default.List, 1),
                    Triple("Workout", Icons.Default.Star, 2),
                    Triple("Focus", Icons.Default.PlayArrow, 3),
                    Triple("Tasks", Icons.Default.Check, 4),
                    Triple("Stats", Icons.Default.Info, 5),
                    Triple("Config", Icons.Default.Settings, 6)
                )

                items.forEach { (title, icon, index) ->
                    val isSelected = selectedTabIndex == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTabIndex = index },
                        icon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = "$title Tab",
                                modifier = if (isSelected) Modifier.size(22.dp) else Modifier.size(20.dp)
                            )
                        },
                        label = { 
                            Text(
                                text = title, 
                                fontSize = 9.sp, 
                                maxLines = 1, 
                                softWrap = false,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            ) 
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = EmeraldGlow,
                            unselectedIconColor = TextMuted,
                            selectedTextColor = EmeraldGlow,
                            unselectedTextColor = TextMuted,
                            indicatorColor = EmeraldDim
                        ),
                        modifier = Modifier.testTag("nav_tab_$index")
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTabIndex) {
                0 -> HomeScreen(viewModel = viewModel, onNavigateToTab = { selectedTabIndex = it })
                1 -> RoutinesScreen(viewModel = viewModel)
                2 -> WorkoutScreen(viewModel = viewModel)
                3 -> TrackerScreen(viewModel = viewModel)
                4 -> TodosScreen(viewModel = viewModel)
                5 -> AnalyticsScreen(viewModel = viewModel)
                6 -> SettingsScreen(viewModel = viewModel)
            }
        }

        // Global back press to return to home if not on home
        BackHandler(enabled = selectedTabIndex != 0) {
            selectedTabIndex = 0
        }
    }
}
