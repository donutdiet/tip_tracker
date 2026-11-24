package com.example.tiptracker.data.repository

import com.example.tiptracker.data.dao.LogDao
import com.example.tiptracker.data.helper.ImageStorageHelper
import com.example.tiptracker.data.entity.Log
import com.example.tiptracker.data.model.LogStats
import com.example.tiptracker.data.model.RatingCount
import com.example.tiptracker.data.model.RestaurantHistory
import com.example.tiptracker.data.model.groupRestaurantHistory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

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

    /** One row per restaurant the user has logged, for history suggestions. */
    fun getRestaurantHistory(): Flow<List<RestaurantHistory>> =
        logDao.getRestaurantVisits()
            .map(::groupRestaurantHistory)
            .flowOn(Dispatchers.Default)

    fun getLogStats(): Flow<LogStats> = logDao.getLogStats()

    fun getRatingDistribution(): Flow<List<RatingCount>> = logDao.getRatingDistribution()

    fun getHighestSpendPerPersonLog(): Flow<Log?> = logDao.getHighestSpendPerPersonLog()

    fun getMostGenerousTipLog(): Flow<Log?> = logDao.getMostGenerousTipLog()

    fun getLargestPartySizeLog(): Flow<Log?> = logDao.getLargestPartySizeLog()

    fun getTopRatedLog(): Flow<Log?> = logDao.getTopRatedLog()

    fun getLongestReviewLog(): Flow<Log?> = logDao.getLongestReview()
}