package com.example.tiptracker.ui_old.util

import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

fun formatCurrency (amount: Double): String {
    val formatter = NumberFormat.getCurrencyInstance()
    return formatter.format(amount)
}

fun formatDate (date: LocalDate): String {
    val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US)
    return date.format(formatter)
}