package com.musicorbit.android.service

import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.musicorbit.platform.AudioPlayer
import org.koin.android.ext.android.inject

/**
 * MediaSessionService that enables background audio playback and system media controls
 * (notification + lock-screen / Bluetooth / wired-headset buttons).
 *
 * The ExoPlayer and its MediaSession live inside the [AudioPlayer] Koin singleton so that
 * they survive Activity teardown. The service simply borrows that session and publishes it
 * through [onGetSession]. Media3 promotes the service to the foreground (and posts the media
 * notification) automatically once playback starts.
 */
class MusicService : MediaSessionService() {

    private val audioPlayer: AudioPlayer by inject()

    // The session is owned by AudioPlayer; we must NOT release it here.
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        mediaSession = audioPlayer.getMediaSession()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onDestroy() {
        // Only drop our reference; AudioPlayer.release() owns the real lifecycle.
        mediaSession = null
        super.onDestroy()
    }
}
