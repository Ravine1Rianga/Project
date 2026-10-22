package com.strath.finalpr0ject.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.strath.finalpr0ject.data.local.dao.*
import com.strath.finalpr0ject.data.local.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ChildEntity::class,
        ScreeningEntity::class,
        NutritionEntity::class,
        SyncQueueEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun childDao(): ChildDao
    abstract fun screeningDao(): ScreeningDao
    abstract fun nutritionDao(): NutritionDao
    abstract fun syncQueueDao(): SyncQueueDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "afya_early_offline.db"
                )
                    .addCallback(DatabasePrepopulateCallback())
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabasePrepopulateCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        seedDatabase(database)
                    }
                }
            }

            private suspend fun seedDatabase(db: AppDatabase) {
                val childDao = db.childDao()
                val nutritionDao = db.nutritionDao()
                val screeningDao = db.screeningDao()

                val seedChildren = listOf(
                    ChildEntity(
                        childId = "TK-1042",
                        name = "Akiru Ewoi",
                        ageMonths = 18,
                        sex = "Male",
                        county = "Turkana",
                        assignedChwId = "CHW-1042",
                        assignedChwName = "Maria Akiru",
                        maternalEducation = "Primary",
                        waterSource = "Unimproved Well",
                        householdSize = 5,
                        currentHaz = -2.4f,
                        currentWhz = -1.6f,
                        currentMuac = 12.5f,
                        latestRiskTier = "MODERATE RISK",
                        latestProbability = 0.68f,
                        lastScreeningDate = System.currentTimeMillis() - 86400000L * 3,
                        motorMilestoneScore = 78,
                        cognitiveMilestoneScore = 80,
                        languageMilestoneScore = 72,
                        isSynced = true
                    ),
                    ChildEntity(
                        childId = "TK-1043",
                        name = "Ekai Lomor",
                        ageMonths = 22,
                        sex = "Female",
                        county = "Turkana",
                        assignedChwId = "CHW-1042",
                        assignedChwName = "Maria Akiru",
                        maternalEducation = "None",
                        waterSource = "River Bed",
                        householdSize = 7,
                        currentHaz = -3.2f,
                        currentWhz = -2.8f,
                        currentMuac = 11.2f,
                        latestRiskTier = "HIGH RISK",
                        latestProbability = 0.89f,
                        lastScreeningDate = System.currentTimeMillis() - 86400000L * 1,
                        motorMilestoneScore = 60,
                        cognitiveMilestoneScore = 65,
                        languageMilestoneScore = 58,
                        isSynced = true
                    ),
                    ChildEntity(
                        childId = "TK-1044",
                        name = "Lokai Chegem",
                        ageMonths = 14,
                        sex = "Male",
                        county = "Turkana",
                        assignedChwId = "CHW-1042",
                        assignedChwName = "Maria Akiru",
                        maternalEducation = "Secondary",
                        waterSource = "Piped Water Kiosk",
                        householdSize = 4,
                        currentHaz = -0.8f,
                        currentWhz = -0.4f,
                        currentMuac = 13.8f,
                        latestRiskTier = "LOW RISK",
                        latestProbability = 0.18f,
                        lastScreeningDate = System.currentTimeMillis() - 86400000L * 5,
                        motorMilestoneScore = 92,
                        cognitiveMilestoneScore = 90,
                        languageMilestoneScore = 88,
                        isSynced = true
                    )
                )
                childDao.insertAll(seedChildren)

                // Seed sample nutrition log
                nutritionDao.insertMeal(
                    NutritionEntity(
                        childId = "TK-1042",
                        mealDescription = "Sorghum porridge and fresh camel milk",
                        categoriesString = "Dairy,Grains",
                        calculatedDdi = 4.2f,
                        isSynced = true
                    )
                )

                // Seed sample clinical screening
                screeningDao.insertScreening(
                    ScreeningEntity(
                        childId = "TK-1042",
                        haz = -2.4f,
                        whz = -1.6f,
                        muac = 12.5f,
                        stuntingRiskTier = "MODERATE RISK",
                        predictedProbability = 0.68f,
                        clinicalNotes = "Mild bilateral muscle wasting. Prescribed supplementary feeding with Ready-to-Use Supplementary Food (RUSF).",
                        isSynced = true
                    )
                )
            }
        }
    }
}
