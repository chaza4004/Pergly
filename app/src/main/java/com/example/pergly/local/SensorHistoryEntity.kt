package com.example.pergly.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sensor_history")
data class SensorHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val voltage: Float? = null,
    val current: Float? = null,
    val power: Float? = null,
    val lightIntensity: Float? = null,
    val windSpeed: Float? = null,
    val motorStatus: String? = null,
    val weatherCondition: String? = null,
    val city: String? = null
)