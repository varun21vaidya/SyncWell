package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.Badge
import com.example.domain.BadgeRules
import com.example.ui.theme.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private const val CONFETTI_PARTICLES = 24

private class Particle(val angle: Float, val speed: Float, val size: Float, val color: Int, val spin: Float)

/**
 * The "stamp moment": a dimmed full-screen overlay with the medal dropping in on a spring, a light-hit haptic and a short
 * confetti burst at contact, then one shine sweep. Under reduced motion everything is drawn in its final, still state.
 * Total motion stays under 1.2 s.
 */
@Composable
fun BadgeStampOverlay(badge: Badge, onShare: () -> Unit, onKeepGoing: () -> Unit) {
    val reduced = LocalReducedMotion.current
    val haptic = LocalHapticFeedback.current
    val colors = remember(badge.tier) { tierColors(badge.tier) }
    val scale = remember { Animatable(if (reduced) 1f else 1.4f) }
    val shine = remember { Animatable(if (reduced) 0.3f else 2f) } // 2 = off the medal until the stamp lands
    val confetti = remember { Animatable(if (reduced) 0.55f else 0f) } // a still frame under reduced motion
    val particles = remember(badge.id) {
        val r = Random(badge.id.hashCode())
        val palette = listOf(colors.base, colors.light, colors.text, Streak).map { it.toArgb() }
        List(CONFETTI_PARTICLES) { Particle(r.nextFloat() * 6.2832f, 120f + r.nextFloat() * 260f, 6f + r.nextFloat() * 8f, palette[r.nextInt(palette.size)], r.nextFloat() * 360f) }
    }

    LaunchedEffect(badge.id) {
        if (reduced) return@LaunchedEffect
        launch { scale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium)) }
        snapshotFlow { scale.value }.first { it <= 1.001f } // contact
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        launch { confetti.animateTo(1f, tween(900, easing = LinearEasing)) }
        shine.snapTo(0f)
        shine.animateTo(1f, tween(900, easing = LinearEasing))
    }

    BackHandler(onBack = onKeepGoing)
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {} // swallow taps behind the card
            .testTag("stamp_overlay"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val t = confetti.value
            if (t > 0f && t < 1f || reduced) {
                val origin = Offset(size.width / 2f, size.height / 2f - 80.dp.toPx())
                particles.forEach { p ->
                    val dist = p.speed * t
                    val x = origin.x + cos(p.angle) * dist
                    val y = origin.y + sin(p.angle) * dist + 380f * t * t // gravity
                    val c = Color(p.color)
                    rotate(p.spin + 300f * t, Offset(x, y)) {
                        drawRect(c.copy(alpha = if (reduced) 0.9f else (1f - t * t).coerceIn(0f, 1f)), Offset(x - p.size / 2, y - p.size / 2), Size(p.size, p.size * 0.6f))
                    }
                }
            }
        }
        Card(
            modifier = Modifier.padding(24.dp).widthIn(max = 360.dp),
            colors = CardDefaults.cardColors(containerColor = Surface2),
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, Border)
        ) {
            Column(
                Modifier.padding(24.dp).semantics { contentDescription = BadgeRules.stampTitle(badge) },
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BadgeMedal(badge, earned = true, size = 160.dp, modifier = Modifier.graphicsLayer { scaleX = scale.value; scaleY = scale.value }, shineOverride = { shine.value })
                Text(BadgeRules.stampTitle(badge), color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                Text(BadgeRules.stampBody(badge), color = TextSecondary, fontSize = 14.sp, textAlign = TextAlign.Center)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onShare, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Border),
                        modifier = Modifier.weight(1f).height(48.dp).testTag("stamp_share")
                    ) { Text("Share", color = TextPrimary, fontWeight = FontWeight.Bold) }
                    Button(
                        onClick = onKeepGoing, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Progress),
                        modifier = Modifier.weight(1.4f).height(48.dp).testTag("stamp_keep_going")
                    ) { Text("Keep going", color = OnProgress, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

/** Shows the first medal of the queue; the next one appears once it is dismissed, and each restarts the stamp animation. */
@Composable
fun StampQueueHost(queue: List<Badge>, onShare: (Badge) -> Unit, onKeepGoing: () -> Unit) {
    queue.firstOrNull()?.let { b -> key(b.id) { BadgeStampOverlay(b, onShare = { onShare(b) }, onKeepGoing = onKeepGoing) } }
}
