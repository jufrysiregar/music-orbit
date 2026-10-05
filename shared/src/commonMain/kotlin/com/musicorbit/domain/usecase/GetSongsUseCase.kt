package com.musicorbit.domain.usecase

import com.musicorbit.domain.model.Song
import com.musicorbit.domain.repository.SongRepository
import kotlinx.coroutines.flow.Flow

/**
 * Returns all songs with hasLyrics resolved.
 * This is the primary source for HomeViewModel.
 */
class GetSongsUseCase(private val songRepository: SongRepository) {
    operator fun invoke(): Flow<List<Song>> = songRepository.getSongsWithLyrics()
}
