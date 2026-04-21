package com.example.tiptracker.data.repository

import com.example.tiptracker.data.dao.LogDao
import com.example.tiptracker.data.helper.ImageStorageHelper
import com.example.tiptracker.data.entity.Log
import com.example.tiptracker.data.model.LogStats
import com.example.tiptracker.data.model.RatingCount
import kotlinx.coroutines.flow.Flow

class LogRepository(
    private val logDao: LogDao,
    private val imageStorageHelper: ImageStorageHelper
) {
    fun getAllLogs(): Flow<List<Log>> = logDao.getAllLogs()

    fun getLogById(id: Int): Flow<Log?> = logDao.getLogById(id)

    suspend fun insertLog(log: Log): Int {
        val savedLogId = logDao.insertLog(log)
        return savedLogId.toInt()
    }

    suspend fun updateLog(log: Log) {
        logDao.updateLog(log)
    }

    suspend fun deleteLogById(id: Int): Int {
        val deletedCount = logDao.deleteLogById(id)
        if (deletedCount > 0) {
            check(imageStorageHelper.deleteImagesForLog(id)) {
                "Couldn't delete stored images for log $id."
            }
        }
        return deletedCount
    }

    fun getLogStats(): Flow<LogStats> = logDao.getLogStats()

    fun getRatingDistribution(): Flow<List<RatingCount>> = logDao.getRatingDistribution()

    fun getHighestSpendPerPersonLog(): Flow<Log?> = logDao.getHighestSpendPerPersonLog()

    fun getMostGenerousTipLog(): Flow<Log?> = logDao.getMostGenerousTipLog()

    fun getLargestPartySizeLog(): Flow<Log?> = logDao.getLargestPartySizeLog()

    fun getTopRatedLog(): Flow<Log?> = logDao.getTopRatedLog()

    fun getLongestReviewLog(): Flow<Log?> = logDao.getLongestReview()
}