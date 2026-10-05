package com.musicorbit.android.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicorbit.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    val theme = preferencesRepository.getTheme()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "SYSTEM")

    val sleepTimerMinutes = preferencesRepository.getSleepTimerMinutes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun saveTheme(theme: String) {
        viewModelScope.launch { preferencesRepository.saveTheme(theme) }
    }

    fun saveSleepTimer(minutes: Int) {
        viewModelScope.launch { preferencesRepository.saveSleepTimerMinutes(minutes) }
    }
}
