@file:OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)

package com.musicorbit.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.musicorbit.android.service.MusicService
import com.musicorbit.android.ui.navigation.AppNavGraph
import com.musicorbit.android.ui.screen.permission.PermissionScreen
import com.musicorbit.android.ui.theme.MusicOrbitTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Start the MediaSessionService once so it can publish the shared MediaSession.
        // This is what keeps audio alive in the background and shows the media notification.
        startService(Intent(this, MusicService::class.java))

        enableEdgeToEdge()
        setContent {
            MusicOrbitTheme {
                PermissionScreen {
                    AppNavGraph(onExitApp = { finish() })
                }
            }
        }
    }
}
