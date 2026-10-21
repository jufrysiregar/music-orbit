package com.musicorbit.android.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush

/**
 * Titanium / Obsidian palette.
 * Dark = "Obsidian Void": deep charcoal (#0D0F14) + dark metallic glass, glowing-cyan & silver accents.
 * Light = "Titanium Slate": platinum slate (#F1F5F9..#E2E8F0) + frosted white glass, deep-cobalt accent.
 * No pink / purple. These tokens back the whole redesign.
 */

// ── Accent hues (shared by both variants) ───────────────────────────────────
val OrbitViolet   = Color(0xFF2563EB)   // deep cobalt blue (primary brand / metallic)
val OrbitIndigo   = Color(0xFF1D4ED8)   // cobalt-700
val OrbitCyan     = Color(0xFF00F0FF)   // glowing cyan
val OrbitMagenta  = Color(0xFFC8CEDA)   // silver
val OrbitBlue     = Color(0xFF0284C7)   // sky cyan

// ── Dark ("Obsidian Void") ──────────────────────────────────────────────────
val SpaceBackground      = Color(0xFF0D0F14)   // deep space charcoal
val SpaceSurface         = Color(0xFF12151C)
val SpaceSurfaceVariant  = Color(0xFF1E222D)   // dark metallic glass cards
val SpaceSurfaceHigh     = Color(0xFF262B3A)
val SpaceOnSurface       = Color(0xFFE6EAF2)   // silver-white text
val SpaceOnSurfaceMuted  = Color(0xFF9AA6B8)   // muted silver
val SpaceOutline         = Color(0xFF2A3140)

// ── Light ("Titanium Slate") ────────────────────────────────────────────────
val DayBackground       = Color(0xFFF1F5F9)   // platinum slate canvas
val DaySurface          = Color(0xFFFFFFFF)   // frosted white glass
val DaySurfaceVariant   = Color(0xFFE2E8F0)   // slate-200
val DayOnSurface        = Color(0xFF0F172A)   // slate-900 text
val DayOnSurfaceMuted   = Color(0xFF475569)   // slate-600
val DayOutline          = Color(0xFFCBD5E1)   // slate-300 metallic border

// ── Light-mode accents (cobalt / sky / cyan) ────────────────────────────────
val DayPrimary           = Color(0xFF2563EB)  // deep cobalt blue
val DayPrimaryContainer  = Color(0xFFDBEAFE)  // blue-100
val DayOnPrimaryContainer = Color(0xFF1E3A8A) // blue-900
val DaySecondary         = Color(0xFF0284C7)  // sky-600
val DaySecondaryContainer = Color(0xFFE0F2FE) // sky-100
val DayTertiary          = Color(0xFF0891B2)  // cyan-700

/** Aurora gradient used on prominent surfaces (player, hero, buttons) — cobalt → sky → cyan. */
fun auroraGradient() = Brush.linearGradient(
    colors = listOf(OrbitViolet, OrbitBlue, OrbitCyan)
)

/** Cobalt → silver accent sweep. */
fun neonGradient() = Brush.linearGradient(
    colors = listOf(OrbitViolet, OrbitMagenta)
)

/** Cyan → cobalt → silver sweep for seek bars / progress. */
fun seekGradient() = Brush.horizontalGradient(
    colors = listOf(OrbitCyan, OrbitViolet, OrbitMagenta)
)
