package com.musicorbit.platform

import android.content.Context
import android.net.Uri
import android.os.PowerManager
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import com.musicorbit.domain.model.PlayMode

actual class AudioPlayer(private val context: Context) {

    private val exoPlayer: ExoPlayer = ExoPlayer.Builder(context).build().apply {
        // USAGE_MEDIA + CONTENT_TYPE_MUSIC with handleAudioFocus=true gives proper audio focus
        // (ducks/pauses for calls) and correct routing on the system media session.
        setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build(),
            /* handleAudioFocus = */ true
        )
        // CPU wake lock held by the player itself during playback -> survives screen-off / Doze.
        setWakeMode(C.WAKE_MODE_LOCAL)
        setHandleAudioBecomingNoisy(true)
    }

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
    private var trackListener: ((Int) -> Unit)? = null

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

            // Fires on Next/Prev, auto-advance, and remote control/widget actions, so the UI can
            // reflect the newly playing track in real time (not only via polling).
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                trackListener?.invoke(exoPlayer.currentMediaItemIndex)
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

    actual fun reorderQueue(items: List<Pair<String, Long>>, newIndex: Int) {
        if (items.isEmpty()) return
        val mediaItems = items.map { (filePath, songId) ->
            MediaItem.Builder()
                .setMediaId(songId.toString())
                .setUri(Uri.parse(filePath))
                .build()
        }
        // Replace the window in place and jump to the re-indexed current item WITHOUT calling
        // prepare()/play(). The player is already prepared and playWhenReady is preserved, so the
        // currently playing audio keeps flowing smoothly across a sort change (no reload stutter).
        exoPlayer.setMediaItems(mediaItems, newIndex.coerceIn(0, mediaItems.size - 1), exoPlayer.currentPosition)
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
    // seekToNext/PreviousMediaItem honour the player's shuffle order and repeat mode set via
    // setPlayMode, so widget/app navigation follows the active playback mode automatically.
    actual fun skipNext()     { exoPlayer.seekToNextMediaItem(); if (exoPlayer.currentMediaItem != null) exoPlayer.play() }
    actual fun skipPrevious() { exoPlayer.seekToPreviousMediaItem(); if (exoPlayer.currentMediaItem != null) exoPlayer.play() }

    /** Media id of the item currently loaded in the player, i.e. the song's database id. */
    fun getCurrentMediaId(): Long? = exoPlayer.currentMediaItem?.mediaId?.toLongOrNull()
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

    actual fun setTrackChangeListener(listener: ((queueIndex: Int) -> Unit)?) {
        trackListener = listener
    }

    actual fun release() {
        if (wakeLock.isHeld) runCatching { wakeLock.release() }
        playbackListener = null
        trackListener = null
        mediaSession.release()
        exoPlayer.release()
    }
}
