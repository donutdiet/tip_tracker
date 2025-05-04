package com.example.tiptracker.ui

import android.app.Application
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.tiptracker.TipTracker
import com.example.tiptracker.ui.home.LogFormViewModel

object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            LogFormViewModel(this.tipTracker().container.logsRepository)
        }
    }
}

/**
 * Extension function to queries for [Application] object and returns an instance of
 * [TipTracker].
 */
fun CreationExtras.tipTracker(): TipTracker =
    (this[AndroidViewModelFactory.APPLICATION_KEY] as TipTracker)