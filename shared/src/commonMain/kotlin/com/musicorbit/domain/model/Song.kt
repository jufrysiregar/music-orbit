package com.musicorbit.domain.model

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,           // milliseconds
    val filePath: String,
    val albumArtUri: String?,
    val playCount: Int,
    val dateAdded: Long,          // epoch millis
    val hasLyrics: Boolean = false
)
