package com.example.data.database.daos

import androidx.room.*
import com.example.data.database.entities.DeviceEntity
import com.example.data.database.entities.AnalyticsLogEntity
import com.example.data.database.entities.AlertEntity
import com.example.data.database.entities.SettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDao {
    @Query("SELECT * FROM devices ORDER BY id ASC")
    fun getAllDevicesFlow(): Flow<List<DeviceEntity>>

    @Query("SELECT * FROM devices ORDER BY id ASC")
    suspend fun getAllDevicesDirect(): List<DeviceEntity>

    @Query("SELECT * FROM devices WHERE id = :id LIMIT 1")
    fun getDeviceByIdFlow(id: Int): Flow<DeviceEntity?>

    @Query("SELECT * FROM devices WHERE id = :id LIMIT 1")
    suspend fun getDeviceByIdDirect(id: Int): DeviceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: DeviceEntity): Long

    @Update
    suspend fun updateDevice(device: DeviceEntity)

    @Delete
    suspend fun deleteDevice(device: DeviceEntity)

    @Query("DELETE FROM devices WHERE id = :id")
    suspend fun deleteDeviceById(id: Int)
}

@Dao
interface AnalyticsLogDao {
    @Query("SELECT * FROM analytics_logs ORDER BY timestamp DESC")
    fun getAllLogsFlow(): Flow<List<AnalyticsLogEntity>>

    @Query("SELECT * FROM analytics_logs WHERE deviceId = :deviceId AND monthGroup = :monthGroup ORDER BY dayOfMonthGroup ASC")
    fun getLogsForDeviceAndMonthFlow(deviceId: Int, monthGroup: String): Flow<List<AnalyticsLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AnalyticsLogEntity)

    @Query("DELETE FROM analytics_logs")
    suspend fun clearAllLogs()
}

@Dao
interface AlertDao {
    @Query("SELECT * FROM alerts ORDER BY timestamp DESC")
    fun getAllAlertsFlow(): Flow<List<AlertEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: AlertEntity)

    @Query("DELETE FROM alerts")
    suspend fun clearAllAlerts()
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings WHERE id = 1 LIMIT 1")
    fun getSettingsFlow(): Flow<SettingsEntity?>

    @Query("SELECT * FROM settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsDirect(): SettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSettings(settings: SettingsEntity)
}
