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
