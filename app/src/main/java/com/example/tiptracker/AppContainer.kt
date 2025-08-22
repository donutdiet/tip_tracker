package com.example.tiptracker

import android.content.Context
import com.example.tiptracker.data.database.LogsDatabase
import com.example.tiptracker.data.repository.LogsRepositoryImpl
import com.example.tiptracker.data.repository.LogsRepository

interface AppContainer {
    val logsRepository: LogsRepository
}

class AppDataContainer(private val context: Context) : AppContainer {
    override val logsRepository: LogsRepository by lazy {
        LogsRepositoryImpl(LogsDatabase.getDatabase(context).logDao())
    }
}