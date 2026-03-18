package com.example.tiptracker.ui.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tiptracker.data.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsState(
    val darkMode: Boolean = false,
    val tipPreset1Percent: Int = 10,
    val tipPreset2Percent: Int = 15
)

sealed interface SettingsAction {
    data class setDarkMode(val enabled: Boolean) : SettingsAction
    data class setTipPreset1Percent(val value: Int) : SettingsAction
    data class setTipPreset2Percent(val value: Int) : SettingsAction
}

class SettingsViewModel(private val settingsRepository: SettingsRepository): ViewModel() {

    val uiState = combine(
        settingsRepository.darkMode,
        settingsRepository.tipPreset1Percent,
        settingsRepository.tipPreset2Percent
    ) { darkMode, preset1, preset2 ->
        SettingsState(darkMode, preset1, preset2)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsState()
    )

    fun onAction(action: SettingsAction) {
        viewModelScope.launch {
            when(action) {
                is SettingsAction.setDarkMode -> settingsRepository.saveDarkMode(action.enabled)
                is SettingsAction.setTipPreset1Percent -> settingsRepository.saveTipPreset1(action.value)
                is SettingsAction.setTipPreset2Percent -> settingsRepository.saveTipPreset2(action.value)
            }
        }
    }
}