package com.example.tiptracker.data.repository

import android.net.Uri
import com.example.tiptracker.data.dao.LogImageDao
import com.example.tiptracker.data.entity.LogImage
import com.example.tiptracker.data.helper.ImageStorageHelper
import kotlinx.coroutines.flow.Flow

class LogImageRepository(
    private val dao: LogImageDao,
    private val imageStorageHelper: ImageStorageHelper
) {
    companion object { const val MAX_IMAGES_PER_LOG = 10 }

    fun getImages(logId: Int): Flow<List<LogImage>> = dao.getAllImagesForLog(logId)

    // Saves all picked URIs in one shot, respecting the limit.
    // Returns how many were actually saved (could be less than picked if near the limit).
    suspend fun addImages(logId: Int, uris: List<Uri>): Result<Int> {
        val currentCount = dao.getImageCount(logId)
        val remaining = MAX_IMAGES_PER_LOG - currentCount
        if (remaining <= 0) return Result.failure(Exception("Image limit reached for this log (10)"))

        val urisToSave = uris.take(remaining)
        val nextOrder = currentCount + 1

        val images = urisToSave.mapIndexed { index, uri ->
            val filePath = imageStorageHelper.saveImage(uri, logId)
            LogImage(logId = logId, filePath = filePath, order = nextOrder + index)
        }

        dao.insertAll(images)
        return Result.success(images.size)
    }

    suspend fun deleteImage(imageId: Int, filePath: String) {
        imageStorageHelper.deleteImage(filePath)
        dao.deleteImageById(imageId)
    }

    suspend fun reorderImages(images: List<LogImage>) {
        val reindexed = images.mapIndexed { index, image -> image.copy(order = index) }
        dao.updateAll(reindexed)
    }
}