package com.musicorbit.platform

/**
 * Platform-specific equalizer engine.
 * Android actual: android.media.audiofx.Equalizer
 * iOS actual:     AVAudioUnitEQ (stub)
 *
 * [audioSessionId] is the ExoPlayer audio session ID on Android.
 * Pass 0 on iOS (stub ignores it).
 */
expect class EqualizerEngine {
    fun initialize(audioSessionId: Int)
    fun getBandLevels(): List<Int>
    fun getBandFrequencies(): List<Int>
    fun setBandLevel(bandIndex: Int, levelMilliDb: Int)
    fun applyPreset(presetName: String)
    fun setEnabled(enabled: Boolean)
    fun release()
}
