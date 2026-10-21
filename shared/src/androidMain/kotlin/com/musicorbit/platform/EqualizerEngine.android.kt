package com.musicorbit.platform

import android.media.audiofx.Equalizer

actual class EqualizerEngine {

    private var equalizer: Equalizer? = null
    private var minLevel: Int = -1500
    private var maxLevel: Int = 1500
    // Session id the effect is currently attached to, so re-initializing the same session is a no-op.
    private var boundSessionId: Int = -1

    actual fun initialize(audioSessionId: Int) {
        // Idempotent: if we are already bound to this exact session, keep the existing effect and
        // its band levels. Releasing + recreating on every EqualizerScreen visit reset the bands and
        // forced enabled=true, which is what made the switch show OFF while the effect was actually
        // on and required repeated ON/OFF toggling to "bind".
        if (equalizer != null && boundSessionId == audioSessionId) return
        runCatching { equalizer?.release() }
        equalizer = runCatching { Equalizer(0, audioSessionId) }.getOrNull()
        boundSessionId = audioSessionId
        equalizer?.let { eq ->
            eq.enabled = false // caller applies the persisted enabled state
            val range = eq.bandLevelRange  // [min, max] in mB
            if (range.size >= 2) {
                minLevel = range[0].toInt()
                maxLevel = range[1].toInt()
            }
        }
    }

    actual fun getBandLevels(): List<Int> {
        val eq = equalizer ?: return emptyList()
        val numBands = eq.numberOfBands.toInt()
        return (0 until numBands).map { eq.getBandLevel(it.toShort()).toInt() }
    }

    actual fun getBandFrequencies(): List<Int> {
        val eq = equalizer ?: return emptyList()
        val numBands = eq.numberOfBands.toInt()
        return (0 until numBands).map { eq.getCenterFreq(it.toShort()) / 1000 }
    }

    actual fun getLevelRange(): Pair<Int, Int> = minLevel to maxLevel

    actual fun setBandLevel(bandIndex: Int, levelMilliDb: Int) {
        val eq = equalizer ?: return
        if (bandIndex < 0 || bandIndex >= eq.numberOfBands) return // persisted bands from another device
        val clamped = levelMilliDb.coerceIn(minLevel, maxLevel)
        eq.setBandLevel(bandIndex.toShort(), clamped.toShort())
    }

    actual fun applyPreset(presetName: String) {
        val eq = equalizer ?: return
        val numBands = eq.numberOfBands.toInt()
        val preset = PRESETS[presetName] ?: PRESETS["Normal"]!!
        for (i in 0 until numBands) {
            // Scale preset value (percentage -100..100) to device range
            val scaled = if (preset[i % preset.size] >= 0) {
                (preset[i % preset.size] * maxLevel / 100)
            } else {
                (preset[i % preset.size] * -minLevel / 100)
            }
            eq.setBandLevel(i.toShort(), scaled.coerceIn(minLevel, maxLevel).toShort())
        }
    }

    actual fun setEnabled(enabled: Boolean) {
        equalizer?.enabled = enabled
    }

    actual fun isEnabled(): Boolean = equalizer?.enabled ?: false

    actual fun release() {
        runCatching { equalizer?.release() }
        equalizer = null
    }

    companion object {
        // Preset band levels as percentages (-100 to 100) relative to device range
        // Ordered: bass-low, bass, low-mid, mid, high-mid, treble-low, treble, treble-high
        val PRESETS: Map<String, List<Int>> = mapOf(
            "Normal"     to listOf(0, 0, 0, 0, 0, 0, 0, 0),
            "Pop"        to listOf(-2, 0, 3, 5, 4, 1, 0, -1),
            "Rock"       to listOf(5, 4, 2, -1, -2, 1, 4, 5),
            "Jazz"       to listOf(3, 2, 0, 2, 3, 3, 2, 1),
            "Classical"  to listOf(4, 3, 1, 0, -1, -2, 0, 3),
            "Dance"      to listOf(6, 5, 1, -2, -2, 1, 4, 3),
            "Hip-Hop"    to listOf(6, 5, 1, 0, -2, 1, 2, 3),
            "Vocal"      to listOf(-3, -2, 1, 4, 5, 4, 2, 0),
            "Acoustic"   to listOf(2, 1, 0, 1, 2, 2, 1, 0),
            "Electronic" to listOf(5, 4, 0, -2, 1, 3, 5, 4),
        )
    }
}
