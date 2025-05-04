package com.example.tiptracker.data

import android.content.Context

interface AppContainer {
    val logsRepository: LogsRepository
}

class AppDataContainer(private val context: Context) : AppContainer {
    override val logsRepository: LogsRepository by lazy {
        OfflineRepository(LogsDatabase.getDatabase(context).logDao())
    }
}