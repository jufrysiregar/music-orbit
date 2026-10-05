package com.musicorbit.domain.usecase

import com.musicorbit.domain.model.Lyrics
import com.musicorbit.domain.repository.LyricsRepository

class GetLyricsUseCase(private val lyricsRepository: LyricsRepository) {
    suspend operator fun invoke(songId: Long): Lyrics? =
        lyricsRepository.getLyricsBySongId(songId)
}
