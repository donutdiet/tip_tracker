package com.example.tiptracker

import android.app.Application
import com.example.tiptracker.data.DatabaseProvider
import com.example.tiptracker.data.TipTrackerDatabase
import com.example.tiptracker.data.dataStore
import com.example.tiptracker.data.repository.LogRepository
import com.example.tiptracker.data.repository.SettingsRepository
import com.example.tiptracker.ui.features.settings.SettingsViewModel
import com.example.tiptracker.ui.features.logdetail.LogDetailViewModel
import com.example.tiptracker.ui.tabs.home.HomeViewModel
import com.example.tiptracker.ui.tabs.logs.LogsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Database and DAO
    single { DatabaseProvider.getDatabase(androidContext()) }
    single { get<TipTrackerDatabase>().logDao() }

    // Datastore
    single { androidContext().dataStore }

    // Repository
    single { LogRepository(get()) }
    single { SettingsRepository(get()) }

    // ViewModels
    viewModel { HomeViewModel(get(), get()) }
    viewModel { SettingsViewModel(get()) }
    viewModel { LogsViewModel(get()) }
    viewModel { (logId: Int) -> LogDetailViewModel(logId = logId, logsRepository = get()) }
}

class TipTracker : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@TipTracker)
            modules(appModule)
        }
    }
}