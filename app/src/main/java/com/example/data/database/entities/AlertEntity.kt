package com.example.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alerts")
data class AlertEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val deviceId: Int,
    val deviceName: String,
    val timestamp: Long,
    val title: String,
    val message: String,
    val cause: String,
    val severity: String // "warning", "danger", "info"
)
