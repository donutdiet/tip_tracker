package com.example.tiptracker

import android.app.Application
import android.net.ConnectivityManager
import com.example.tiptracker.data.DatabaseProvider
import com.example.tiptracker.data.TipTrackerDatabase
import com.example.tiptracker.data.dataStore
import com.example.tiptracker.data.helper.ImageStorageHelper
import com.example.tiptracker.data.helper.LocationProvider
import com.example.tiptracker.data.repository.ConnectivityRepository
import com.example.tiptracker.data.repository.LogImageRepository
import com.example.tiptracker.data.repository.LogRepository
import com.example.tiptracker.data.repository.SettingsRepository
import com.example.tiptracker.ui.features.editlog.EditLogViewModel
import com.example.tiptracker.ui.features.settings.SettingsViewModel
import com.example.tiptracker.ui.features.logdetail.LogDetailViewModel
import com.example.tiptracker.ui.features.logimagemanager.LogImageManagerViewModel
import com.example.tiptracker.ui.tabs.home.HomeViewModel
import com.example.tiptracker.ui.tabs.logs.LogsViewModel
import com.example.tiptracker.ui.tabs.profile.ProfileViewModel
import com.google.android.libraries.places.api.Places
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Database and DAO
    single { DatabaseProvider.getDatabase(androidContext()) }
    single { get<TipTrackerDatabase>().logDao() }
    single { get<TipTrackerDatabase>().logImageDao() }

    // Datastore
    single { androidContext().dataStore }

    // Helpers
    single { ImageStorageHelper(androidContext()) }
    single { LocationProvider(androidContext()) }

    // Repository
    single { LogRepository(get(), get()) }
    single { SettingsRepository(get()) }
    single { LogImageRepository(get(), get()) }
    single {
        ConnectivityRepository(
            androidContext().getSystemService(ConnectivityManager::class.java)
        )
    }

    // ViewModels
    viewModel { HomeViewModel(get(), get()) }
    viewModel { SettingsViewModel(get()) }
    viewModel { LogsViewModel(get()) }
    viewModel { (logId: Int) -> LogDetailViewModel(logId, get(), get()) }
    viewModel { (logId: Int) -> EditLogViewModel(logId, get()) }
    viewModel { ProfileViewModel(get()) }
    viewModel { (logId: Int) -> LogImageManagerViewModel(logId, get()) }
}

class TipTracker : Application() {
    override fun onCreate() {
        super.onCreate()

        if (!Places.isInitialized() && BuildConfig.PLACES_API_KEY.isNotEmpty()) {
            Places.initializeWithNewPlacesApiEnabled(this, BuildConfig.PLACES_API_KEY)
        }

        startKoin {
            androidContext(this@TipTracker)
            modules(appModule)
        }
    }
}