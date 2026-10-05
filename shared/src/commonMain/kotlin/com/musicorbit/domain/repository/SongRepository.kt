package com.musicorbit.domain.repository

import com.musicorbit.domain.model.Song
import com.musicorbit.domain.model.SongData
import kotlinx.coroutines.flow.Flow

interface SongRepository {
    /** All songs sorted by title (A-Z). */
    fun getAllSongs(): Flow<List<Song>>

    /** All songs with hasLyrics flag resolved via LEFT JOIN with Lyrics table. */
    fun getSongsWithLyrics(): Flow<List<Song>>

    /** Live search by title or artist (case-insensitive, real-time). */
    fun searchSongs(query: String): Flow<List<Song>>

    /** Insert or replace a single song (used for metadata edits). */
    suspend fun upsertSong(song: Song)

    /** Bulk insert / replace after a MediaStore scan. */
    suspend fun upsertSongs(songs: List<SongData>)

    /** Increment play count by 1. */
    suspend fun incrementPlayCount(songId: Long)

    /** Single song lookup, or null if not found. */
    suspend fun getSongById(songId: Long): Song?
}
