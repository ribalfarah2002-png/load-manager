package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.database.daos.AlertDao
import com.example.data.database.daos.AnalyticsLogDao
import com.example.data.database.daos.DeviceDao
import com.example.data.database.daos.SettingsDao
import com.example.data.database.entities.AlertEntity
import com.example.data.database.entities.AnalyticsLogEntity
import com.example.data.database.entities.DeviceEntity
import com.example.data.database.entities.SettingsEntity

@Database(
    entities = [
        DeviceEntity::class,
        AnalyticsLogEntity::class,
        AlertEntity::class,
        SettingsEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deviceDao(): DeviceDao
    abstract fun analyticsLogDao(): AnalyticsLogDao
    abstract fun alertDao(): AlertDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smart_load_manager_db"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
