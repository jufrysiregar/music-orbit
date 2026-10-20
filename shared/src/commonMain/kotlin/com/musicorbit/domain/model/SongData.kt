package com.musicorbit.domain.model

/**
 * Platform Transfer Object returned by [expect class MusicScanner].
 * Plain data class with no platform dependencies — safe to use in commonMain.
 *
 * [realFilePath] is the actual filesystem path (from MediaStore DATA column).
 * Used only during scanning to locate sibling .lrc/.txt lyrics files.
 * It is NOT persisted to the database.
 */
data class SongData(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,           // milliseconds
    val filePath: String,         // content URI used for playback
    val albumArtUri: String?,
    val dateAdded: Long,          // epoch millis
    val realFilePath: String? = null  // actual file path on disk (null on iOS)
)
