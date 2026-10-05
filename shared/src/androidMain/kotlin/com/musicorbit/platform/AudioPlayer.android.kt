package com.musicorbit.platform

import android.content.Context
import android.net.Uri
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

    /** Exposed so EqualizerEngine can bind to the correct audio session. */
    val audioSessionId: Int get() = exoPlayer.audioSessionId

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
    actual fun skipNext()               { exoPlayer.seekToNextMediaItem() }
    actual fun skipPrevious()           { exoPlayer.seekToPreviousMediaItem() }

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
                exoPlayer.repeatMode = Player.REPEAT_MODE_OFF
                exoPlayer.shuffleModeEnabled = true
            }
        }
    }

    actual fun getCurrentPosition(): Long = exoPlayer.currentPosition
    actual fun getDuration(): Long =
        exoPlayer.duration.takeIf { it != C.TIME_UNSET } ?: 0L

    actual fun isPlaying(): Boolean = exoPlayer.isPlaying

    actual fun release() {
        mediaSession.release()
        exoPlayer.release()
    }
}
