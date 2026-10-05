package com.musicorbit.domain.model

/**
 * Platform Transfer Object returned by [expect class MusicScanner].
 * Plain data class with no platform dependencies — safe to use in commonMain.
 */
data class SongData(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,           // milliseconds
    val filePath: String,
    val albumArtUri: String?,
    val dateAdded: Long           // epoch millis
)
