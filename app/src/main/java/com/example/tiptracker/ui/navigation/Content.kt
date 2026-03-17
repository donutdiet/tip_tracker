package com.example.tiptracker.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.example.tiptracker.ui.screens.settings.SettingsRoot
import com.example.tiptracker.ui.screens.settings.SettingsScreen

fun EntryProviderScope<NavKey>.tabEntries() {
    entry<TabKey.Logs> {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(text = "Logs")
        }
    }

    entry<TabKey.Home> {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(text = "New")
        }
    }

    entry<TabKey.Profile> {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(text = "Profile")
        }
    }
}

fun EntryProviderScope<NavKey>.rootEntries(
    navigateBack: () -> Unit
) {
    entry<RootKey.Settings> {
        SettingsRoot(
            onBack = navigateBack
        )
    }
}