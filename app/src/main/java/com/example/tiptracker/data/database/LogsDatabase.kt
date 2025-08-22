package com.example.tiptracker.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.tiptracker.data.dao.LogDao
import com.example.tiptracker.data.entity.Log
import com.example.tiptracker.ui_old.util.Converters

@Database(entities = [Log::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class LogsDatabase: RoomDatabase() {

    abstract fun logDao(): LogDao

    companion object {
        @Volatile
        private var Instance: LogsDatabase? = null

        fun getDatabase(context: Context): LogsDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, LogsDatabase::class.java, "logs_database")
                    .build()
                    .also { Instance = it }
            }
        }
    }
}