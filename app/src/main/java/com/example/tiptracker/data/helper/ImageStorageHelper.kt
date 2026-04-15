package com.example.tiptracker.data.helper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

class ImageStorageHelper(private val context: Context) {
    // Save a URI (from picker) to private app storage
    suspend fun saveImage(sourceUri: Uri, logId: Int): String = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "log_images/$logId")
        if (!dir.exists() && !dir.mkdirs()) {
            throw IllegalStateException("Couldn't create image directory for log $logId")
        }

        val fileName = "img_${UUID.randomUUID()}.jpg"
        val destFile = File(dir, fileName)

        try {
            val input = context.contentResolver.openInputStream(sourceUri)
                ?: throw IllegalArgumentException("Couldn't open selected image.")

            input.use {
                FileOutputStream(destFile).use { output ->
                    compressAndCopy(it, output)
                }
            }
        } catch (e: Exception) {
            destFile.delete()
            throw e
        }

        destFile.absolutePath
    }

    fun deleteImage(filePath: String): Boolean {
        val file = File(filePath)
        return !file.exists() || file.delete()
    }

    fun deleteImagesForLog(logId: Int): Boolean {
        val directory = File(context.filesDir, "log_images/$logId")
        return !directory.exists() || directory.deleteRecursively()
    }

    fun loadBitmap(filePath: String): Bitmap? =
        File(filePath).takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.absolutePath) }

    private fun compressAndCopy(input: InputStream, output: FileOutputStream) {
        val bitmap = BitmapFactory.decodeStream(input)
            ?: throw IllegalArgumentException("Selected file is not a valid image.")

        check(bitmap.compress(Bitmap.CompressFormat.JPEG, 80, output)) {
            "Couldn't write image to app storage."
        }
        bitmap.recycle()
    }
}