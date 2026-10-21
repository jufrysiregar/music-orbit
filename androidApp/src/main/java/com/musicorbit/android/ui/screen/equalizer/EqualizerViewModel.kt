package com.musicorbit.android.ui.screen.equalizer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicorbit.android.ui.util.EqualizerCodec
import com.musicorbit.domain.repository.PreferencesRepository
import com.musicorbit.platform.EqualizerEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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

    /**
     * Bind the (idempotent) engine to the live session, restore the persisted enabled/preset/band
     * state, and reflect the TRUE engine state so the "Aktifkan Equalizer" switch renders correctly
     * on launch — no manual ON/OFF toggling required.
     */
    suspend fun initialize(audioSessionId: Int) {
        equalizerEngine.initialize(audioSessionId)
        val enabled = preferencesRepository.getEqualizerEnabled().first()
        val preset  = preferencesRepository.getEqualizerPreset().first()
        val bands   = EqualizerCodec.fromJson(preferencesRepository.getEqualizerBands().first())
        if (bands.isNotEmpty()) bands.forEachIndexed { i, level -> equalizerEngine.setBandLevel(i, level) }
        else equalizerEngine.applyPreset(preset)
        equalizerEngine.setEnabled(enabled)
        val range = equalizerEngine.getLevelRange()
        _state.update {
            it.copy(
                isEnabled       = equalizerEngine.isEnabled(),
                selectedPreset  = preset,
                bandLevels      = equalizerEngine.getBandLevels(),
                bandFrequencies = equalizerEngine.getBandFrequencies(),
                minLevel        = range.first,
                maxLevel        = range.second
            )
        }
    }

    fun onPresetSelected(preset: String) {
        equalizerEngine.applyPreset(preset)
        val levels = equalizerEngine.getBandLevels()
        _state.update {
            it.copy(
                selectedPreset = preset,
                bandLevels     = levels
            )
        }
        viewModelScope.launch {
            preferencesRepository.saveEqualizerPreset(preset)
            preferencesRepository.saveEqualizerBands(EqualizerCodec.toJson(levels))
        }
    }

    fun onBandLevelChange(bandIndex: Int, level: Int) {
        equalizerEngine.setBandLevel(bandIndex, level)
        val updated = _state.value.bandLevels.toMutableList()
        if (bandIndex < updated.size) updated[bandIndex] = level
        _state.update { it.copy(bandLevels = updated.toList()) }
        viewModelScope.launch { preferencesRepository.saveEqualizerBands(EqualizerCodec.toJson(updated)) }
    }

    fun onEnabledChanged(enabled: Boolean) {
        equalizerEngine.setEnabled(enabled)
        _state.update { it.copy(isEnabled = equalizerEngine.isEnabled()) }
        viewModelScope.launch { preferencesRepository.saveEqualizerEnabled(enabled) }
    }
}
