package com.example.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val isOn: Boolean = false,
    val isManualOn: Boolean = false, // state of manually flipping the switch
    val isManualMode: Boolean = true, // true = Manual, false = Auto
    
    // Auto Mode 1: Max Cost Limit
    val maxCostLimit: Double = 10000.0, // Active running limit
    val pendingMaxCostLimit: Double = 10000.0, // Draft slider limit
    val isMaxCostActive: Boolean = false,
    val maxCostStatus: Int = 0, // 0 = Stopped/Cancelled, 1 = Running, 2 = Paused
    
    // Auto Mode 2: Scheduled Hours
    // Comma-separated string of active indices in range 0..287 (5-minute offsets in 24 hours)
    val selectedSlotsCsv: String = "", // Active running slots
    val pendingSlotsCsv: String = "", // Drafted slots on clock dial
    val scheduledHoursStatus: Int = 0, // 0 = stopped, 1 = running, 2 = paused
    
    // Auto Mode 3: Seconds Plan
    val secondsOn: Int = 30, // Active running seconds ON
    val secondsOff: Int = 30, // Active running seconds OFF
    val pendingSecondsOn: Int = 30, // Draft seconds ON slider
    val pendingSecondsOff: Int = 30, // Draft seconds OFF slider
    val secondsPlanStatus: Int = 0, // 0 = stopped, 1 = running, 2 = paused
    
    // Realtime sensor values from PZEM-004T / ESP8266 Connection
    val voltage: Double = 220.0,
    val current: Double = 0.0,
    val activePower: Double = 0.0,
    val energyWh: Double = 0.0,
    val frequency: Double = 50.0,
    val powerFactor: Double = 1.0,
    
    // UI Local tracking states for timers
    val currentSecondsPhaseIsOn: Boolean = true,
    val secondsTimeLeftInPhase: Int = 0,
    val localTimerActive: Boolean = false,
    
    // Calculated Cost in SYP for this device
    val costSyp: Double = 0.0,

    // Device type and protection settings
    val deviceType: String = "انارة", // "ضاغط", "حمل حراري", "اجهزة كهربائية", "انارة"
    val timeToOn: Int = 0, // seconds for ON state protection (prevent turning OFF)
    val timeToOff: Int = 0, // seconds for OFF state protection (prevent turning ON)
    val lastStateChangeTime: Long = 0L // timestamp in ms of last state change
)
