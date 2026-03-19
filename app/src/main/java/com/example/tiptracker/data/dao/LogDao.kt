package com.example.tiptracker.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.tiptracker.data.entity.Log
import kotlinx.coroutines.flow.Flow

@Dao
interface LogDao {
    @Query("SELECT * FROM log ORDER BY date DESC")
    fun getAllTips(): Flow<List<Log>>

    @Query("SELECT * FROM log WHERE id = :id")
    suspend fun getLogById(id: Int): Log?

    @Insert
    suspend fun insertLog(log: Log)

    @Update
    suspend fun updateLog(log: Log)

    @Delete
    suspend fun deleteLog(log: Log)
}