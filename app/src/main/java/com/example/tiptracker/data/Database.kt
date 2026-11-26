package com.example.tiptracker.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.DeleteColumn
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec
import com.example.tiptracker.data.dao.LogDao
import com.example.tiptracker.data.dao.LogImageDao
import com.example.tiptracker.data.entity.Log
import com.example.tiptracker.data.entity.LogImage

@Database(
    version = 6,
    entities = [
        Log::class,
        LogImage::class
    ],
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6, spec = TipTrackerDatabase.DropAddress::class)
    ]
)
abstract class TipTrackerDatabase : RoomDatabase() {
    @DeleteColumn(tableName = "log", columnName = "address")
    class DropAddress : AutoMigrationSpec

    abstract fun logDao(): LogDao
    abstract fun logImageDao(): LogImageDao
}

object DatabaseProvider {
    @Volatile
    private var INSTANCE: TipTrackerDatabase? = null

    fun getDatabase(context: Context): TipTrackerDatabase {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                TipTrackerDatabase::class.java,
                "tip_tracker_db"
            ).build()
            INSTANCE = instance
            instance
        }
    }
}