package com.example.tiptracker.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "log_image",
    foreignKeys = [
        ForeignKey(
            entity = Log::class,
            parentColumns = ["id"],
            childColumns = ["log_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("log_id")]
)
data class LogImage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "log_id") val logId: Int,
    @ColumnInfo(name = "file_path") val filePath: String,
    @ColumnInfo(name = "order") val order: Int
)