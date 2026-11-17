package com.example.tiptracker.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "log", indices = [Index("place_id")])
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
    @ColumnInfo(name = "date") val date: String, // yyyy-mm-dd
    // Set only when the restaurant was picked from Places autocomplete; null for free-text logs.
    // place_id groups logs of the same restaurant; lat/lng position the map marker.
    @ColumnInfo(name = "place_id") val placeId: String? = null,
    @ColumnInfo(name = "latitude") val latitude: Double? = null,
    @ColumnInfo(name = "longitude") val longitude: Double? = null
)