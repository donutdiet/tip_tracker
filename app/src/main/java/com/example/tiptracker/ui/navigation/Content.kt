package com.example.tiptracker.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.example.tiptracker.ui.features.settings.SettingsRoot
import com.example.tiptracker.ui.tabs.home.HomeRoot

fun EntryProviderScope<NavKey>.tabEntries(
    snackbarHostState: SnackbarHostState
) {
    entry<TabKey.Logs> {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(text = "Logs")
        }
    }

    entry<TabKey.Home> {
        HomeRoot(
            snackbarHostState = snackbarHostState
        )
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