package com.example.tiptracker

import android.app.Application
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.tiptracker.ui.logs.LogFormViewModel
import com.example.tiptracker.ui.logs.LogsViewModel

object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer<LogFormViewModel> {
            LogFormViewModel(this.tipTracker().container.logsRepository)
        }

        initializer<LogsViewModel> {
            LogsViewModel(
                this.tipTracker().container.logsRepository
            )
        }
    }
}

/**
 * Extension function to queries for [Application] object and returns an instance of
 * [TipTracker].
 */
fun CreationExtras.tipTracker(): TipTracker =
    (this[AndroidViewModelFactory.APPLICATION_KEY] as TipTracker)