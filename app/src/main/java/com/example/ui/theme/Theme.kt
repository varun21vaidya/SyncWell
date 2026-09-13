package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Emerald500,
    secondary = Slate400,
    tertiary = IndigoAccent,
    background = RichBlack,
    surface = SurfaceDeep,
    surfaceVariant = SurfaceVariantDeep,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = Slate50,
    onSurface = Slate100,
    onSurfaceVariant = Slate200
)

private val LightColorScheme = lightColorScheme(
    primary = Emerald600,
    secondary = Slate700,
    tertiary = GoldAccent,
    background = Slate50,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Slate900,
    onSurface = Slate800,
    onSurfaceVariant = Slate400
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
