package com.musicorbit.android.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush

/**
 * Space / Orbit palette.
 * A deep-space dark base with vibrant nebula accents (violet, cyan, magenta),
 * plus a soft "day orbit" light variant. These tokens back the whole redesign.
 */

// ── Accent hues (shared by both variants) ───────────────────────────────────
val OrbitViolet   = Color(0xFF7C5CFF)   // primary
val OrbitIndigo   = Color(0xFF6366F1)
val OrbitCyan     = Color(0xFF22D3EE)   // secondary
val OrbitMagenta  = Color(0xFFEC4899)   // tertiary
val OrbitBlue     = Color(0xFF3B82F6)

// ── Dark (default, "deep space") ────────────────────────────────────────────
val SpaceBackground      = Color(0xFF05070F)
val SpaceSurface         = Color(0xFF0B1020)
val SpaceSurfaceVariant  = Color(0xFF151D36)
val SpaceSurfaceHigh     = Color(0xFF1C2745)
val SpaceOnSurface       = Color(0xFFE9ECF8)
val SpaceOnSurfaceMuted  = Color(0xFF9AA6C6)
val SpaceOutline         = Color(0xFF2A3450)

// ── Light ("day orbit") — clean, gender-neutral slate palette ───────────────
val DayBackground       = Color(0xFFF1F5F9)   // slate-100 canvas
val DaySurface          = Color(0xFFFFFFFF)   // white surface
val DaySurfaceVariant   = Color(0xFFE2E8F0)   // slate-200
val DayOnSurface        = Color(0xFF0F172A)   // slate-900
val DayOnSurfaceMuted   = Color(0xFF475569)   // slate-600
val DayOutline          = Color(0xFFCBD5E1)   // slate-300

// ── Light-mode accents (indigo / blue / teal) ───────────────────────────────
val DayPrimary           = Color(0xFF4F46E5)  // indigo-600
val DayPrimaryContainer  = Color(0xFFE0E7FF)  // indigo-100
val DayOnPrimaryContainer = Color(0xFF312E81) // indigo-900
val DaySecondary         = Color(0xFF2563EB)  // blue-600
val DaySecondaryContainer = Color(0xFFDBEAFE) // blue-100
val DayTertiary          = Color(0xFF0D9488)  // teal-600

/** Aurora gradient used on prominent surfaces (player, hero, buttons). */
fun auroraGradient() = Brush.linearGradient(
    colors = listOf(OrbitViolet, OrbitBlue, OrbitCyan)
)

/** Signature violet → magenta accent sweep. */
fun neonGradient() = Brush.linearGradient(
    colors = listOf(OrbitViolet, OrbitMagenta)
)

/** Cyan → violet sweep for seek bars / progress. */
fun seekGradient() = Brush.horizontalGradient(
    colors = listOf(OrbitCyan, OrbitViolet, OrbitMagenta)
)
