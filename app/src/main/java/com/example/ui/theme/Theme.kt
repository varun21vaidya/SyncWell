package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

private val DarkColorScheme = darkColorScheme(
    primary            = Progress,
    onPrimary          = OnProgress,
    primaryContainer   = ProgressDim,
    onPrimaryContainer = Progress,

    secondary            = Focus,
    onSecondary          = Background,
    secondaryContainer   = FocusDim,
    onSecondaryContainer = Focus,

    tertiary            = Streak,
    onTertiary          = Background,
    tertiaryContainer   = StreakDim,
    onTertiaryContainer = Streak,

    error            = Danger,
    onError          = Background,
    errorContainer   = DangerDim,
    onErrorContainer = Danger,

    background        = Background,
    onBackground      = TextPrimary,

    surface           = Surface1,
    onSurface         = TextPrimary,
    surfaceVariant    = Surface2,
    onSurfaceVariant  = TextSecondary,
    surfaceContainerLowest  = Background,
    surfaceContainerLow     = Surface1,
    surfaceContainer        = Surface1,
    surfaceContainerHigh    = Surface2,
    surfaceContainerHighest = Surface3,

    outline           = Border,
    outlineVariant    = OledBorderActive,

    scrim             = Color(0x99000000)
)

private val LightColorScheme = lightColorScheme(
    primary            = Emerald600,
    onPrimary          = Color.White,
    primaryContainer   = Emerald100,
    onPrimaryContainer = Emerald700,

    secondary            = PurpleAccent,
    onSecondary          = Color.White,
    secondaryContainer   = Color(0xFFEDE9FE),
    onSecondaryContainer = Color(0xFF4C1D95),

    tertiary            = GoldAccent,
    onTertiary          = Color.White,
    tertiaryContainer   = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF78350F),

    error            = RoseCrimson,
    onError          = Color.White,
    errorContainer   = Color(0xFFFFE4E6),
    onErrorContainer = Color(0xFF881337),

    background       = Slate50,
    onBackground     = Slate900,

    surface          = Color.White,
    onSurface        = Slate800,
    surfaceVariant   = Slate100,
    onSurfaceVariant = Slate400,

    outline          = Slate200,
    outlineVariant   = Slate100,
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    // Dark Mode Only: The application is designed exclusively with the high-performance OLED Dark Theme
    val context = LocalContext.current
    val reducedMotion = remember { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
    CompositionLocalProvider(LocalReducedMotion provides reducedMotion) {
        MaterialTheme(
            colorScheme = DarkColorScheme,
            typography  = Typography
        ) {
            // Tabular numbers everywhere, so counters and timers do not jump as digits change
            ProvideTextStyle(TextStyle(fontFeatureSettings = "tnum"), content)
        }
    }
}
