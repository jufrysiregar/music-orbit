package com.musicorbit.domain.usecase

import com.musicorbit.domain.repository.SongRepository
import com.musicorbit.platform.MusicScanner

/**
 * Triggers a MediaStore scan and bulk-upserts results into Room/SQLDelight.
 */
class ScanMusicUseCase(
    private val songRepository: SongRepository,
    private val musicScanner: MusicScanner
) {
    suspend operator fun invoke() {
        val songs = musicScanner.scanAudioFiles()
        if (songs.isNotEmpty()) {
            songRepository.upsertSongs(songs)
        }
    }
}
