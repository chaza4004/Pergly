package com.example.pergly.local

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SensorHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(item: SensorHistoryEntity)

    @Query("SELECT * FROM sensor_history ORDER BY timestamp DESC")
    fun getAllHistory(): LiveData<List<SensorHistoryEntity>>

    @Query("SELECT * FROM sensor_history ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestHistory(): SensorHistoryEntity?

    @Query("DELETE FROM sensor_history")
    suspend fun clearAllHistory()
}