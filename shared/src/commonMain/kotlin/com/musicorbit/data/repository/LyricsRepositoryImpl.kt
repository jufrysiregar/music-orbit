package com.musicorbit.data.repository

import com.musicorbit.db.MusicOrbitDatabase
import com.musicorbit.domain.model.Lyrics
import com.musicorbit.domain.repository.LyricsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LyricsRepositoryImpl(
    private val database: MusicOrbitDatabase
) : LyricsRepository {

    private val queries = database.musicOrbitDatabaseQueries

    override suspend fun getLyricsBySongId(songId: Long): Lyrics? = withContext(Dispatchers.Default) {
        queries.getLyricsBySongId(songId).executeAsOneOrNull()?.let {
            Lyrics(
                songId     = it.songId,
                lyricsText = it.lyricsText,
                hasLrc     = it.hasLrc != 0L
            )
        }
    }

    override suspend fun saveLyrics(lyrics: Lyrics): Unit = withContext(Dispatchers.Default) {
        queries.upsertLyrics(
            songId     = lyrics.songId,
            lyricsText = lyrics.lyricsText,
            hasLrc     = if (lyrics.hasLrc) 1L else 0L
        )
    }

    override suspend fun deleteLyrics(songId: Long): Unit = withContext(Dispatchers.Default) {
        queries.deleteLyrics(songId)
    }
}
