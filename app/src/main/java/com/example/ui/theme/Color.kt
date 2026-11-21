package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ── "Deep tide" tokens (docs/DESIGN_REFRESH_PLAN.md §3) ────────────────────────
// Colour means something: green = progress/done, gold = streak and rewards, blue = water,
// violet = focus, coral = gym. Coloured areas are a tint of the surface, never a solid block.
val Background    = Color(0xFF05070D)  // Screen: near-black, navy-tinted, still OLED-friendly
val Surface1      = Color(0xFF0D1322)  // Cards
val Surface2      = Color(0xFF141C30)  // Sheets, dialogs, selected rows
val Surface3      = Color(0xFF1A2236)  // Tracks, inactive rings, chips
val Border        = Color(0xFF22304D)  // 1 dp card border

val TextPrimary   = Color(0xFFE8EEF7)  // Titles, numbers (soft white, less OLED glare)
val TextSecondary = Color(0xFF9AA8BF)  // Labels, captions (about 7.6:1 on Surface1)
val TextMuted     = Color(0xFF64738C)  // Hints, day letters

val Progress      = Color(0xFF3DDC97)  // Done, rings, primary button
val OnProgress    = Color(0xFF05291A)  // Text and icons on Progress
val Streak        = Color(0xFFFFC857)  // Streak number, week bar, badge highlights
val Water         = Color(0xFF4FB8FF)  // Water ring and log
val Focus         = Color(0xFFA78BFA)  // Focus timer
val Gym           = Color(0xFFFF7A85)  // Workout
val Danger        = Color(0xFFFF5C6C)  // Delete only

// Tints: the accent at 12-14% over Surface1, used as container fills
val ProgressDim   = Color(0xFF142F32)
val StreakDim     = Color(0xFF2A2928)
val WaterDim      = Color(0xFF162A41)
val FocusDim      = Color(0xFF232440)
val GymDim        = Color(0xFF2F2130)
val DangerDim     = Color(0xFF2F1D2C)

// ── Aliases: the old names keep working, so screens move over one at a time ─────
val OledBlack         = Background
val SurfaceTier1      = Surface1
val SurfaceTier2      = Surface2
val SurfaceTier3      = Surface3
val OledBorder        = Border
val OledBorderActive  = Color(0xFF3A4B70)  // Focused / active borders

val EmeraldGlow   = Progress
val EmeraldDim    = ProgressDim
val Emerald400    = Progress
val Emerald500    = Progress
val Emerald600    = Progress
val Emerald700    = Color(0xFF2BB57A)      // Success state of a mint button
val Emerald100    = Color(0xFFD1FAE5)      // Light badge background (light scheme only)

val CyanLaser     = Water
val CyanDim       = WaterDim

val VioletPulse   = Focus
val VioletDim     = FocusDim
val PurpleAccent  = Focus

val AmberFlame    = Streak
val AmberDim      = Surface1               // No longer a card fill: use StreakDim for a tint
val GoldAccent    = Streak

val RoseCrimson   = Gym
val RoseDim       = GymDim
val RoseAccent    = Gym

val IndigoAccent  = Color(0xFF4F46E5)      // Secondary brand colour

val TextInverse   = OnProgress

// ── Legacy Slate aliases ────────────────────────────────────────────────────────
val Slate50   = Color(0xFFF8FAFC)
val Slate100  = Color(0xFFF1F5F9)
val Slate200  = Color(0xFFE2E8F0)
val Slate400  = TextSecondary
val Slate700  = Color(0xFF334155)
val Slate800  = Color(0xFF1E293B)
val Slate900  = Color(0xFF0F172A)

// ── Legacy surface aliases ───────────────────────────────────────────────────────
val RichBlack          = Background
val SurfaceDeep        = Surface1
val SurfaceVariantDeep = Surface2

// ── Misc ──────────────────────────────────────────────────────────────────────
val OnboardBg = Color(0xFFF8FAF9)          // Light onboarding (unchanged)
