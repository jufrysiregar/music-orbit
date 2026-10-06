package com.musicorbit.android.ui.screen.permission

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

/**
 * Gates the app behind an audio-read permission.
 *
 * Android 13+ : READ_MEDIA_AUDIO
 * Android < 13: READ_EXTERNAL_STORAGE
 *
 * Immediately launches the system dialog on first run.
 * Shows a rationale screen if the user previously denied.
 * Renders [content] once permission is granted.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionScreen(content: @Composable () -> Unit) {
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val permissionState = rememberPermissionState(permission)

    when {
        permissionState.status.isGranted -> {
            content()
        }

        permissionState.status.shouldShowRationale -> {
            // User denied once — explain why we need it
            PermissionRationaleContent(
                onRequest = { permissionState.launchPermissionRequest() }
            )
        }

        else -> {
            // First launch: fire the system dialog immediately
            LaunchedEffect(Unit) {
                permissionState.launchPermissionRequest()
            }
            PermissionRationaleContent(
                onRequest = { permissionState.launchPermissionRequest() }
            )
        }
    }
}

@Composable
private fun PermissionRationaleContent(onRequest: () -> Unit) {
    Box(
        modifier         = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier                = Modifier.padding(32.dp),
            horizontalAlignment     = Alignment.CenterHorizontally,
            verticalArrangement     = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector        = Icons.Default.MusicNote,
                contentDescription = null,
                modifier           = Modifier.size(72.dp),
                tint               = MaterialTheme.colorScheme.primary
            )
            Text(
                text      = "Izin Akses Musik",
                style     = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            Text(
                text      = "Music Orbit perlu izin untuk membaca file audio di perangkat kamu supaya bisa menampilkan dan memutar musik.",
                style     = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color     = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick  = onRequest,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Berikan Izin")
            }
        }
    }
}
