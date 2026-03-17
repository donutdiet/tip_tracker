package com.example.tiptracker

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.tiptracker.data.repository.SettingsRepository
import com.example.tiptracker.ui.RootActivity
import com.example.tiptracker.utils.setEdgeToEdgeConfig
import com.example.tiptracker.ui.theme.TipTrackerTheme
import org.koin.compose.koinInject

class MainActivity : ComponentActivity() {
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        setEdgeToEdgeConfig()
        super.onCreate(savedInstanceState)
        setContent {
            val settingsRepository = koinInject<SettingsRepository>()
            val darkMode by settingsRepository.darkMode
                .collectAsState(initial = false)

            TipTrackerTheme(darkTheme = darkMode) {
                RootActivity()
            }
        }
    }
}