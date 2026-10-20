package com.musicorbit.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import com.musicorbit.domain.repository.PreferencesRepository
import org.koin.compose.koinInject

/** CompositionLocal telling descendants whether the deep-space (dark) variant is active. */
val LocalOrbitDark = staticCompositionLocalOf { true }

private val DarkColorScheme = darkColorScheme(
    primary            = OrbitViolet,
    onPrimary          = SpaceOnSurface,
    primaryContainer   = SpaceSurfaceHigh,
    onPrimaryContainer = SpaceOnSurface,
    secondary          = OrbitCyan,
    onSecondary        = SpaceBackground,
    secondaryContainer = SpaceSurfaceVariant,
    onSecondaryContainer = OrbitCyan,
    tertiary           = OrbitMagenta,
    onTertiary         = SpaceOnSurface,
    background         = SpaceBackground,
    onBackground       = SpaceOnSurface,
    surface            = SpaceSurface,
    onSurface          = SpaceOnSurface,
    surfaceVariant     = SpaceSurfaceVariant,
    onSurfaceVariant   = SpaceOnSurfaceMuted,
    surfaceTint        = OrbitViolet,
    outline            = SpaceOutline,
    outlineVariant     = SpaceOutline,
    error              = Color(0xFFFF6B6B)
)

private val LightColorScheme = lightColorScheme(
    primary            = OrbitViolet,
    onPrimary          = DaySurface,
    primaryContainer   = Color(0xFFE4DBFF),
    onPrimaryContainer = DayOnSurface,
    secondary          = OrbitBlue,
    onSecondary        = DaySurface,
    secondaryContainer = Color(0xFFCFEBF7),
    onSecondaryContainer = DayOnSurface,
    tertiary           = OrbitMagenta,
    onTertiary         = DaySurface,
    background         = DayBackground,
    onBackground       = DayOnSurface,
    surface            = DaySurface,
    onSurface          = DayOnSurface,
    surfaceVariant     = DaySurfaceVariant,
    onSurfaceVariant   = DayOnSurfaceMuted,
    surfaceTint        = OrbitViolet,
    outline            = DayOutline,
    outlineVariant     = DayOutline
)

/** App-wide corner radius scale for the premium, soft-edged look. */
val OrbitShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small      = RoundedCornerShape(14.dp),
    medium     = RoundedCornerShape(20.dp),
    large      = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

@Composable
fun MusicOrbitTheme(
    content: @Composable () -> Unit
) {
    val preferences: PreferencesRepository = koinInject()
    val theme by preferences.getTheme()
        .collectAsState(initial = "SYSTEM")

    val darkTheme = when (theme) {
        "LIGHT" -> false
        "DARK"  -> true
        else    -> isSystemInDarkTheme()
    }

    // Brand consistency takes priority over Material You dynamic color.
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    androidx.compose.runtime.CompositionLocalProvider(
        LocalOrbitDark provides darkTheme
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = MusicOrbitTypography,
            shapes      = OrbitShapes,
            content     = content
        )
    }
}
