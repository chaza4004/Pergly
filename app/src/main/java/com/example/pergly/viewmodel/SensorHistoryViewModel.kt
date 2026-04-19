package com.example.pergly.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.pergly.local.PerglyDatabase
import com.example.pergly.local.SensorHistoryEntity
import com.example.pergly.repository.SensorRepository
import kotlinx.coroutines.launch

class SensorHistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SensorRepository
    val allHistory: LiveData<List<SensorHistoryEntity>>

    init {
        val dao = PerglyDatabase.getDatabase(application).sensorHistoryDao()
        repository = SensorRepository(dao)
        allHistory = repository.allHistory
    }

    fun insertHistory(item: SensorHistoryEntity) {
        viewModelScope.launch {
            repository.insertHistory(item)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAllHistory()
        }
    }
}