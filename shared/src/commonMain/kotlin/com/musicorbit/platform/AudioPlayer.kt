package com.musicorbit.platform

import com.musicorbit.domain.model.PlayMode

/**
 * Platform-specific audio playback engine.
 * Android actual: Media3 ExoPlayer
 * iOS actual:     AVFoundation (stub)
 */
expect class AudioPlayer {
    fun playSong(filePath: String, songId: Long)
    fun pause()
    fun resume()
    fun stop()
    fun seekTo(positionMs: Long)
    fun skipNext()
    fun skipPrevious()
    fun setPlayMode(mode: PlayMode)
    fun getCurrentPosition(): Long
    fun getDuration(): Long
    fun isPlaying(): Boolean
    fun release()
}
