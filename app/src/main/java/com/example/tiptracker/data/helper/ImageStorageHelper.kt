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

class ImageStorageHelper(private val context: Context) {
    // Save a URI (from picker) to private app storage
    suspend fun saveImage(sourceUri: Uri, logId: Int): String = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "log_images/$logId").apply { mkdirs() }
        val fileName = "img_${System.currentTimeMillis()}.jpg"
        val destFile = File(dir, fileName)

        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            FileOutputStream(destFile).use { output ->
                compressAndCopy(input, output)
            }
        }
        destFile.absolutePath
    }

    fun deleteImage(filePath: String) {
        File(filePath).takeIf { it.exists() }?.delete()
    }

    fun loadBitmap(filePath: String): Bitmap? =
        File(filePath).takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.absolutePath) }

    private fun compressAndCopy(input: InputStream, output: FileOutputStream) {
        val bitmap = BitmapFactory.decodeStream(input)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, output) // 80% quality to save space
        bitmap.recycle()
    }
}