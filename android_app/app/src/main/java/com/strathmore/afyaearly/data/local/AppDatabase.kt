package com.strathmore.afyaearly.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.strathmore.afyaearly.data.local.dao.PatientDao
import com.strathmore.afyaearly.data.local.dao.PredictionDao
import com.strathmore.afyaearly.data.local.dao.ScreeningDao
import com.strathmore.afyaearly.data.local.entity.PatientEntity
import com.strathmore.afyaearly.data.local.entity.PredictionResultEntity
import com.strathmore.afyaearly.data.local.entity.SHAPExplanationEntity
import com.strathmore.afyaearly.data.local.entity.ScreeningRecordEntity

@Database(
    entities = [
        PatientEntity::class,
        ScreeningRecordEntity::class,
        PredictionResultEntity::class,
        SHAPExplanationEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun patientDao(): PatientDao
    abstract fun screeningDao(): ScreeningDao
    abstract fun predictionDao(): PredictionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "afya_early_childhood.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
