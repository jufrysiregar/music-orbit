package com.musicorbit.domain.model

data class Lyrics(
    val songId: Long,
    val lyricsText: String,
    val hasLrc: Boolean = false   // true if a .lrc file is linked
)
