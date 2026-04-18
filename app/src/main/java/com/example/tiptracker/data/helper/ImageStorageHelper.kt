package com.example.tiptracker.data.helper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.roundToInt

class ImageStorageHelper(private val context: Context) {
    companion object {
        private const val IMAGES_DIRECTORY = "log_images"
        private const val IMAGE_FILE_PREFIX = "img_"
        private const val IMAGE_FILE_EXTENSION = ".jpg"
        private const val MAX_SAVED_IMAGE_DIMENSION = 1280
        private const val JPEG_QUALITY = 80

        fun resolveStoredImageFile(filesDir: File, relativePath: String): File =
            File(filesDir, relativePath)
    }

    // Save a URI (from picker) to private app storage
    suspend fun saveImage(sourceUri: Uri, logId: Int): String = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "$IMAGES_DIRECTORY/$logId")
        if (!dir.exists() && !dir.mkdirs()) {
            throw IllegalStateException("Couldn't create image directory for log $logId")
        }

        val fileName = "$IMAGE_FILE_PREFIX${UUID.randomUUID()}$IMAGE_FILE_EXTENSION"
        val relativePath = buildRelativeImagePath(logId, fileName)
        val destFile = resolveStoredFile(relativePath)

        try {
            FileOutputStream(destFile).use { output ->
                compressAndCopy(sourceUri, output)
            }
        } catch (e: Exception) {
            destFile.delete()
            throw e
        }

        relativePath
    }

    fun deleteImage(relativePath: String): Boolean {
        val file = resolveStoredFile(relativePath)
        return !file.exists() || file.delete()
    }

    fun deleteImagesForLog(logId: Int): Boolean {
        val directory = File(context.filesDir, "$IMAGES_DIRECTORY/$logId")
        return !directory.exists() || directory.deleteRecursively()
    }

    private fun compressAndCopy(sourceUri: Uri, output: FileOutputStream) {
        val bitmap = decodeBitmapForStorage(
            sourceUri = sourceUri,
        ) ?: throw IllegalArgumentException("Selected file is not a valid image.")

        check(bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)) {
            "Couldn't write image to app storage."
        }
        bitmap.recycle()
    }

    private fun decodeBitmapForStorage(sourceUri: Uri): Bitmap? {
        return runCatching {
            val source = ImageDecoder.createSource(context.contentResolver, sourceUri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val (targetWidth, targetHeight) = calculateTargetDimensions(
                    width = info.size.width,
                    height = info.size.height,
                    maxDimension = MAX_SAVED_IMAGE_DIMENSION
                )
                decoder.setTargetSize(targetWidth, targetHeight)
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.isMutableRequired = false
            }
        }.getOrNull()
    }

    private fun calculateTargetDimensions(
        width: Int,
        height: Int,
        maxDimension: Int
    ): Pair<Int, Int> {
        if (width <= 0 || height <= 0) {
            return 1 to 1
        }

        val scale = minOf(
            1f,
            maxDimension.toFloat() / width,
            maxDimension.toFloat() / height
        )
        val targetWidth = (width * scale).roundToInt().coerceAtLeast(1)
        val targetHeight = (height * scale).roundToInt().coerceAtLeast(1)
        return targetWidth to targetHeight
    }

    private fun buildRelativeImagePath(logId: Int, fileName: String): String =
        "$IMAGES_DIRECTORY/$logId/$fileName"

    private fun resolveStoredFile(relativePath: String): File =
        resolveStoredImageFile(context.filesDir, relativePath)
}