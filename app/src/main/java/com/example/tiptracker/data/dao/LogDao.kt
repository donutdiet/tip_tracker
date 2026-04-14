package com.example.tiptracker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.tiptracker.data.entity.Log
import com.example.tiptracker.data.model.LogStats
import com.example.tiptracker.data.model.RatingCount
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

    // Stats profile section
    @Query("""
        SELECT
            COUNT(*) as totalLogs,
            ROUND(AVG(bill), 2) as avgBill,
            ROUND(AVG(tip_percent), 2) as avgTipPercent,
            ROUND(AVG(total), 2) as avgTotal,
            ROUND(AVG(party_size), 2) as avgPartySize,
            ROUND(AVG(rating), 2) as avgRating,
            ROUND(AVG(total - bill), 2) as avgTipAmount,
            ROUND(AVG(total / party_size), 2) as avgTotalPerPerson
        FROM log
    """)
    fun getLogStats(): Flow<LogStats>

    // Rating distribution graph
    @Query("SELECT rating, COUNT(*) as count from log GROUP BY rating ORDER BY rating")
    fun getRatingDistribution(): Flow<List<RatingCount>>

    // Awards
    @Query("SELECT * FROM log ORDER BY (bill / party_size) DESC LIMIT 1")
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
