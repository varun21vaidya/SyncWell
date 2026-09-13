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
            "Circadian Runbooks & Habits",
            "Track daily checklists frozen safely in vacation blocks where needed."
        ),
        OnboardFeature(
            Icons.Default.PlayArrow,
            "Multi-Category Focus Clock",
            "Track Study, Work, Sleep, and Sport sessions with live background timers."
        ),
        OnboardFeature(
            Icons.Default.Star,
            "PPL Workout Progression",
            "Log sets, reps, and weights with automatic progressive overload suggestions."
        ),
        OnboardFeature(
            Icons.Default.Info,
            "Secure Google Cloud Backup",
            "End-of-day snapshots synced to Firestore with manual trigger control."
        )
    )

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
                onClick = {
                    scope.launch {
                        viewModel.completeOnboarding()
                    }
                },
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
                text = "100% offline. No account required to start.\nCloud backup available in Settings.",
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
