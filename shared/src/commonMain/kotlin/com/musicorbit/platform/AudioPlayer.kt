package com.musicorbit.platform

import com.musicorbit.domain.model.PlayMode

/**
 * Platform-specific audio playback engine.
 * Android actual: Media3 ExoPlayer
 * iOS actual:     AVFoundation (stub)
 */
expect class AudioPlayer {
    fun setQueue(items: List<Pair<String, Long>>, startIndex: Int)
    /**
     * Re-order the current queue in place (same window, new order/index) without re-preparing the
     * player, so ongoing audio playback stays perfectly smooth when the sort order changes.
     */
    fun reorderQueue(items: List<Pair<String, Long>>, newIndex: Int)
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

    /**
     * Whether the user intends playback (playWhenReady). Unlike [isPlaying] this stays true while
     * the player is buffering, so UI state never flickers between "playing" and "paused" during
     * short stalls. Use this — not [isPlaying] — to drive play/pause button visuals.
     */
    fun isPlayIntent(): Boolean
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
