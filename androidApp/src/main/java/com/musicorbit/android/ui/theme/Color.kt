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

// ── Light ("day orbit") ─────────────────────────────────────────────────────
val DayBackground       = Color(0xFFF3F5FF)
val DaySurface          = Color(0xFFFFFFFF)
val DaySurfaceVariant   = Color(0xFFE7EAF8)
val DayOnSurface        = Color(0xFF0B1020)
val DayOnSurfaceMuted   = Color(0xFF4C5876)
val DayOutline          = Color(0xFFC7CEE6)

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
