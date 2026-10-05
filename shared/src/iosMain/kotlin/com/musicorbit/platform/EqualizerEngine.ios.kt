package com.musicorbit.platform

/**
 * iOS stub — AVAudioUnitEQ implementation to be added in a future sprint.
 * All methods are no-ops; band queries return empty lists.
 */
actual class EqualizerEngine {
    actual fun initialize(audioSessionId: Int) {} // TODO: AVAudioUnitEQ
    actual fun getBandLevels(): List<Int>       = emptyList()
    actual fun getBandFrequencies(): List<Int>  = emptyList()
    actual fun setBandLevel(bandIndex: Int, levelMilliDb: Int) {}
    actual fun applyPreset(presetName: String) {}
    actual fun setEnabled(enabled: Boolean) {}
    actual fun release() {}
}
