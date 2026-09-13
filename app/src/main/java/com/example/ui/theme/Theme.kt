package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary            = EmeraldGlow,
    onPrimary          = OledBlack,
    primaryContainer   = EmeraldDim,
    onPrimaryContainer = EmeraldGlow,

    secondary            = VioletPulse,
    onSecondary          = OledBlack,
    secondaryContainer   = VioletDim,
    onSecondaryContainer = VioletPulse,

    tertiary            = AmberFlame,
    onTertiary          = OledBlack,
    tertiaryContainer   = AmberDim,
    onTertiaryContainer = AmberFlame,

    error            = RoseCrimson,
    onError          = OledBlack,
    errorContainer   = RoseDim,
    onErrorContainer = RoseCrimson,

    background        = OledBlack,       // #000000
    onBackground      = TextPrimary,     // #F8FAFC

    surface           = SurfaceTier1,   // #0A0C10
    onSurface         = TextPrimary,
    surfaceVariant    = SurfaceTier2,   // #12151F
    onSurfaceVariant  = TextSecondary,

    outline           = OledBorder,     // #242B3F
    outlineVariant    = OledBorderActive,

    scrim             = Color(0xCC000000)
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
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography  = Typography,
        content     = content
    )
}
