package com.example.tiptracker.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tiptracker.data.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsState(
    val darkMode: Boolean = false,
    val tipPercentPreset1: Int = 10,
    val tipPercentPreset2: Int = 15
)

sealed interface SettingsAction {
    data class setDarkMode(val enabled: Boolean) : SettingsAction
    data class setTipPercentPreset1(val value: Int) : SettingsAction
    data class setTipPercentPreset2(val value: Int) : SettingsAction
}

class SettingsViewModel(private val settingsRepository: SettingsRepository): ViewModel() {

    val uiState = combine(
        settingsRepository.darkMode,
        settingsRepository.tipPercentPreset1,
        settingsRepository.tipPercentPreset2
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
                is SettingsAction.setTipPercentPreset1 -> settingsRepository.saveTipPreset1(action.value)
                is SettingsAction.setTipPercentPreset2 -> settingsRepository.saveTipPreset2(action.value)
            }
        }
    }
}