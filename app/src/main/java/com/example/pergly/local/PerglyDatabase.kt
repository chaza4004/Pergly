package com.example.pergly.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [SensorHistoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class PerglyDatabase : RoomDatabase() {

    abstract fun sensorHistoryDao(): SensorHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: PerglyDatabase? = null

        fun getDatabase(context: Context): PerglyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PerglyDatabase::class.java,
                    "pergly_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}