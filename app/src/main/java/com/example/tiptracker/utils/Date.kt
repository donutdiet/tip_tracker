package com.example.tiptracker.utils

import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.temporal.ChronoUnit
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
// yyyy-MM-dd to how long ago it was: "Today", "Yesterday", "3 days ago", "2 weeks ago", ...
fun formatDateRelative(dateString: String, today: LocalDate = LocalDate.now()): String {
    val date = try {
        LocalDate.parse(dateString)
    } catch (_: Exception) {
        return dateString
    }
    val days = ChronoUnit.DAYS.between(date, today)
    fun ago(count: Long, unit: String) = if (count == 1L) "1 $unit ago" else "$count ${unit}s ago"
    return when {
        days < 0 -> formatDateForDisplay(dateString)
        days == 0L -> "Today"
        days == 1L -> "Yesterday"
        days < 7 -> ago(days, "day")
        days < 30 -> ago(days / 7, "week")
        days < 365 -> ago(ChronoUnit.MONTHS.between(date, today).coerceAtLeast(1), "month")
        else -> ago(ChronoUnit.YEARS.between(date, today), "year")
    }
}
