package com.musicorbit.domain.usecase

import com.musicorbit.domain.repository.LyricsRepository
import com.musicorbit.domain.repository.SongRepository
import com.musicorbit.platform.MusicScanner

/**
 * Triggers a MediaStore scan, bulk-upserts results into SQLDelight,
 * then scans for sibling .lrc / .txt lyrics files and saves any found.
 */
class ScanMusicUseCase(
    private val songRepository: SongRepository,
    private val musicScanner: MusicScanner,
    private val lyricsRepository: LyricsRepository
) {
    suspend operator fun invoke() {
        val songs = musicScanner.scanAudioFiles()
        if (songs.isNotEmpty()) {
            songRepository.upsertSongs(songs)

            // Auto-import lyrics from .lrc / .txt files on disk
            val lyricsFound = musicScanner.scanLyricsForSongs(songs)
            lyricsFound.forEach { lyricsRepository.saveLyrics(it) }
        }
    }
}
