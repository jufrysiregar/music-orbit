package com.musicorbit.android.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicorbit.domain.model.Song
import com.musicorbit.domain.model.SortOrder
import com.musicorbit.domain.repository.PreferencesRepository
import com.musicorbit.domain.usecase.GetSortedSongsUseCase
import com.musicorbit.domain.usecase.ScanMusicUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val getSortedSongsUseCase: GetSortedSongsUseCase,
    private val scanMusicUseCase: ScanMusicUseCase,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.DATE_ADDED)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    val songs: StateFlow<List<Song>> = combine(
        getSortedSongsUseCase(),
        _searchQuery,
        _sortOrder
    ) { allSongs, query, sort ->
        val filtered = if (query.isBlank()) allSongs
        else allSongs.filter {
            it.title.contains(query, ignoreCase = true) ||
            it.artist.contains(query, ignoreCase = true)
        }
        when (sort) {
            SortOrder.DATE_ADDED  -> filtered.sortedByDescending { it.dateAdded }
            SortOrder.PLAY_COUNT  -> filtered.sortedByDescending { it.playCount }
            SortOrder.TITLE       -> filtered.sortedBy { it.title.lowercase() }
            SortOrder.ARTIST      -> filtered.sortedBy { it.artist.lowercase() }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onSearchQueryChange(query: String) { _searchQuery.value = query }

    fun onSortOrderChange(order: SortOrder) {
        _sortOrder.value = order
        viewModelScope.launch { preferencesRepository.saveSortOrder(order) }
    }

    fun triggerScan() {
        viewModelScope.launch { scanMusicUseCase() }
    }
}
