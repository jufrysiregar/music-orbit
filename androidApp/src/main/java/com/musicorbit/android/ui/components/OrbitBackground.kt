package com.musicorbit.android.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.musicorbit.android.ui.theme.LocalOrbitDark
import com.musicorbit.android.ui.theme.OrbitBlue
import com.musicorbit.android.ui.theme.OrbitCyan
import com.musicorbit.android.ui.theme.OrbitIndigo
import com.musicorbit.android.ui.theme.OrbitMagenta
import com.musicorbit.android.ui.theme.OrbitViolet
import kotlin.random.Random

/**
 * Animated deep-space backdrop: a soft aurora glow behind a field of faint, slowly
 * twinkling stars. Place as the first child of a screen Box to sit behind all content.
 */
@Composable
fun OrbitBackground(
    modifier: Modifier = Modifier,
    showStars: Boolean = true
) {
    val isDark = LocalOrbitDark.current
    // Solid theme base so a transparent Scaffold never reveals the black window behind it
    // (this was the Light-Mode "black bleed"). Dark mode → deep space; Light mode → clean bright surface.
    val baseColor = MaterialTheme.colorScheme.background

    // Star positions are generated once from a fixed seed so the field is stable.
    val stars = rememberStableStars(count = if (isDark) 60 else 34)

    val transition = rememberInfiniteTransition(label = "orbit")
    val twinkle by transition.animateFloat(
        initialValue  = 0.55f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "twinkle"
    )

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // 1) Base vertical gradient: keeps a solid, legible surface top-to-bottom while adding
            //    subtle depth (no flat dead zone in the middle / bottom).
            val bottomTint = if (isDark) Color(0xFF0A0C14) else Color(0xFFE9F1FC)
            drawRect(brush = Brush.verticalGradient(colors = listOf(baseColor, bottomTint)))

            val w = size.width
            val h = size.height

            // Aurora glows spread across the WHOLE canvas (top → mid → bottom, both sides) so colour
            // is present everywhere, not just in a band under the header. Cool cobalt / cyan / sky /
            // indigo / silver tones keep the look premium and neutral (never pink / feminine).
            drawRadialGlow(Offset(w * 0.08f, h * 0.06f), w * 0.90f, OrbitViolet.copy(alpha  = if (isDark) 0.34f else 0.20f))
            drawRadialGlow(Offset(w * 0.98f, h * 0.02f), w * 0.70f, OrbitCyan.copy(alpha   = if (isDark) 0.26f else 0.16f))
            drawRadialGlow(Offset(w * 0.86f, h * 0.34f), w * 0.72f, OrbitBlue.copy(alpha   = if (isDark) 0.26f else 0.15f))
            drawRadialGlow(Offset(w * 0.10f, h * 0.50f), w * 0.82f, OrbitIndigo.copy(alpha = if (isDark) 0.24f else 0.14f))
            drawRadialGlow(Offset(w * 0.92f, h * 0.70f), w * 0.78f, OrbitMagenta.copy(alpha= if (isDark) 0.22f else 0.15f))
            drawRadialGlow(Offset(w * 0.06f, h * 0.98f), w * 0.88f, OrbitCyan.copy(alpha   = if (isDark) 0.24f else 0.15f))
            drawRadialGlow(Offset(w * 0.62f, h * 0.96f), w * 0.72f, OrbitViolet.copy(alpha = if (isDark) 0.22f else 0.13f))

            // Starfield
            if (showStars && isDark) {
                val starColor = Color.White
                stars.forEach { s ->
                    val alpha = s.baseAlpha * twinkle
                    drawCircle(
                        color  = starColor.copy(alpha = alpha),
                        radius = s.radius,
                        center = Offset(s.x * w, s.y * h)
                    )
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRadialGlow(
    center: Offset,
    radius: Float,
    color: Color
) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color, Color.Transparent),
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )
}

private data class Star(val x: Float, val y: Float, val radius: Float, val baseAlpha: Float)

@Composable
private fun rememberStableStars(count: Int): List<Star> =
    androidx.compose.runtime.remember(count) {
        val rnd = Random(42)
        List(count) {
            Star(
                x         = rnd.nextFloat(),
                y         = rnd.nextFloat(),
                radius    = 0.6f + rnd.nextFloat() * 1.8f,
                baseAlpha = 0.25f + rnd.nextFloat() * 0.55f
            )
        }
    }
