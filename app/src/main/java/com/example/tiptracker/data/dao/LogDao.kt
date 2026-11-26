package com.example.tiptracker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.tiptracker.data.entity.Log
import com.example.tiptracker.data.model.LogStats
import com.example.tiptracker.data.model.PlacedVisit
import com.example.tiptracker.data.model.RatingCount
import com.example.tiptracker.data.model.RestaurantVisit
import kotlinx.coroutines.flow.Flow

@Dao
interface LogDao {
    @Query("SELECT * FROM log ORDER BY date DESC")
    fun getAllLogs(): Flow<List<Log>>

    @Query("SELECT * FROM log WHERE id = :id")
    fun getLogById(id: Int): Flow<Log?>

    @Insert
    suspend fun insertLog(log: Log): Long

    @Update
    suspend fun updateLog(log: Log)

    @Query("DELETE FROM log WHERE id = :id")
    suspend fun deleteLogById(id: Int): Int

    // History suggestions; matching ignores punctuation in repository code.
    @Query("SELECT id, restaurant_name AS name, place_id AS placeId, date FROM log")
    fun getRestaurantVisits(): Flow<List<RestaurantVisit>>

    // One name per linked place: the names on a place's logs, leaving out the log being
    // edited (excludeId), and renaming them all
    @Query("SELECT restaurant_name FROM log WHERE place_id = :placeId AND id != :excludeId")
    fun observeNamesAtPlace(placeId: String, excludeId: Int): Flow<List<String>>

    @Query("UPDATE log SET restaurant_name = :name WHERE place_id = :placeId")
    suspend fun renamePlace(placeId: String, name: String): Int

    // Map: every log linked to a place, grouped into places in repository (groupMapPlaces)
    @Query("""
        SELECT id, place_id AS placeId, restaurant_name AS name, date, rating, tip_percent AS tipPercent,
            total, party_size AS partySize, bill_type AS billType, latitude, longitude,
            coordinates_fetched_at AS coordinatesFetchedAt
        FROM log WHERE place_id IS NOT NULL
    """)
    fun observePlacedVisits(): Flow<List<PlacedVisit>>

    @Query("SELECT * FROM log WHERE place_id = :placeId ORDER BY date DESC, id DESC")
    fun observeLogsAtPlace(placeId: String): Flow<List<Log>>

    // Gives every log at a place the coordinates, unless it already has newer ones (an edited
    // log's older lookup never replaces a fresh one)
    @Query("""
        UPDATE log SET latitude = :latitude, longitude = :longitude, coordinates_fetched_at = :fetchedAt
        WHERE place_id = :placeId AND (coordinates_fetched_at IS NULL OR coordinates_fetched_at < :fetchedAt)
    """)
    suspend fun setPlaceCoordinates(placeId: String, latitude: Double, longitude: Double, fetchedAt: Long)

    // Google's terms: coordinates may be kept 30 days. Also clears any without a fetch time.
    @Query("""
        UPDATE log SET latitude = NULL, longitude = NULL, coordinates_fetched_at = NULL
        WHERE coordinates_fetched_at < :cutoff OR (coordinates_fetched_at IS NULL AND latitude IS NOT NULL)
    """)
    suspend fun clearCoordinatesFetchedBefore(cutoff: Long)

    // Saves a log and gives every log at its place the log's name, all or nothing
    @Transaction
    suspend fun insertLogNamingPlace(log: Log): Long {
        val id = insertLog(log)
        renamePlace(checkNotNull(log.placeId), log.restaurantName)
        shareCoordinates(log)
        return id
    }

    @Transaction
    suspend fun updateLogNamingPlace(log: Log) {
        updateLog(log)
        renamePlace(checkNotNull(log.placeId), log.restaurantName)
        shareCoordinates(log)
    }

    // A log saved with a fresh lookup of its place's coordinates gives them to the place's logs
    private suspend fun shareCoordinates(log: Log) {
        val placeId = log.placeId ?: return
        val latitude = log.latitude ?: return
        val longitude = log.longitude ?: return
        val fetchedAt = log.coordinatesFetchedAt ?: return
        setPlaceCoordinates(placeId, latitude, longitude, fetchedAt)
    }

    // Stats profile section. Bill, total and tip are whole-party amounts, so a separate check
    // (the user's own check only) is extrapolated to the party by assuming everyone spent the
    // same, the same assumption a split bill makes in reverse. Per person uses the
    // Log.totalPerPerson rule.
    @Query("""
        SELECT
            COUNT(*) as totalLogs,
            ROUND(AVG(CASE WHEN bill_type = 'SEPARATE' THEN bill * party_size ELSE bill END), 2) as avgBill,
            ROUND(AVG(tip_percent), 2) as avgTipPercent,
            ROUND(AVG(CASE WHEN bill_type = 'SEPARATE' THEN total * party_size ELSE total END), 2) as avgTotal,
            ROUND(AVG(party_size), 2) as avgPartySize,
            ROUND(AVG(rating), 2) as avgRating,
            ROUND(AVG(CASE WHEN bill_type = 'SEPARATE' THEN (total - bill) * party_size ELSE total - bill END), 2) as avgTipAmount,
            ROUND(AVG(CASE WHEN bill_type = 'SEPARATE' THEN total ELSE total / party_size END), 2) as avgTotalPerPerson
        FROM log
    """)
    fun getLogStats(): Flow<LogStats>

    // Rating distribution graph
    @Query("SELECT rating, COUNT(*) as count from log GROUP BY rating ORDER BY rating")
    fun getRatingDistribution(): Flow<List<RatingCount>>

    // Awards. Ranks by the same per-person total the award displays (Log.totalPerPerson).
    @Query("""
        SELECT * FROM log
        ORDER BY CASE WHEN bill_type = 'SEPARATE' THEN total ELSE total / party_size END DESC
        LIMIT 1
    """)
    fun getHighestSpendPerPersonLog(): Flow<Log?>

    @Query("SELECT * FROM log ORDER BY tip_percent DESC LIMIT 1")
    fun getMostGenerousTipLog(): Flow<Log?>

    @Query("SELECT * FROM log ORDER BY party_size DESC LIMIT 1")
    fun getLargestPartySizeLog(): Flow<Log?>

    @Query("SELECT * FROM log ORDER BY rating DESC LIMIT 1")
    fun getTopRatedLog(): Flow<Log?>

    @Query("SELECT * FROM log ORDER BY LENGTH(review) DESC LIMIT 1")
    fun getLongestReview(): Flow<Log?>
}
