package com.musicorbit.android.service

import android.content.Intent
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.musicorbit.android.R
import com.musicorbit.platform.AudioPlayer
import org.koin.android.ext.android.inject

/**
 * MediaSessionService that enables background audio playback and system media controls
 * (notification shade + lock-screen / Bluetooth / wired-headset buttons).
 *
 * The ExoPlayer and its MediaSession live inside the [AudioPlayer] Koin singleton so they survive
 * Activity teardown. The service borrows that session via [onGetSession].
 *
 * Item 7: we let Media3 own the notification through [DefaultMediaNotificationProvider]. That
 * produces a native MediaStyle notification with Play/Pause, Previous, Next and a Stop ("X")
 * action, plus a swipe-to-dismiss delete intent — and it promotes the service to the foreground
 * (FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK) automatically while playing. This replaces an earlier
 * control-less persistent notification so there is exactly ONE, fully actionable media notification
 * that stays on the shade / lock screen even when the app is minimized or removed from Recents.
 * Doze/screen-off playback is kept alive by the PARTIAL_WAKE_LOCK held inside [AudioPlayer].
 */
class MusicService : MediaSessionService() {

    private val audioPlayer: AudioPlayer by inject()

    // The session is owned by AudioPlayer; we must NOT release it here.
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        mediaSession = audioPlayer.getMediaSession()
        // Rich MediaStyle notification with transport controls + stop/dismiss action.
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider(this).apply {
                setSmallIcon(R.drawable.ic_stat_music)
            }
        )
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    /**
     * Return START_STICKY so the system will recreate the service if it is ever killed while
     * nothing is playing. Media3 re-posts the foreground media notification on its own as soon as
     * playback resumes. [intent] may be null on a sticky restart.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    /**
     * Keep playback alive when the user dismisses the app from Recents — matching every other
     * music app. The default MediaSessionService.onTaskRemoved() stops the service whenever no
     * MediaController is bound, and our UI drives the shared ExoPlayer directly (never binds a
     * controller), so calling super here while playing is precisely what kills the music.
     *
     * Therefore: while playing we intentionally do NOT call super — the service stays foreground
     * and audio continues; the user can still stop it from the notification "X" or the home widget.
     * Only when nothing is playing do we tear the service down.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!audioPlayer.isPlaying()) {
            stopSelf()
            super.onTaskRemoved(rootIntent)
        }
        // else: keep running in the background with the ongoing media notification.
    }

    override fun onDestroy() {
        // Only drop our reference; AudioPlayer.release() owns the real lifecycle.
        mediaSession = null
        super.onDestroy()
    }
}
