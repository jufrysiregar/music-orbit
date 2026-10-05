package com.musicorbit.android.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme  = darkColorScheme()
private val LightColorScheme = lightColorScheme()

@Composable
fun MusicOrbitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color uses Material You wallpaper colors on Android 12+
    dynamicColor: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && darkTheme  -> dynamicDarkColorScheme(LocalContext.current)
        dynamicColor && !darkTheme -> dynamicLightColorScheme(LocalContext.current)
        darkTheme                  -> DarkColorScheme
        else                       -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = MusicOrbitTypography,
        content     = content
    )
}
