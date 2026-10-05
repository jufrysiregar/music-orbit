package com.musicorbit.android.ui.screen.lyrics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicorbit.domain.model.Lyrics
import com.musicorbit.domain.model.Song
import com.musicorbit.domain.repository.SongRepository
import com.musicorbit.domain.usecase.GetLyricsUseCase
import com.musicorbit.domain.usecase.SaveLyricsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.URLEncoder

class LyricsViewModel(
    private val getLyricsUseCase: GetLyricsUseCase,
    private val saveLyricsUseCase: SaveLyricsUseCase,
    private val songRepository: SongRepository
) : ViewModel() {

    private val _lyrics    = MutableStateFlow<Lyrics?>(null)
    val lyrics: StateFlow<Lyrics?> = _lyrics.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _song = MutableStateFlow<Song?>(null)
    val song: StateFlow<Song?> = _song.asStateFlow()

    fun loadLyrics(songId: Long) {
        viewModelScope.launch {
            _song.value    = songRepository.getSongById(songId)
            _lyrics.value  = getLyricsUseCase(songId)
            _inputText.value = _lyrics.value?.lyricsText ?: ""
        }
    }

    fun onInputTextChange(text: String) { _inputText.value = text }

    fun saveLyrics(songId: Long) {
        val text = _inputText.value.trim()
        if (text.isEmpty()) return
        viewModelScope.launch {
            saveLyricsUseCase(Lyrics(songId = songId, lyricsText = text))
            _lyrics.value = Lyrics(songId = songId, lyricsText = text)
        }
    }

    /** Returns Google Search URL: "Lirik [Title] [Artist]" */
    fun buildSearchUrl(): String {
        val s = _song.value ?: return "https://www.google.com"
        val q = URLEncoder.encode("Lirik ${s.title} ${s.artist}", "UTF-8")
        return "https://www.google.com/search?q=$q"
    }
}
