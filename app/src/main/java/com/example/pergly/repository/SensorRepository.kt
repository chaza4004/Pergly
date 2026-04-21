package com.example.pergly.repository

import androidx.lifecycle.LiveData
import com.example.pergly.local.SensorHistoryDao
import com.example.pergly.local.SensorHistoryEntity

class SensorRepository(private val dao: SensorHistoryDao) {

    val allHistory: LiveData<List<SensorHistoryEntity>> = dao.getAllHistory()

    suspend fun insertHistory(item: SensorHistoryEntity) {
        dao.insertHistory(item)
    }

    suspend fun clearAllHistory() {
        dao.clearAllHistory()
    }

    suspend fun getLatestHistory(): SensorHistoryEntity? {
        return dao.getLatestHistory()
    }
}