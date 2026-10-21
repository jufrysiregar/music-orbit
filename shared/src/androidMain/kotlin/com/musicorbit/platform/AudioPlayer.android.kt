package com.musicorbit.platform

import android.content.Context
import android.net.Uri
import android.os.PowerManager
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import com.musicorbit.domain.model.PlayMode

actual class AudioPlayer(private val context: Context) {

    private val exoPlayer: ExoPlayer = ExoPlayer.Builder(context).build()

    private val mediaSession: MediaSession =
        MediaSession.Builder(context, exoPlayer)
            .setId("MusicOrbitSession")
            .build()

    // PARTIAL_WAKE_LOCK keeps the CPU awake while audio is playing, so playback doesn't stall
    // once the screen turns off / the device enters Doze. Reference-counted off for simple hold/release.
    private val wakeLock: PowerManager.WakeLock by lazy {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MusicOrbit:PlaybackWakeLock")
            .apply { setReferenceCounted(false) }
    }

    private var playbackListener: ((Boolean) -> Unit)? = null

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    // 10h safety timeout so a leaked lock can never keep the CPU awake forever.
                    if (!wakeLock.isHeld) wakeLock.acquire(10L * 60L * 60L * 1000L)
                } else if (wakeLock.isHeld) {
                    wakeLock.release()
                }
                playbackListener?.invoke(isPlaying)
            }
        })
    }

    /**
     * Exposes the singleton [MediaSession] so [com.musicorbit.android.service.MusicService]
     * can publish it via MediaSessionService.onGetSession(). This is what enables the system
     * media notification and background playback. There is exactly ONE session for the whole app.
     */
    fun getMediaSession(): MediaSession = mediaSession

    /** Exposed so EqualizerEngine can bind to the correct audio session. */
    val audioSessionId: Int get() = exoPlayer.audioSessionId

    actual fun setQueue(items: List<Pair<String, Long>>, startIndex: Int) {
        val mediaItems = items.map { (filePath, songId) ->
            MediaItem.Builder()
                .setMediaId(songId.toString())
                .setUri(Uri.parse(filePath))
                .build()
        }
        exoPlayer.setMediaItems(mediaItems, startIndex.coerceIn(0, mediaItems.size - 1), 0L)
        exoPlayer.prepare()
        exoPlayer.play()
    }

    actual fun playSong(filePath: String, songId: Long) {
        val mediaItem = MediaItem.Builder()
            .setMediaId(songId.toString())
            .setUri(Uri.parse(filePath))
            .build()
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.play()
    }

    actual fun pause()  { exoPlayer.pause() }
    actual fun resume() { exoPlayer.play() }
    actual fun stop()   { exoPlayer.stop(); exoPlayer.seekTo(0) }

    actual fun seekTo(positionMs: Long) { exoPlayer.seekTo(positionMs) }
    actual fun skipNext()     { exoPlayer.seekToNext(); if (exoPlayer.currentMediaItem != null) exoPlayer.play() }
    actual fun skipPrevious() { exoPlayer.seekToPrevious(); if (exoPlayer.currentMediaItem != null) exoPlayer.play() }
    actual fun getCurrentQueueIndex(): Int = exoPlayer.currentMediaItemIndex

    actual fun setPlayMode(mode: PlayMode) {
        when (mode) {
            PlayMode.REPEAT_ONE -> {
                exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
                exoPlayer.shuffleModeEnabled = false
            }
            PlayMode.SEQUENTIAL -> {
                exoPlayer.repeatMode = Player.REPEAT_MODE_OFF
                exoPlayer.shuffleModeEnabled = false
            }
            PlayMode.SHUFFLE -> {
                exoPlayer.repeatMode = Player.REPEAT_MODE_ALL
                exoPlayer.shuffleModeEnabled = true
            }
        }
    }

    actual fun getCurrentPosition(): Long = exoPlayer.currentPosition
    actual fun getDuration(): Long =
        exoPlayer.duration.takeIf { it != C.TIME_UNSET } ?: 0L

    actual fun isPlaying(): Boolean = exoPlayer.isPlaying

    actual fun setPlaybackStateListener(listener: ((isPlaying: Boolean) -> Unit)?) {
        playbackListener = listener
    }

    actual fun release() {
        if (wakeLock.isHeld) runCatching { wakeLock.release() }
        playbackListener = null
        mediaSession.release()
        exoPlayer.release()
    }
}
