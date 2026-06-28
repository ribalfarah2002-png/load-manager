package com.example.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "analytics_logs")
data class AnalyticsLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val deviceId: Int, // 0 for total, or specific deviceId
    val deviceName: String, // e.g. "الغسالة" or "الاستهلاك الكلي"
    val timestamp: Long,
    val monthGroup: String, // e.g., "كانون الثاني - شباط", "آذار - نيسان"
    val dayOfMonthGroup: Int, // Day offset 1 to 60 for coordinate display
    val kwh: Double,
    val costSyp: Double,
    val avgCurrent: Double,
    val avgPower: Double,
    val alertMsg: String? = null
)
