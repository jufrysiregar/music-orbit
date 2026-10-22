package com.musicorbit.android.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicorbit.domain.model.Song
import com.musicorbit.domain.model.SortOrder
import com.musicorbit.domain.repository.PreferencesRepository
import com.musicorbit.domain.usecase.GetSortedSongsUseCase
import com.musicorbit.domain.usecase.ScanMusicUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeViewModel(
    private val getSortedSongsUseCase: GetSortedSongsUseCase,
    private val scanMusicUseCase: ScanMusicUseCase,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.DATE_ADDED)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    // Live set of favorite song ids, so the "Favorit" sort can filter to loved tracks and react
    // instantly when a song is favorited/unfavorited from the player.
    private val favorites: StateFlow<Set<Long>> = preferencesRepository.getFavorites()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    // Raw, unsorted scan result cached so a sort change can rebuild the sequential queue
    // synchronously and deterministically (no race against the Flow emission).
    private var rawSongs: List<Song> = emptyList()

    /**
     * Invoked when the header sort order changes, delivering the freshly sorted list so the
     * live playback queue can re-align to it (keeps "Berurut" in sync with "urutkan berdasarkan").
     */
    var onQueueShouldFollowSort: ((List<Song>) -> Unit)? = null

    val songs: StateFlow<List<Song>> = combine(
        getSortedSongsUseCase(),
        _searchQuery,
        _sortOrder,
        favorites
    ) { allSongs, query, sort, favs ->
        rawSongs = allSongs
        sortAndFilter(allSongs, query, sort, favs)
    }
        // Sort/filter a potentially large library OFF the main thread. Doing it on Main blocked the
        // UI thread during a sort change, which is what made the playing audio hitch.
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private fun sortAndFilter(
        all: List<Song>,
        query: String,
        sort: SortOrder,
        favs: Set<Long>
    ): List<Song> {
        val filtered = if (query.isBlank()) all
        else all.filter {
            it.title.contains(query, ignoreCase = true) ||
            it.artist.contains(query, ignoreCase = true)
        }
        return when (sort) {
            SortOrder.DATE_ADDED  -> filtered.sortedByDescending { it.dateAdded }
            SortOrder.PLAY_COUNT  -> filtered.sortedByDescending { it.playCount }
            SortOrder.TITLE       -> filtered.sortedBy { it.title.lowercase() }
            // "Favorit" is both a filter and a sort: keep only loved tracks (matching the search
            // query too), shown A-Z.
            SortOrder.FAVORITES   -> filtered.filter { it.id in favs }.sortedBy { it.title.lowercase() }
            SortOrder.ARTIST      -> filtered.sortedBy { it.artist.lowercase() }
        }
    }

    init {
        // Auto-scan on first launch so the list is populated immediately
        viewModelScope.launch { scanMusicUseCase() }
    }

    fun onSearchQueryChange(query: String) { _searchQuery.value = query }

    fun onSortOrderChange(order: SortOrder) {
        _sortOrder.value = order
        viewModelScope.launch {
            preferencesRepository.saveSortOrder(order)
            // Compute the new order off the main thread, then hop back to Main only to reorder the
            // ExoPlayer window (which must run on the player's own thread).
            val sorted = withContext(Dispatchers.Default) {
                sortAndFilter(rawSongs, _searchQuery.value, order, favorites.value)
            }
            onQueueShouldFollowSort?.invoke(sorted)
        }
    }

    fun triggerScan() {
        viewModelScope.launch { scanMusicUseCase() }
    }
}
