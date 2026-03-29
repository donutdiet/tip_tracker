package com.example.tiptracker.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.tiptracker.data.dao.LogDao
import com.example.tiptracker.data.entity.Log

@Database(
    version = 2,
    entities = [Log::class],
    autoMigrations = [
        AutoMigration(from = 1, to = 2)
    ]
)
abstract class TipTrackerDatabase : RoomDatabase() {
    abstract fun logDao(): LogDao
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