package com.example.tiptracker.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Force UTC to match how DatePicker handles time
fun convertMillisToDate(millis: Long): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
        timeZone = java.util.TimeZone.getTimeZone("UTC")
    }
    return formatter.format(Date(millis))
}

fun convertDateToMillis(date: String): Long {
    val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
        timeZone = java.util.TimeZone.getTimeZone("UTC")
    }
    return try {
        parser.parse(date)?.time
    } catch (_: Exception) {
        null
    } ?: System.currentTimeMillis()
}

// yyyy-MM-dd to MMMM dd, yyyy
fun formatDateForDisplay(dateString: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
            isLenient = false
        }
        val formatter = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
        val parsedDate = parser.parse(dateString) ?: return dateString
        formatter.format(parsedDate)
    } catch (_: Exception) {
        dateString
    }
}