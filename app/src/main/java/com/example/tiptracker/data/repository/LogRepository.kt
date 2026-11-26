package com.example.tiptracker.data.repository

import com.example.tiptracker.data.dao.LogDao
import com.example.tiptracker.data.helper.ImageStorageHelper
import com.example.tiptracker.data.entity.Log
import com.example.tiptracker.data.helper.Coordinates
import com.example.tiptracker.data.model.COORDINATES_MAX_AGE_MS
import com.example.tiptracker.data.model.LogStats
import com.example.tiptracker.data.model.MapPlace
import com.example.tiptracker.data.model.groupMapPlaces
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

    /**
     * Saves [log]. A linked place has one name across all its logs, so a log linked to a place
     * renames the place's other logs too. The forms require a name on linked logs.
     */
    suspend fun insertLog(log: Log): Int {
        val savedLogId = if (log.placeId != null) logDao.insertLogNamingPlace(log) else logDao.insertLog(log)
        return savedLogId.toInt()
    }

    suspend fun updateLog(log: Log) {
        if (log.placeId != null) logDao.updateLogNamingPlace(log) else logDao.updateLog(log)
    }

    /**
     * The names on the user's logs at [placeId], except [editingLogId] (the log being edited),
     * so a form can say which of them saving will rename.
     */
    fun observeNamesAtPlace(placeId: String, editingLogId: Int? = null): Flow<List<String>> =
        logDao.observeNamesAtPlace(placeId, excludeId = editingLogId ?: -1)

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

    /** The user's places with their visits, for the map (see groupMapPlaces). */
    fun observeMapPlaces(now: () -> Long = System::currentTimeMillis): Flow<List<MapPlace>> =
        logDao.observePlacedVisits()
            .map { groupMapPlaces(it, now()) }
            .flowOn(Dispatchers.Default)

    fun observeLogsAtPlace(placeId: String): Flow<List<Log>> = logDao.observeLogsAtPlace(placeId)

    /** Stores coordinates just looked up for [placeId] on all its logs. */
    suspend fun setPlaceCoordinates(placeId: String, coordinates: Coordinates, fetchedAt: Long) {
        logDao.setPlaceCoordinates(placeId, coordinates.latitude, coordinates.longitude, fetchedAt)
    }

    /** Clears coordinates older than Google's terms allow keeping them. */
    suspend fun clearExpiredCoordinates(now: Long = System.currentTimeMillis()) {
        logDao.clearCoordinatesFetchedBefore(now - COORDINATES_MAX_AGE_MS)
    }

    fun getLogStats(): Flow<LogStats> = logDao.getLogStats()

    fun getRatingDistribution(): Flow<List<RatingCount>> = logDao.getRatingDistribution()

    fun getHighestSpendPerPersonLog(): Flow<Log?> = logDao.getHighestSpendPerPersonLog()

    fun getMostGenerousTipLog(): Flow<Log?> = logDao.getMostGenerousTipLog()

    fun getLargestPartySizeLog(): Flow<Log?> = logDao.getLargestPartySizeLog()

    fun getTopRatedLog(): Flow<Log?> = logDao.getTopRatedLog()

    fun getLongestReviewLog(): Flow<Log?> = logDao.getLongestReview()
}