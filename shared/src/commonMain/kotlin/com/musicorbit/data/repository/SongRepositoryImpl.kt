package com.musicorbit.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.musicorbit.db.MusicOrbitDatabase
import com.musicorbit.db.Song as DbSong
import com.musicorbit.domain.model.Song
import com.musicorbit.domain.model.SongData
import com.musicorbit.domain.repository.SongRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class SongRepositoryImpl(
    private val database: MusicOrbitDatabase
) : SongRepository {

    private val queries = database.musicOrbitDatabaseQueries

    override fun getAllSongs(): Flow<List<Song>> =
        queries.getAllSongs()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toDomain() } }

    override fun getSongsWithLyrics(): Flow<List<Song>> =
        queries.getSongsWithLyrics()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows ->
                rows.map { row ->
                    Song(
                        id          = row.id,
                        title       = row.title,
                        artist      = row.artist,
                        album       = row.album,
                        duration    = row.duration,
                        filePath    = row.filePath,
                        albumArtUri = row.albumArtUri,
                        playCount   = row.playCount.toInt(),
                        dateAdded   = row.dateAdded,
                        hasLyrics   = row.lyricsText != null
                    )
                }
            }

    override fun searchSongs(query: String): Flow<List<Song>> =
        queries.searchSongs(query)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toDomain() } }

    override suspend fun upsertSong(song: Song): Unit = withContext(Dispatchers.Default) {
        queries.upsertSong(
            id          = song.id,
            title       = song.title,
            artist      = song.artist,
            album       = song.album,
            duration    = song.duration,
            filePath    = song.filePath,
            albumArtUri = song.albumArtUri,
            playCount   = song.playCount.toLong(),
            dateAdded   = song.dateAdded
        )
    }

    override suspend fun upsertSongs(songs: List<SongData>): Unit = withContext(Dispatchers.Default) {
        queries.transaction {
            songs.forEach { s ->
                queries.upsertSong(
                    id          = s.id,
                    title       = s.title,
                    artist      = s.artist,
                    album       = s.album,
                    duration    = s.duration,
                    filePath    = s.filePath,
                    albumArtUri = s.albumArtUri,
                    playCount   = 0L,
                    dateAdded   = s.dateAdded
                )
            }
        }
    }

    override suspend fun incrementPlayCount(songId: Long): Unit = withContext(Dispatchers.Default) {
        queries.incrementPlayCount(songId)
    }

    override suspend fun getSongById(songId: Long): Song? = withContext(Dispatchers.Default) {
        queries.getSongById(songId).executeAsOneOrNull()?.toDomain()
    }

    // ── mapping ───────────────────────────────────────────────────────────────

    private fun DbSong.toDomain() = Song(
        id          = id,
        title       = title,
        artist      = artist,
        album       = album,
        duration    = duration,
        filePath    = filePath,
        albumArtUri = albumArtUri,
        playCount   = playCount.toInt(),
        dateAdded   = dateAdded,
        hasLyrics   = false   // resolved in getSongsWithLyrics
    )
}
