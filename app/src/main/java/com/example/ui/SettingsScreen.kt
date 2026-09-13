package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.SyncWellViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SyncWellViewModel) {
    val theme by viewModel.theme.collectAsState()
    val backupEnabled by viewModel.backupEnabled.collectAsState()
    val isVacationActive by viewModel.isVacationActive.collectAsState()
    val isCloudBackupSyncInProgress by viewModel.isCloudBackupSyncInProgress.collectAsState()
    val lastSyncTime by viewModel.lastSuccessfulTransmissionTimestamp.collectAsState()
    val backupTestResult by viewModel.backupTestResult.collectAsState()
    val user by viewModel.user.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Header
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "SYSTEM PREFERENCES & ARCHIVES",
                color = Slate400,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = "Settings Tree",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black
            )
        }

        // Card 1: DISPLAY/THEME PANEL
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceTier1),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, OledBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "DISPLAY SETTINGS",
                    color = Slate400,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Theme Icon",
                            tint = EmeraldGlow
                        )
                        Column {
                            Text(
                                text = "Pure OLED Dark Mode",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Locked to Dark Mode for high-contrast data density",
                                color = Slate400,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Surface(
                        color = EmeraldDim,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, EmeraldGlow.copy(alpha = 0.5f)),
                        modifier = Modifier.testTag("theme_mode_switch")
                    ) {
                        Text(
                            text = "ACTIVE",
                            color = EmeraldGlow,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Card 2: VACATION MODE PANELS
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceTier1),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, OledBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "VACATION MODE CONTROLLER",
                    color = Slate400,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Text(
                    text = "Freeze daily checklist tracking segments cleanly without penalizing continuous active lifestyle streak counters:",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Warning Ticket",
                            tint = if (isVacationActive) EmeraldGlow else TextMuted
                        )
                        Text(
                            text = "Vacation Status",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp
                        )
                    }

                    Switch(
                        checked = isVacationActive,
                        onCheckedChange = { viewModel.toggleVacationMode() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = EmeraldGlow,
                            checkedTrackColor = EmeraldDim
                        ),
                        modifier = Modifier.testTag("vacation_mode_switch")
                    )
                }
            }
        }

        // Card 3: BACKUP / ARCHIVES CONTROLLERS
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceTier1),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, OledBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "CLOUDS BACKUP SNAPSHOTS",
                    color = Slate400,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Text(
                    text = "Sync consolidated End-of-Day JSON compilation payloads safely under Firestore append-only directories.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                // On EOD Sync active switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sync Icon",
                            tint = EmeraldGlow
                        )
                        Text(
                            text = "EOD Automated Sync",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp
                        )
                    }

                    Switch(
                        checked = backupEnabled,
                        onCheckedChange = { viewModel.setBackupEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = EmeraldGlow,
                            checkedTrackColor = EmeraldDim
                        ),
                        modifier = Modifier.testTag("backup_automated_switch")
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = OledBorder)

                // FEATURE 14: Test Backup Now button with result feedback
                Button(
                    onClick = { viewModel.triggerTestBackup() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when (backupTestResult) {
                            "SUCCESS" -> EmeraldGlow
                            else -> if (backupTestResult?.startsWith("FAILED") == true) RoseCrimson else EmeraldGlow
                        }
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("force_backup_btn")
                ) {
                    if (isCloudBackupSyncInProgress) {
                        CircularProgressIndicator(color = OledBlack, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Testing Backup...", color = OledBlack, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    } else when {
                        backupTestResult == "SUCCESS" -> {
                            Icon(Icons.Default.Check, contentDescription = null, tint = OledBlack)
                            Spacer(Modifier.width(8.dp))
                            Text("Backup Successful!", color = OledBlack, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        backupTestResult?.startsWith("FAILED") == true -> {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("Backup Failed", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        else -> {
                            Icon(Icons.Default.Share, contentDescription = null, tint = OledBlack)
                            Spacer(Modifier.width(8.dp))
                            Text("Test Backup Now", color = OledBlack, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }

                // FEATURE 14: Status row showing last backup time
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = if (lastSyncTime != null) EmeraldGlow else TextMuted, modifier = Modifier.size(14.dp))
                    Text(
                        text = if (lastSyncTime != null) "Last backup: ${lastSyncTime!!.take(16).replace("T", " ")}" else "No backup recorded yet",
                        color = if (lastSyncTime != null) EmeraldGlow else TextMuted,
                        fontSize = 12.sp, fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Card 4: USER / FIREBASE SIGN-IN PLATFORM AUTHORIZATION
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceTier1),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, OledBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "FIREBASE AUTH CONTROL",
                    color = Slate400,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                if (user != null && user != "Offline_User") {
                    Text(
                        text = "Currently logged in securely as: $user",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Button(
                        onClick = { viewModel.clearAuthentication() },
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceTier2, contentColor = TextPrimary),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, OledBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("clear_auth_btn")
                    ) {
                        Text("Sign Out Auth", fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                } else {
                    Text(
                        text = "Authorize Google Sign-In secure authentication inside Firebase backends to enable continuous backup pathways.",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    Button(
                        onClick = { viewModel.initiateGoogleSignIn(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceTier2, contentColor = MaterialTheme.colorScheme.onSurface),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("authorize_google_signin"),
                        border = BorderStroke(1.dp, OledBorder)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "Google",
                                tint = EmeraldGlow,
                                modifier = Modifier.size(20.dp)
                            )
                            Text("Sign in with Google", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Card 5: MAINTENANCE & TROUBLESHOOTING
        var showResetDialog by remember { mutableStateOf(false) }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceTier1),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, OledBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "MAINTENANCE & RECOVERY",
                    color = Slate400,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Text(
                    text = "If today's routine tasks seem incorrect or incomplete, use the reset trigger to re-populate from master templates.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                Button(
                    onClick = { showResetDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseCrimson),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("reset_routine_btn")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset", tint = Color.White)
                        Text("Reset Today's Routine", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                title = { Text("Reset Confirmation", fontWeight = FontWeight.Black) },
                text = { Text("This will permanently delete all progress for today's routine and reload the 22+ tasks from templates. Other data like logs and focus sessions will remain safe.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.resetTodayRoutine()
                            showResetDialog = false
                        }
                    ) {
                        Text("RESET NOW", color = RoseCrimson, fontWeight = FontWeight.Black)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetDialog = false }) {
                        Text("CANCEL", color = TextMuted)
                    }
                },
                shape = RoundedCornerShape(20.dp),
                containerColor = SurfaceTier1
            )
        }
    }
}
