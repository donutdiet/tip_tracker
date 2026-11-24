package com.example.tiptracker.utils

import java.util.Locale

fun formatCurrency(value: Double): String {
    return String.format(Locale.getDefault(), "%.2f", value)
}

fun formatTipPercent(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        String.format(Locale.getDefault(), "%.2f", value)
    }
}

/** Trims and turns each run of spaces, tabs or newlines into a single space. */
fun String.collapseWhitespace(): String = trim().replace(WHITESPACE_RUN, " ")

private val WHITESPACE_RUN = Regex("\\s+")
