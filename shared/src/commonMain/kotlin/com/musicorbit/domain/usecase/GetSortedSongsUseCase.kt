package com.musicorbit.domain.usecase

import com.musicorbit.domain.model.Song
import com.musicorbit.domain.repository.SongRepository
import kotlinx.coroutines.flow.Flow

/**
 * Delegates to getSongsWithLyrics so the hasLyrics flag is always resolved.
 * Sorting is applied in HomeViewModel using combine() to support reactive
 * sort-order changes without re-querying the database.
 */
class GetSortedSongsUseCase(private val songRepository: SongRepository) {
    operator fun invoke(): Flow<List<Song>> = songRepository.getSongsWithLyrics()
}
