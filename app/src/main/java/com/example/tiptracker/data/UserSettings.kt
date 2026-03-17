package com.example.tiptracker.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

object SettingsKeys {
    val DARK_MODE = booleanPreferencesKey("dark_mode")
    val TIP_PERCENT_PRESET_1 = intPreferencesKey("tip_percent_preset_1")
    val TIP_PERCENT_PRESET_2 = intPreferencesKey("tip_percent_preset_2")
}