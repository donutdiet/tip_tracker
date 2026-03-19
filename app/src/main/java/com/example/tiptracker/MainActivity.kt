package com.example.tiptracker

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.example.tiptracker.data.repository.SettingsRepository
import com.example.tiptracker.ui.RootActivity
import com.example.tiptracker.utils.setEdgeToEdgeConfig
import com.example.tiptracker.ui.theme.TipTrackerTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val settingsRepository: SettingsRepository by inject()
    private var isReady by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        splashScreen.setKeepOnScreenCondition { !isReady }

        lifecycleScope.launch {
            settingsRepository.darkMode.first()
            isReady = true
        }

        setEdgeToEdgeConfig()

        setContent {
            val darkMode by settingsRepository.darkMode.collectAsState(initial = false)

            TipTrackerTheme(darkTheme = darkMode) {
                RootActivity()
            }
        }
    }
}
