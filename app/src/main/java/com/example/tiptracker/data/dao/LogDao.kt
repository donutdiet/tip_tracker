package com.example.tiptracker.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.tiptracker.data.entity.Log
import kotlinx.coroutines.flow.Flow

@Dao
interface LogDao {

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insert(log: Log)

    @Update
    suspend fun update(log: Log)

    @Delete
    suspend fun delete(log: Log)

    @Query("SELECT * FROM logs ORDER BY date DESC")
    fun getLogsByDate(): Flow<List<Log>>

    @Query("SELECT * FROM logs WHERE id = :id")
    fun getLog(id: Int): Flow<Log>

    // cost, tip %, alphabetical, restaurant name, etc...
}