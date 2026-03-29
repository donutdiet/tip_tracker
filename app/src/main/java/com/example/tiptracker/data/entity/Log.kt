package com.example.tiptracker.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "log")
data class Log (
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "bill") val bill: Double,
    @ColumnInfo(name = "tip_percent") val tipPercent: Double,
    @ColumnInfo(name = "total") val total: Double,
    @ColumnInfo(name = "party_size") val partySize: Int,
    @ColumnInfo(name = "restaurant_name") val restaurantName: String,
    @ColumnInfo(name = "address") val address: String?,
    @ColumnInfo(name = "review") val review: String,
    @ColumnInfo(name = "rating") val rating: Double, // 0-10, 1 decimal
    @ColumnInfo(name = "date") val date: String // yyyy-mm-dd
)