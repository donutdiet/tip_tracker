package com.example.tiptracker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.tiptracker.data.entity.Log
import com.example.tiptracker.data.model.LogStats
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

    // Saves a log and gives every log at its place the log's name, all or nothing
    @Transaction
    suspend fun insertLogNamingPlace(log: Log): Long {
        val id = insertLog(log)
        renamePlace(checkNotNull(log.placeId), log.restaurantName)
        return id
    }

    @Transaction
    suspend fun updateLogNamingPlace(log: Log) {
        updateLog(log)
        renamePlace(checkNotNull(log.placeId), log.restaurantName)
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
