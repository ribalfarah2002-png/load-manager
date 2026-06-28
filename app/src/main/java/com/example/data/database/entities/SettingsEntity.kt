package com.example.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = 1,
    val espIpAddress: String = "192.168.4.1", // Default softAP or local IP
    val isSimulatorEnabled: Boolean = true,
    val tariffPerKWh: Double = 125.0, // Cost per kWh inside Syria
    val tariffBracket1Limit: Double = 300.0,
    val tariffBracket1Price: Double = 600.0,
    val tariffBracket2Price: Double = 1400.0
)
