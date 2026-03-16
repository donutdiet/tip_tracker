package com.example.tiptracker.data.repository

import com.example.tiptracker.data.dao.LogDao
import com.example.tiptracker.data.entity.Log
import kotlinx.coroutines.flow.Flow

class LogRepository(
    private val logDao: LogDao
) {
    fun getAllLogs(): Flow<List<Log>> = logDao.getAllTips()

    fun getLogById(id: Int): Log? = logDao.getLogById(id)

    suspend fun insertLog(log: Log) {
        logDao.insertLog(log)
    }

    suspend fun updateLog(log: Log) {
        logDao.updateLog(log)
    }

    suspend fun deleteLog(log: Log) {
        logDao.deleteLog(log)
    }
}