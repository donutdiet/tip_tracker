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
        if (uris.isEmpty()) return Result.success(0)

        val currentCount = dao.getImageCount(logId)
        val remaining = MAX_IMAGES_PER_LOG - currentCount
        if (remaining <= 0) return Result.failure(Exception("Image limit reached for this log (10)"))

        val urisToSave = uris.take(remaining)
        val nextOrder = currentCount
        val savedFilePaths = mutableListOf<String>()

        return try {
            val images = urisToSave.mapIndexed { index, uri ->
                val filePath = imageStorageHelper.saveImage(uri, logId)
                savedFilePaths += filePath
                LogImage(logId = logId, filePath = filePath, order = nextOrder + index)
            }

            dao.insertAll(images)
            Result.success(images.size)
        } catch (e: Exception) {
            savedFilePaths.forEach { filePath ->
                imageStorageHelper.deleteImage(filePath)
            }
            Result.failure(e)
        }
    }

    suspend fun deleteImage(logId: Int, imageId: Int, filePath: String) {
        check(imageStorageHelper.deleteImage(filePath)) {
            "Couldn't delete image file from app storage."
        }
        dao.deleteImageById(imageId)
        val remainingImages = dao.getAllImagesForLogOnce(logId)
        reorderImages(remainingImages)
    }

    suspend fun reorderImages(images: List<LogImage>) {
        val reindexed = images.mapIndexed { index, image -> image.copy(order = index) }
        dao.updateAll(reindexed)
    }
}