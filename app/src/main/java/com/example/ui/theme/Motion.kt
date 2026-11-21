package com.example.ui.theme

import androidx.compose.runtime.compositionLocalOf

/**
 * True when the system animation scale is 0. Shine, confetti and the stamp drop are then drawn static.
 * Provided by [MyApplicationTheme], so every screen and every test sees the real setting.
 */
val LocalReducedMotion = compositionLocalOf { false }
