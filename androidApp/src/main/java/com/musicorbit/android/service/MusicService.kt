package com.musicorbit.android.service

import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.musicorbit.platform.AudioPlayer
import org.koin.android.ext.android.inject

/**
 * MediaSessionService enabling background audio playback and system media controls.
 * The AudioPlayer actual (ExoPlayer) is injected from the Koin androidModule.
 */
class MusicService : MediaSessionService() {

    private val audioPlayer: AudioPlayer by inject()
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        // AudioPlayer.android.kt already creates the MediaSession internally.
        // We expose it here so the system can connect media controllers.
        // Phase 3 will wire the full session lifecycle.
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onDestroy() {
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }
}
