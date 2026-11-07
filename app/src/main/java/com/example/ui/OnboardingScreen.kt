package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.SyncWellViewModel
import kotlinx.coroutines.launch

data class OnboardFeature(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val description: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(viewModel: SyncWellViewModel) {
    val scope = rememberCoroutineScope()
    val features = listOf(
        OnboardFeature(
            Icons.Default.CheckCircle,
            "Routines & Habits",
            "Build your own daily or weekly checklists. Pause them safely with vacation mode."
        ),
        OnboardFeature(
            Icons.Default.PlayArrow,
            "Multi-Category Focus Clock",
            "Track Study, Work, Sleep, and Sport sessions with live background timers."
        ),
        OnboardFeature(
            Icons.Default.Star,
            "Workout Logging",
            "Log sets, reps, and weights. Start at 0 kg and build up at your own pace."
        ),
        OnboardFeature(
            Icons.Default.Info,
            "Private Google Drive Backup",
            "Optional backup stored in your own Google Drive that only SyncWell can see."
        )
    )

    var step by remember { mutableIntStateOf(0) }
    if (step > 0) {
        OnboardingSetup(viewModel, step, onStep = { step = it })
        return
    }

    Scaffold(
        containerColor = OledBlack
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.Start
        ) {
            // LOGO MARK — OLED neon emblem
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(EmeraldDim, shape = RoundedCornerShape(14.dp))
                    .padding(1.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "SyncWell Logo",
                    tint = EmeraldGlow,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // HEADLINE — crisp OLED high contrast
            Text(
                text = "Welcome to\nSyncWell",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimary,
                lineHeight = 38.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Your personal elite lifestyle dashboard. Track, align, and execute every day with precision.",
                fontSize = 15.sp,
                color = TextSecondary,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            // FEATURE CARDS — OLED SurfaceTier1 with 1px border
            features.forEach { feature ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceTier1),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, OledBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(EmeraldDim, shape = RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = feature.icon,
                                contentDescription = null,
                                tint = EmeraldGlow,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = feature.title,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = feature.description,
                                fontSize = 13.sp,
                                color = TextSecondary,
                                lineHeight = 19.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // CTA BUTTON — Punchy Emerald glow on black
            Button(
                onClick = { step = 1 },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("get_started_onboarding_btn"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldGlow,
                    contentColor = OledBlack
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Get Started",
                        color = OledBlack,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = OledBlack,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Works offline. Signing in is optional and only used for backup.",
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = TextMuted,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

/** Step 1: optional sign-in / restore. Step 2: personalization questions. */
@Composable
private fun OnboardingSetup(viewModel: SyncWellViewModel, step: Int, onStep: (Int) -> Unit) {
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val user by viewModel.user.collectAsState()
    val signedIn = user != null && user != "Offline_User"

    Scaffold(containerColor = OledBlack) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp, vertical = 40.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (step == 1) {
                Text("Restore your data?", fontSize = 28.sp, fontWeight = FontWeight.Black, color = TextPrimary)
                Text(
                    "Sign in with Google to restore a backup from your own Google Drive. SyncWell can't see your other files. You can skip this and sign in later in Settings.",
                    color = TextSecondary, fontSize = 14.sp, lineHeight = 21.sp
                )
                if (signedIn) Text("Signed in as $user", color = EmeraldGlow, fontWeight = FontWeight.Bold)
                Button(
                    onClick = { viewModel.initiateGoogleSignIn(context) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceTier2, contentColor = TextPrimary),
                    shape = RoundedCornerShape(14.dp)
                ) { Text(if (signedIn) "Check for a backup again" else "Sign in with Google") }
                Button(
                    onClick = { onStep(2) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGlow, contentColor = OledBlack),
                    shape = RoundedCornerShape(14.dp)
                ) { Text(if (signedIn) "Continue (no backup)" else "Continue without signing in", fontWeight = FontWeight.Black) }
            } else {
                Text("Set up your routine", fontSize = 28.sp, fontWeight = FontWeight.Black, color = TextPrimary)
                Text("A few quick questions build your starter routine. You can edit everything later.", color = TextSecondary, fontSize = 14.sp)
                SetupQuestionsForm(
                    initial = null, askCurrentWeight = true, submitLabel = "Build my routine",
                    onSubmit = { r ->
                        scope.launch { viewModel.completeOnboarding(r.answers, r.waterMl, r.location, r.currentWeightKg, r.goalWeightKg) }
                    },
                    onSkip = { scope.launch { viewModel.completeOnboarding() } }
                )
            }
        }
    }
}
