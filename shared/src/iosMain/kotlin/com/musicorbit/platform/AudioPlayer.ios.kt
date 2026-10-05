package com.musicorbit.platform

import com.musicorbit.domain.model.PlayMode

/**
 * iOS stub — AVFoundation implementation to be added in a future sprint.
 * All methods are no-ops; isPlaying() always returns false.
 */
actual class AudioPlayer {
    actual fun playSong(filePath: String, songId: Long) { /* TODO: AVPlayer */ }
    actual fun pause()   {}
    actual fun resume()  {}
    actual fun stop()    {}
    actual fun seekTo(positionMs: Long) {}
    actual fun skipNext()     {}
    actual fun skipPrevious() {}
    actual fun setPlayMode(mode: PlayMode) {}
    actual fun getCurrentPosition(): Long = 0L
    actual fun getDuration(): Long        = 0L
    actual fun isPlaying(): Boolean       = false
    actual fun release() {}
}
