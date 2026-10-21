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
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    /** Set when the widget's album art / launcher icon deep-links us into the player screen. */
    private val deepLinkPlayer = MutableStateFlow(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Start the MediaSessionService once so it can publish the shared MediaSession.
        // This is what keeps audio alive in the background and shows the media notification.
        startService(Intent(this, MusicService::class.java))

        deepLinkPlayer.value = intent?.getBooleanExtra(EXTRA_OPEN_PLAYER, false) == true

        enableEdgeToEdge()
        setContent {
            MusicOrbitTheme {
                PermissionScreen {
                    AppNavGraph(deepLinkPlayer = deepLinkPlayer, onExitApp = { finish() })
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_OPEN_PLAYER, false)) {
            deepLinkPlayer.value = false
            deepLinkPlayer.value = true
        }
    }

    companion object {
        const val EXTRA_OPEN_PLAYER = "EXTRA_OPEN_PLAYER"
        const val EXTRA_TRACK_ID    = "track_id"
    }
}
