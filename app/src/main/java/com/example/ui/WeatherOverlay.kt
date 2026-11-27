package com.example.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.example.domain.Weather
import com.example.domain.WeatherParticles
import com.example.ui.theme.LocalReducedMotion

/** Light rain or snow over the map. Nothing is drawn for clear weather, and the reduce-motion setting switches the weather off with the other animations. */
@Composable
fun WeatherOverlay(weather: Weather, modifier: Modifier = Modifier.fillMaxSize()) {
    if (WeatherParticles.count(weather) == 0 || LocalReducedMotion.current) return // returns before any animation exists
    val phase by rememberInfiniteTransition(label = "weather").animateFloat(0f, 1f, infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Restart), label = "weather_phase")
    val ink = if (weather == Weather.RAIN) Color(0xFF9CC9FF).copy(alpha = 0.55f) else Color.White.copy(alpha = 0.7f)
    Canvas(modifier) {
        WeatherParticles.at(weather, phase).forEach { p ->
            val from = Offset(p.x * size.width, p.y * size.height)
            if (weather == Weather.RAIN) drawLine(ink, from, Offset(from.x - 4f, from.y + p.length * size.height), strokeWidth = 2f)
            else drawCircle(ink, 3f, from)
        }
    }
}
