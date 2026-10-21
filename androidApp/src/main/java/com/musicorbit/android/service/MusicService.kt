package com.musicorbit.android.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.musicorbit.android.MainActivity
import com.musicorbit.android.R
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
 *
 * We additionally raise a persistent foreground notification at start and return START_STICKY
 * so the OS keeps the service (and the PARTIAL_WAKE_LOCK held by [AudioPlayer]) alive through
 * screen-off / Doze, preventing playback from stalling a couple of minutes after the screen locks.
 */
class MusicService : MediaSessionService() {

    private val audioPlayer: AudioPlayer by inject()

    // The session is owned by AudioPlayer; we must NOT release it here.
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        mediaSession = audioPlayer.getMediaSession()
        createNotificationChannel()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    /**
     * Keep the service alive and foreground immediately: a persistent notification plus
     * START_STICKY makes the system restart it if it is ever killed, so background audio survives
     * Doze / screen-off. [intent] may be null on a sticky restart.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundCompat(buildPersistentNotification())
        return START_STICKY
    }

    /**
     * Keep playback alive when the user dismisses the app from Recents. Because the player is
     * a started foreground service (promoted by Media3 while playing), removing the task should
     * NOT stop the music. We only tear the service down when nothing is actually playing.
     */
    override fun onTaskRemoved(rootIntent: android.content.Intent?) {
        if (!audioPlayer.isPlaying()) {
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        // Only drop our reference; AudioPlayer.release() owns the real lifecycle.
        mediaSession = null
        super.onDestroy()
    }

    // ── Foreground notification ──────────────────────────────────────────────

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Orbit Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps music playing in the background"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildPersistentNotification(): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Music Orbit")
            .setContentText("Music is playing")
            .setSmallIcon(R.drawable.ic_stat_music)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIF_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIF_ID, notification)
        }
    }

    companion object {
        private const val NOTIF_ID = 1001
        private const val CHANNEL_ID = "music_orbit_playback"
    }
}
