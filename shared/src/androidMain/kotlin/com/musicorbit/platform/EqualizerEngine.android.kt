package com.musicorbit.platform

import android.media.audiofx.Equalizer

actual class EqualizerEngine {

    private var equalizer: Equalizer? = null

    actual fun initialize(audioSessionId: Int) {
        runCatching { equalizer?.release() }
        equalizer = runCatching {
            Equalizer(0, audioSessionId).apply { enabled = true }
        }.getOrNull()
    }

    actual fun getBandLevels(): List<Int> {
        val eq = equalizer ?: return emptyList()
        return (0 until eq.numberOfBands).map { eq.getBandLevel(it.toShort()).toInt() }
    }

    actual fun getBandFrequencies(): List<Int> {
        val eq = equalizer ?: return emptyList()
        // getCenterFreq returns milli-Hz; divide by 1000 for Hz
        return (0 until eq.numberOfBands).map { eq.getCenterFreq(it.toShort()) / 1000 }
    }

    actual fun setBandLevel(bandIndex: Int, levelMilliDb: Int) {
        equalizer?.setBandLevel(bandIndex.toShort(), levelMilliDb.toShort())
    }

    actual fun applyPreset(presetName: String) {
        val eq = equalizer ?: return
        val idx = (0 until eq.numberOfPresets)
            .firstOrNull { eq.getPresetName(it.toShort()).equals(presetName, ignoreCase = true) }
            ?.toShort()
        idx?.let { eq.usePreset(it) }
    }

    actual fun setEnabled(enabled: Boolean) {
        equalizer?.enabled = enabled
    }

    actual fun release() {
        runCatching { equalizer?.release() }
        equalizer = null
    }
}
