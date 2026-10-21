package com.musicorbit.android.ui.util

/**
 * Serialize/deserialize equalizer band levels as a simple "[a,b,c]" string, matching the
 * format documented on PreferencesRepository.getEqualizerBands(). Kept dependency-free so both
 * PlayerViewModel (startup restore) and EqualizerViewModel (live edits) can share it.
 */
object EqualizerCodec {
    fun toJson(levels: List<Int>): String = levels.joinToString(",", "[", "]")

    fun fromJson(raw: String?): List<Int> {
        val body = raw?.trim()?.removePrefix("[")?.removeSuffix("]") ?: return emptyList()
        if (body.isBlank()) return emptyList()
        return body.split(",").mapNotNull { it.trim().toIntOrNull() }
    }
}
