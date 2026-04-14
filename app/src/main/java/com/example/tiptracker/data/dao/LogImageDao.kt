package com.example.tiptracker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.tiptracker.data.entity.LogImage
import kotlinx.coroutines.flow.Flow

@Dao
interface LogImageDao {
    @Query("SELECT * FROM log_image WHERE log_id = :logId ORDER BY `order` ASC")
    fun getAllImagesForLog(logId: Int): Flow<List<LogImage>>

    @Query("SELECT COUNT(*) FROM log_image WHERE log_id = :logId")
    suspend fun getImageCount(logId: Int): Int

    @Insert
    suspend fun insertAll(images: List<LogImage>)

    @Update
    suspend fun updateAll(images: List<LogImage>)

    @Query("DELETE FROM log_image WHERE id = :imageId")
    suspend fun deleteImageById(imageId: Int)
}