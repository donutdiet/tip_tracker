package com.example.tiptracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "logs")
data class Log (
    @PrimaryKey(autoGenerate = true)
    val id: Int,

    val bill: Double,
    val tipPercent: Double,
    val partySize: Int,
    val tip: Double,
    val total: Double,
    val perPerson: Double,
    val restaurantName: String,
    val restaurantDescription: String,
    val date: LocalDate
)