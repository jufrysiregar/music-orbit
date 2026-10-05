package com.musicorbit.domain.repository

import com.musicorbit.domain.model.Lyrics

interface LyricsRepository {
    suspend fun getLyricsBySongId(songId: Long): Lyrics?
    suspend fun saveLyrics(lyrics: Lyrics)
    suspend fun deleteLyrics(songId: Long)
}
