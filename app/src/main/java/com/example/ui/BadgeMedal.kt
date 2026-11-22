package com.example.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.domain.Badge
import com.example.domain.BadgeIcon
import com.example.domain.BadgeTier
import com.example.ui.theme.LocalReducedMotion
import com.example.ui.theme.Surface3
import com.example.ui.theme.TextSecondary
import kotlin.math.cos
import kotlin.math.sin

/** Tones of one tier: base / light / dark / text / deepest (docs/DESIGN_REFRESH_PLAN.md section 4). */
class TierColors(val base: Color, val light: Color, val dark: Color, val text: Color, val deepest: Color)

private fun hex(rgb: Long) = Color(0xFF000000 or rgb)

fun tierColors(t: BadgeTier): TierColors = when (t) {
    BadgeTier.BRONZE -> TierColors(hex(0xC98552), hex(0xE8B084), hex(0x7A4522), hex(0xFCE3CC), hex(0x5A3018))
    BadgeTier.SILVER -> TierColors(hex(0xB8C4D3), hex(0xE6EDF5), hex(0x5E6B7E), hex(0xF4F7FB), hex(0x3F4A5A))
    BadgeTier.GOLD -> TierColors(hex(0xF2B635), hex(0xFFDC85), hex(0x8F620B), hex(0xFFF3CF), hex(0x6A4706))
    BadgeTier.TIDE -> TierColors(hex(0x4FA8F0), hex(0xA4D7FF), hex(0x1D4F86), hex(0xE6F4FF), hex(0x123766))
    BadgeTier.MINT -> TierColors(hex(0x34C98A), hex(0x8AEDC2), hex(0x146B49), hex(0xE2FFF2), hex(0x0C4B33))
    BadgeTier.ROSE -> TierColors(hex(0xF08A9B), hex(0xFFC6CF), hex(0x8A2E40), hex(0xFFE8EC), hex(0x661F2E))
    BadgeTier.AQUA -> TierColors(hex(0x3BB7E8), hex(0x99E2FB), hex(0x155E7E), hex(0xE4F8FF), hex(0x0C4560))
    BadgeTier.VIOLET -> TierColors(hex(0x9F86F0), hex(0xD0C4FF), hex(0x4A3592), hex(0xF0EBFF), hex(0x33236B))
}

/** Icon outlines on a 24 box: (SVG path, isStroke). Filled shapes are solid, strokes use round caps. */
private fun iconShapes(icon: BadgeIcon): List<Pair<String, Boolean>> = when (icon) {
    BadgeIcon.CALENDAR_CHECK -> listOf("M4,6 H20 V20 H4 Z" to true, "M4,10 H20" to true, "M8,3 V7" to true, "M16,3 V7" to true, "M9,15 L11.2,17.2 L15.5,12.8" to true)
    BadgeIcon.STAR -> listOf("M12,2.5 L14.8,9 L21.5,9.6 L16.4,14 L18,20.8 L12,17.2 L6,20.8 L7.6,14 L2.5,9.6 L9.2,9 Z" to false)
    BadgeIcon.LEAF -> listOf("M5,19 C5,10 10,5 20,4 C20,14 15,19 5,19 Z" to false)
    BadgeIcon.BOLT -> listOf("M13,2 L5,13.5 H11 L10,22 L19,10 H13 Z" to false)
    BadgeIcon.DROP -> listOf("M12,3 C12,3 6,10 6,14.5 C6,17.8 8.7,20.5 12,20.5 C15.3,20.5 18,17.8 18,14.5 C18,10 12,3 12,3 Z" to false)
    BadgeIcon.CHECK -> listOf("M5,12.5 L9.5,17 L19,7" to true)
    BadgeIcon.SUN -> listOf(
        "M8,12 A4,4 0 1,1 16,12 A4,4 0 1,1 8,12 Z" to false,
        "M12,2 V5" to true, "M12,19 V22" to true, "M2,12 H5" to true, "M19,12 H22" to true,
        "M5,5 L7,7" to true, "M17,17 L19,19" to true, "M5,19 L7,17" to true, "M17,7 L19,5" to true
    )
    BadgeIcon.STONES -> listOf(
        "M4,18 A8,3 0 1,1 20,18 A8,3 0 1,1 4,18 Z" to false,
        "M6,12.5 A6,2.3 0 1,1 18,12.5 A6,2.3 0 1,1 6,12.5 Z" to false,
        "M8.2,7 A3.8,2 0 1,1 15.8,7 A3.8,2 0 1,1 8.2,7 Z" to false
    )
    BadgeIcon.DUMBBELL -> listOf(
        "M2.5,9 H5 V15 H2.5 Z" to false, "M5.5,6.5 H8.5 V17.5 H5.5 Z" to false, "M8.5,11 H15.5 V13 H8.5 Z" to false,
        "M15.5,6.5 H18.5 V17.5 H15.5 Z" to false, "M19,9 H21.5 V15 H19 Z" to false
    )
    BadgeIcon.HOURGLASS -> listOf("M6,3 H18 V7 L13,12 L18,17 V21 H6 V17 L11,12 L6,7 Z" to false)
    BadgeIcon.SPROUT -> listOf(
        "M12,21 V12" to true,
        "M12,13 C12,8 8,5.5 4,6 C4,10 7.5,13 12,13 Z" to false,
        "M12,11 C12,7 15,4.5 20,4.5 C20,9 16.5,11.5 12,11 Z" to false
    )
}

/** A 16-point rosette (outer radius 46, inner 41) on a 100 box, [rotationDeg] turns it for the second layer of the 100-day medals. */
private fun rosettePath(rotationDeg: Double = 0.0): Path = Path().apply {
    for (k in 0 until 32) {
        val r = if (k % 2 == 0) 46.0 else 41.0
        val a = Math.toRadians(k * 11.25 + rotationDeg)
        val x = (50 + r * cos(a)).toFloat()
        val y = (50 + r * sin(a)).toFloat()
        if (k == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

/** The shapes every medal shares, built once (paths do not depend on the badge). */
internal class MedalPaths {
    val rosette = rosettePath()
    val rosetteTurned = rosettePath(11.25)
    val tailLeft = Path().apply { moveTo(30f, 58f); lineTo(48f, 62f); lineTo(46f, 99f); lineTo(39f, 91f); lineTo(31f, 99f); close() }
    val tailRight = Path().apply { moveTo(70f, 58f); lineTo(52f, 62f); lineTo(54f, 99f); lineTo(61f, 91f); lineTo(69f, 99f); close() }
    val banner = Path().apply { moveTo(17f, 59f); lineTo(83f, 59f); lineTo(78f, 65f); lineTo(83f, 71f); lineTo(17f, 71f); lineTo(22f, 65f); close() }
    val depth = Path().apply { moveTo(0f, 0f); lineTo(100f, 0f); lineTo(0f, 100f); close() } // the top-left half, lit
}

private fun DrawScope.drawIcon(icon: BadgeIcon, color: Color, box: Float, center: Offset) {
    val shapes = iconShapes(icon).map { (d, stroke) -> PathParser().parsePathString(d).toPath() to stroke }
    withTransform({
        translate(center.x - box / 2f, center.y - box / 2f)
        scale(box / 24f, box / 24f, Offset.Zero)
    }) {
        shapes.forEach { (path, stroke) ->
            if (stroke) drawPath(path, color, style = Stroke(width = 2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            else drawPath(path, color)
        }
    }
}

/**
 * Draws one medal fitted to the current draw size. [shineP] null draws no shine; otherwise it places the white band:
 * 0 is left of the medal, 1 is right of it, above 1 is off the medal. Used by [BadgeMedal] and by the share image.
 */
internal fun DrawScope.drawMedal(badge: Badge, paths: MedalPaths, shineP: Float?) {
    val colors = tierColors(badge.tier)
    val u = size.minDimension / 100f
    scale(u, u, pivot = Offset.Zero) {
        drawPath(paths.tailLeft, colors.dark); drawPath(paths.tailRight, colors.dark)
        if (badge.doubleRosette) drawPath(paths.rosetteTurned, colors.dark)
        drawPath(paths.rosette, colors.base)
        clipPath(paths.rosette) { drawPath(paths.depth, colors.light.copy(alpha = 0.5f)) }
        val c = Offset(50f, 50f)
        drawCircle(colors.dark, 34f, c); drawCircle(colors.light, 31f, c); drawCircle(colors.base, 28f, c)
        drawCircle(colors.text, 25f, c, style = Stroke(width = 1.4f, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(0.1f, 3.5f), 0f)))
        val bannerText = badge.banner
        if (bannerText == null) drawIcon(badge.icon, colors.deepest, 30f, c)
        else {
            drawIcon(badge.icon, colors.deepest, 24f, Offset(50f, 43f))
            drawPath(paths.banner, colors.deepest)
            drawIntoCanvas { cv ->
                val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    color = colors.text.toArgb(); textSize = 7f; textAlign = android.graphics.Paint.Align.CENTER; isFakeBoldText = true
                }
                cv.nativeCanvas.drawText(bannerText, 50f, 67.4f, paint)
            }
        }
        if (shineP != null) {
            clipPath(paths.rosette) {
                rotate(22f, pivot = Offset(50f, 50f)) {
                    drawRect(Color.White.copy(alpha = 0.4f), topLeft = Offset(-30f + shineP * 160f, -20f), size = Size(14f, 140f))
                }
            }
        }
    }
}

/**
 * One medal, drawn as a vector (no image files). Earned medals shine; locked ones are the same medal at 22% opacity
 * with a small lock chip. [shineOverride] lets a caller drive the band itself (the stamp popup sweeps it once);
 * under reduced motion the band is drawn still.
 */
@Composable
fun BadgeMedal(
    badge: Badge, earned: Boolean, size: Dp, modifier: Modifier = Modifier, staggerMs: Int = 0,
    shineOverride: (() -> Float)? = null
) {
    val reduced = LocalReducedMotion.current
    val paths = remember { MedalPaths() }
    val sweep = if (earned && !reduced && shineOverride == null) {
        rememberInfiniteTransition(label = "shine").animateFloat(
            initialValue = 0f, targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = keyframes { durationMillis = 3200; 0f at 0 using LinearEasing; 1f at 1600 using LinearEasing; 1f at 3200 },
                initialStartOffset = StartOffset(staggerMs)
            ), label = "shineProgress"
        )
    } else null

    Box(
        modifier.size(size).semantics { contentDescription = "${badge.title}, ${if (earned) "earned" else "not yet earned"}" },
        contentAlignment = Alignment.BottomEnd
    ) {
        Canvas(Modifier.size(size).alpha(if (earned) 1f else 0.22f)) {
            val p = if (!earned) null else if (reduced) 0.3f else shineOverride?.invoke() ?: sweep?.value ?: 0.3f
            drawMedal(badge, paths, p)
        }
        if (!earned) LockChip(size * 0.3f)
    }
}

@Composable
private fun LockChip(size: Dp) {
    Box(Modifier.size(size).clip(CircleShape).background(Surface3), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size * 0.6f)) {
            val u = this.size.minDimension / 24f
            scale(u, u, pivot = Offset.Zero) {
                drawPath(PathParser().parsePathString("M6,11 H18 V21 H6 Z").toPath(), TextSecondary)
                drawPath(PathParser().parsePathString("M8.5,11 V8 A3.5,3.5 0 0 1 15.5,8 V11").toPath(), TextSecondary, style = Stroke(width = 2f, cap = StrokeCap.Round))
            }
        }
    }
}
