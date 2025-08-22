package com.example.tiptracker.data.repository

import com.example.tiptracker.data.entity.Log
import kotlinx.coroutines.flow.Flow

interface LogsRepository {

    suspend fun insertLog(log: Log)

    suspend fun updateLog(log: Log)

    suspend fun deleteLog(log: Log)

    fun getAllLogsStream(): Flow<List<Log>>

    fun getLogStream(id: Int): Flow<Log?>
}