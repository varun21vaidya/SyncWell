package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*

@Composable
fun SyncWellSplashScreen() {
    // 1. Subtle breathing scale pulse for the logo container
    val infiniteTransition = rememberInfiniteTransition(label = "splash_infinite")
    
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // 2. Neon aura glow breathing alpha
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    // 3. Shimmer progress bar sweep
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_offset"
    )

    // True Full-screen OLED black viewport
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBlack),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
        ) {
            // Logo Container with multilayer neon aura glow
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(160.dp)
            ) {
                // Outer ambient radial blur glow
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .scale(pulseScale * 1.15f)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    EmeraldGlow.copy(alpha = glowAlpha * 0.45f),
                                    CyanLaser.copy(alpha = glowAlpha * 0.20f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )

                // High-contrast frosted glass container for the emblem
                Box(
                    modifier = Modifier
                        .size(108.dp)
                        .scale(pulseScale)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    SurfaceTier2,
                                    SurfaceTier1
                                )
                            ),
                            shape = RoundedCornerShape(28.dp)
                        )
                        .border(
                            width = 1.5.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    EmeraldGlow.copy(alpha = 0.8f),
                                    CyanLaser.copy(alpha = 0.4f),
                                    OledBorder
                                )
                            ),
                            shape = RoundedCornerShape(28.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Crisp official SW botanical wellness emblem
                    Image(
                        painter = painterResource(id = R.drawable.ic_syncwell_emblem),
                        contentDescription = "SyncWell Logo",
                        modifier = Modifier
                            .size(68.dp)
                            .padding(4.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Brand Typography with neon gradient title
            Text(
                text = "SyncWell",
                color = TextPrimary,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.5).sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Subtitle
            Text(
                text = "CIRCADIAN WELLNESS & RUNBOOK ENGINE",
                color = EmeraldGlow.copy(alpha = 0.9f),
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.5.sp
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Premium Linear Loading Track with animated gradient sweep
            Box(
                modifier = Modifier
                    .width(180.dp)
                    .height(3.dp)
                    .background(SurfaceTier2, shape = RoundedCornerShape(2.dp))
            ) {
                val startX = (shimmerOffset * 220f) - 60f
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(60.dp)
                        .offset(x = (shimmerOffset * 120).dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    EmeraldGlow,
                                    CyanLaser,
                                    Color.Transparent
                                )
                            ),
                            shape = RoundedCornerShape(2.dp)
                        )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Status message
            Text(
                text = "Synchronizing routines & health biometrics...",
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(glowAlpha + 0.3f)
            )
        }
    }
}
