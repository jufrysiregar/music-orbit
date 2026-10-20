package com.musicorbit.android.ui.screen.equalizer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicorbit.domain.repository.PreferencesRepository
import com.musicorbit.platform.EqualizerEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EqualizerViewModel(
    private val equalizerEngine: EqualizerEngine,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    data class EqualizerState(
        val isEnabled: Boolean = false,
        val selectedPreset: String = "Normal",
        val bandLevels: List<Int> = emptyList(),
        val bandFrequencies: List<Int> = emptyList(),
        val minLevel: Int = -1500,
        val maxLevel: Int = 1500
    )

    val presets = listOf("Normal", "Pop", "Rock", "Jazz", "Classical", "Dance", "Hip-Hop", "Vocal", "Acoustic", "Electronic")

    private val _state = MutableStateFlow(EqualizerState())
    val state: StateFlow<EqualizerState> = _state.asStateFlow()

    fun initialize(audioSessionId: Int) {
        equalizerEngine.initialize(audioSessionId)
        val range = equalizerEngine.getLevelRange()
        _state.update {
            it.copy(
                bandLevels      = equalizerEngine.getBandLevels(),
                bandFrequencies = equalizerEngine.getBandFrequencies(),
                minLevel        = range.first,
                maxLevel        = range.second
            )
        }
    }

    fun onPresetSelected(preset: String) {
        equalizerEngine.applyPreset(preset)
        _state.update {
            it.copy(
                selectedPreset = preset,
                bandLevels     = equalizerEngine.getBandLevels()
            )
        }
        viewModelScope.launch { preferencesRepository.saveEqualizerPreset(preset) }
    }

    fun onBandLevelChange(bandIndex: Int, level: Int) {
        equalizerEngine.setBandLevel(bandIndex, level)
        val updated = _state.value.bandLevels.toMutableList()
        if (bandIndex < updated.size) updated[bandIndex] = level
        _state.update { it.copy(bandLevels = updated.toList()) }
    }

    fun onEnabledChanged(enabled: Boolean) {
        equalizerEngine.setEnabled(enabled)
        _state.update { it.copy(isEnabled = enabled) }
        viewModelScope.launch { preferencesRepository.saveEqualizerEnabled(enabled) }
    }
}
