package com.musicorbit.android.ui.screen.permission

import android.Manifest
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.musicorbit.android.R
import com.musicorbit.android.ui.components.GlassCard
import com.musicorbit.android.ui.components.OrbitBackground
import com.musicorbit.android.ui.theme.OrbitViolet
import com.musicorbit.android.ui.theme.auroraGradient
import com.musicorbit.android.ui.util.pressableSpring

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

    // On Android 13+ the media notification is required for background playback controls,
    // so we also ask for POST_NOTIFICATIONS once audio access is granted.
    val notificationState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        null
    }

    when {
        permissionState.status.isGranted -> {
            // Fire the notification request the first time (non-blocking for content).
            LaunchedEffect(Unit) {
                if (notificationState != null && !notificationState.status.isGranted) {
                    notificationState.launchPermissionRequest()
                }
            }
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
        OrbitBackground()

        GlassCard(
            modifier       = Modifier.padding(horizontal = 28.dp),
            shape          = RoundedCornerShape(28.dp),
            contentPadding = 28.dp,
            glow           = true
        ) {
            Column(
                modifier                = Modifier.fillMaxWidth(),
                horizontalAlignment     = Alignment.CenterHorizontally,
                verticalArrangement     = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .shadow(18.dp, RoundedCornerShape(22.dp), ambientColor = OrbitViolet, spotColor = OrbitViolet, clip = false)
                        .clip(RoundedCornerShape(22.dp))
                ) {
                    Image(
                        painter            = painterResource(R.drawable.app_logo),
                        contentDescription = "Music Orbit",
                        modifier           = Modifier.fillMaxSize()
                    )
                }
                Text(
                    text      = "Izin Akses Musik",
                    style     = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text      = "Music Orbit perlu izin untuk membaca file audio di perangkat kamu supaya bisa menampilkan dan memutar musik, serta menampilkan kontrol di notifikasi.",
                    style     = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .shadow(14.dp, RoundedCornerShape(16.dp), ambientColor = OrbitViolet, spotColor = OrbitViolet, clip = false)
                        .clip(RoundedCornerShape(16.dp))
                        .background(auroraGradient(), RoundedCornerShape(16.dp))
                        .pressableSpring(onClick = onRequest),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text     = "Berikan Izin",
                        style    = MaterialTheme.typography.titleMedium,
                        color    = Color.White
                    )
                }
            }
        }
    }
}
