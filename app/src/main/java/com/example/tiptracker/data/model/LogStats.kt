package com.example.tiptracker.data.model

data class LogStats(
    val totalLogs: Int,
    val avgBill: Double,
    val avgTipPercent: Double,
    val avgTotal: Double,
    val avgPartySize: Double,
    val avgRating: Double,
    val avgTipAmount: Double,
    val avgTotalPerPerson: Double
)

data class RatingCount(
    val rating: Double,
    val count: Int
)