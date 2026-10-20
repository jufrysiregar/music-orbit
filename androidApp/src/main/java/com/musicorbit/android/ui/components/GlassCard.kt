package com.musicorbit.android.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.musicorbit.android.ui.theme.LocalOrbitDark

/**
 * Frosted-glass surface used across the redesign.
 * Layered translucent fill + a subtle gradient stroke simulate glass on every API level
 * (real backdrop blur needs API 31+, so we fake it with tinted translucency + sheen).
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    contentPadding: Dp = 16.dp,
    borderColor: Color? = null,
    glow: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = LocalOrbitDark.current
    val scheme = MaterialTheme.colorScheme

    // Translucent glass fill: darker/subtle in space, brighter in day mode.
    val glassFill = if (isDark) Color.White.copy(alpha = 0.06f)
                    else Color.White.copy(alpha = 0.65f)

    val primaryTint = borderColor ?: scheme.primary
    val tertiaryTint = if (borderColor != null) borderColor else scheme.tertiary

    val sheenBrush = Brush.verticalGradient(
        colors = listOf(glassFill.copy(alpha = glassFill.alpha + 0.05f), glassFill)
    )

    val borderBrush = Brush.linearGradient(
        colors = listOf(
            primaryTint.copy(alpha = 0.55f),
            Color.White.copy(alpha = if (isDark) 0.12f else 0.40f),
            tertiaryTint.copy(alpha = 0.45f)
        ),
        start = Offset.Zero,
        end   = Offset.Infinite
    )

    Box(
        modifier = modifier
            .clip(shape)
            .drawBehind {
                drawRect(brush = sheenBrush)
                // soft diagonal sheen highlight
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.White.copy(alpha = 0.10f), Color.Transparent),
                        start  = Offset.Zero,
                        end    = Offset(size.width, size.height)
                    )
                )
                // bottom tint fade for depth
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(primaryTint.copy(alpha = if (isDark) 0.22f else 0.30f), Color.Transparent),
                        startY = size.height * 0.7f
                    )
                )
                if (glow) {
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(scheme.primary.copy(alpha = 0.18f), Color.Transparent),
                            center = Offset(size.width / 2f, size.height / 2f),
                            radius = size.maxDimension
                        )
                    )
                }
            }
            .border(BorderStroke(1.dp, borderBrush), shape)
    ) {
        Box(
            modifier = Modifier.padding(contentPadding),
            content  = content
        )
    }
}
