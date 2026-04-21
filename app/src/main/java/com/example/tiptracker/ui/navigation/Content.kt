package com.example.tiptracker.ui.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.example.tiptracker.ui.features.editlog.EditLogRoot
import com.example.tiptracker.ui.features.logdetail.LogDetailRoot
import com.example.tiptracker.ui.features.logimagemanager.LogImageManagerRoot
import com.example.tiptracker.ui.features.logsaved.LogSavedRoot
import com.example.tiptracker.ui.features.settings.SettingsRoot
import com.example.tiptracker.ui.tabs.home.HomeRoot
import com.example.tiptracker.ui.tabs.logs.LogsRoot
import com.example.tiptracker.ui.tabs.profile.ProfileRoot

fun EntryProviderScope<NavKey>.tabEntries(
    openLogDetail: (Int) -> Unit,
    openLogSaved: (Int) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    entry<TabKey.Logs> {
        LogsRoot(
            onLogClick = openLogDetail
        )
    }

    entry<TabKey.Home> {
        HomeRoot(
            snackbarHostState = snackbarHostState,
            onLogSaved = openLogSaved
        )
    }

    entry<TabKey.Profile> {
        ProfileRoot(
            onLogClick = openLogDetail
        )
    }
}

fun EntryProviderScope<NavKey>.rootEntries(
    navigateBack: () -> Unit,
    onLogDeleted: () -> Unit,
    onLogUpdated: () -> Unit,
    openEditPage: (Int) -> Unit,
    openImageManager: (Int) -> Unit,
    pop: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    entry<RootKey.Settings> {
        SettingsRoot(
            onBack = navigateBack
        )
    }

    entry<RootKey.LogSaved> { key ->
        LogSavedRoot(
            logId = key.id,
            snackbarHostState = snackbarHostState,
            onDone = pop
        )
    }

    entry<RootKey.LogDetail> { key ->
        LogDetailRoot(
            logId = key.id,
            onBack = navigateBack,
            onEdit = openEditPage,
            onManageImages = openImageManager,
            snackbarHostState = snackbarHostState,
            onLogDeleted = onLogDeleted
        )
    }

    entry<RootKey.EditLog> { key ->
        EditLogRoot(
            logId = key.id,
            onBack = navigateBack,
            snackbarHostState = snackbarHostState,
            onLogUpdated = onLogUpdated
        )
    }

    entry<RootKey.LogImageManager> { key ->
        LogImageManagerRoot(
            logId = key.id,
            navigateBack = navigateBack,
            snackbarHostState = snackbarHostState
        )
    }
}