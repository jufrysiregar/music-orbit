package com.musicorbit.domain.usecase

import com.musicorbit.domain.model.Lyrics
import com.musicorbit.domain.repository.LyricsRepository

class SaveLyricsUseCase(private val lyricsRepository: LyricsRepository) {
    suspend operator fun invoke(lyrics: Lyrics) =
        lyricsRepository.saveLyrics(lyrics)
}
