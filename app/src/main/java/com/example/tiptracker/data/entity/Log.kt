package com.example.tiptracker.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.tiptracker.data.model.BillType
import com.example.tiptracker.data.model.totalPerPerson

@Entity(tableName = "log", indices = [Index("place_id")])
data class Log (
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "bill") val bill: Double,
    @ColumnInfo(name = "tip_percent") val tipPercent: Double,
    @ColumnInfo(name = "total") val total: Double,
    @ColumnInfo(name = "party_size") val partySize: Int,
    @ColumnInfo(name = "bill_type", defaultValue = "SPLIT") val billType: BillType = BillType.SPLIT,
    @ColumnInfo(name = "restaurant_name") val restaurantName: String,
    @ColumnInfo(name = "review") val review: String,
    @ColumnInfo(name = "rating") val rating: Double, // 0-10, 1 decimal
    @ColumnInfo(name = "date") val date: String, // yyyy-mm-dd

    @ColumnInfo(name = "place_id") val placeId: String? = null,
    @ColumnInfo(name = "latitude") val latitude: Double? = null,
    @ColumnInfo(name = "longitude") val longitude: Double? = null,
    @ColumnInfo(name = "coordinates_fetched_at") val coordinatesFetchedAt: Long? = null // can be cached 30 days
)

val Log.totalPerPerson: Double
    get() = totalPerPerson(total, partySize, billType)