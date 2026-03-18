package com.example.tiptracker.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.example.tiptracker.data.SettingsKeys
import com.example.tiptracker.data.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepository(private val context: Context) {
    val darkMode: Flow<Boolean> = context.dataStore.data
        .map { it[SettingsKeys.DARK_MODE] ?: false }

    val tipPreset1Percent = context.dataStore.data
        .map { it[SettingsKeys.TIP_PERCENT_PRESET_1] ?: 10 }

    val tipPreset2Percent = context.dataStore.data
        .map { it[SettingsKeys.TIP_PERCENT_PRESET_2] ?: 15 }

    suspend fun saveDarkMode(enabled: Boolean) {
        context.dataStore.edit { it[SettingsKeys.DARK_MODE] = enabled }
    }

    suspend fun saveTipPreset1(value: Int) {
        context.dataStore.edit { it[SettingsKeys.TIP_PERCENT_PRESET_1] = value }
    }

    suspend fun saveTipPreset2(value: Int) {
        context.dataStore.edit { it[SettingsKeys.TIP_PERCENT_PRESET_2] = value }
    }
}