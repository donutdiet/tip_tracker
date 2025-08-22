package com.example.tiptracker.data.repository

import com.example.tiptracker.data.dao.LogDao
import com.example.tiptracker.data.entity.Log
import kotlinx.coroutines.flow.Flow

class LogsRepositoryImpl(private val logDao: LogDao) : LogsRepository {
    override suspend fun insertLog(log: Log) = logDao.insert(log)

    override suspend fun updateLog(log: Log) = logDao.update(log)

    override suspend fun deleteLog(log: Log) = logDao.delete(log)

    override fun getAllLogsStream(): Flow<List<Log>> = logDao.getLogsByDate()

    override fun getLogStream(id: Int): Flow<Log?> = logDao.getLog(id)

}