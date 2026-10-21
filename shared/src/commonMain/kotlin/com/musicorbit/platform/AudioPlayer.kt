package com.musicorbit.platform

import com.musicorbit.domain.model.PlayMode

/**
 * Platform-specific audio playback engine.
 * Android actual: Media3 ExoPlayer
 * iOS actual:     AVFoundation (stub)
 */
expect class AudioPlayer {
    fun setQueue(items: List<Pair<String, Long>>, startIndex: Int)
    fun playSong(filePath: String, songId: Long)
    fun pause()
    fun resume()
    fun stop()
    fun seekTo(positionMs: Long)
    fun skipNext()
    fun skipPrevious()
    fun getCurrentQueueIndex(): Int
    fun setPlayMode(mode: PlayMode)
    fun getCurrentPosition(): Long
    fun getDuration(): Long
    fun isPlaying(): Boolean
    fun release()

    /**
     * Registers a callback invoked immediately whenever playback starts/stops, driven by the
     * platform player's own listener (not polling). Pass null to clear. Used to keep the UI and
     * the home-screen widget in sync even after long idle periods or when the app is backgrounded.
     */
    fun setPlaybackStateListener(listener: ((isPlaying: Boolean) -> Unit)?)

    /**
     * Registers a callback invoked immediately when the current media item changes (Next/Prev,
     * auto-advance, or a control coming from the system notification / home-screen widget).
     * Receives the new queue index. Pass null to clear. Used for real-time footer/title sync.
     */
    fun setTrackChangeListener(listener: ((queueIndex: Int) -> Unit)?)
}
