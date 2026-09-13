package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ── OLED Surfaces ──────────────────────────────────────────────────────────
val OledBlack         = Color(0xFF000000)  // Screen background (saves OLED power)
val SurfaceTier1      = Color(0xFF0A0C10)  // Cards / list items
val SurfaceTier2      = Color(0xFF12151F)  // Elevated cards / modal sheets
val SurfaceTier3      = Color(0xFF1C2234)  // Selected / interactive states
val OledBorder        = Color(0xFF242B3F)  // Subtle 1 dp card borders
val OledBorderActive  = Color(0xFF4F5B78)  // Focused / active borders

// ── Semantic Accent Colors ──────────────────────────────────────────────────
val EmeraldGlow   = Color(0xFF10B981)  // Health, routine done, hydration CTA
val EmeraldDim    = Color(0xFF064E3B)  // Emerald container / badge background
val Emerald400    = Color(0xFF34D399)  // Lighter emerald for progress highlights
val Emerald500    = Color(0xFF10B981)  // Alias for existing usages
val Emerald600    = Color(0xFF059669)  // Alias for existing usages
val Emerald700    = Color(0xFF047857)  // Alias for existing usages
val Emerald100    = Color(0xFFD1FAE5)  // Light badge background (light mode only)

val CyanLaser     = Color(0xFF06B6D4)  // Hydration, water intake
val CyanDim       = Color(0xFF164E63)  // Water container background

val VioletPulse   = Color(0xFF8B5CF6)  // Focus sessions, study mode
val VioletDim     = Color(0xFF3B1D6E)  // Focus container background
val PurpleAccent  = Color(0xFF8B5CF6)  // Alias for existing usages

val AmberFlame    = Color(0xFFF59E0B)  // Streak, warnings, gold badges
val AmberDim      = Color(0xFF78350F)  // Warning container background
val GoldAccent    = Color(0xFFF59E0B)  // Alias for existing usages

val RoseCrimson   = Color(0xFFF43F5E)  // High priority, workout, delete
val RoseDim       = Color(0xFF881337)  // Danger container background
val RoseAccent    = Color(0xFFF43F5E)  // Alias for existing usages

val IndigoAccent  = Color(0xFF4F46E5)  // Secondary brand color

// ── Text Hierarchy ───────────────────────────────────────────────────────────
val TextPrimary   = Color(0xFFF8FAFC)  // Headings — 95% white
val TextSecondary = Color(0xFF94A3B8)  // Sub-labels, captions
val TextMuted     = Color(0xFF64748B)  // Timestamps, placeholders
val TextInverse   = Color(0xFF000000)  // On bright accent buttons

// ── Legacy Slate Aliases (keep for backward compat) ──────────────────────────
val Slate50   = Color(0xFFF8FAFC)
val Slate100  = Color(0xFFF1F5F9)
val Slate200  = Color(0xFFE2E8F0)
val Slate400  = Color(0xFF94A3B8)
val Slate700  = Color(0xFF334155)
val Slate800  = Color(0xFF1E293B)
val Slate900  = Color(0xFF0F172A)

// ── Legacy Surface Aliases ────────────────────────────────────────────────────
val RichBlack          = Color(0xFF000000)   // Now true OLED black
val SurfaceDeep        = Color(0xFF0A0C10)   // Was #1E1E1E — now tier 1
val SurfaceVariantDeep = Color(0xFF12151F)   // Was #2C2C2C — now tier 2

// ── Misc ──────────────────────────────────────────────────────────────────────
val OnboardBg = Color(0xFFF8FAF9)            // Light onboarding (unchanged)
