package com.strath.finalpr0ject.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.strath.finalpr0ject.data.local.dao.*
import com.strath.finalpr0ject.data.local.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * AppDatabase — version 4
 * ========================
 * Local SQLite database powering the full offline-first experience.
 *
 * Version history:
 *   v1 → v2: Added user_profiles table; added guardianUid, photoLocalPath,
 *             pendingSync columns to children table.
 *   v2 → v3: Added ageMonths, weightKg, heightCm, feedingCategory to screenings table.
 *   v3 → v4: Added approvalStatus and isApprovedForTracking to children table.
 */
@Database(
    entities = [
        ChildEntity::class,
        ScreeningEntity::class,
        NutritionEntity::class,
        SyncQueueEntity::class,
        UserProfileEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun childDao(): ChildDao
    abstract fun screeningDao(): ScreeningDao
    abstract fun nutritionDao(): NutritionDao
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun userProfileDao(): UserProfileDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS user_profiles (
                        uid TEXT NOT NULL PRIMARY KEY,
                        email TEXT NOT NULL,
                        displayName TEXT NOT NULL DEFAULT '',
                        role TEXT NOT NULL DEFAULT 'PARENT',
                        phone TEXT NOT NULL DEFAULT '',
                        county TEXT NOT NULL DEFAULT 'Turkana',
                        subCounty TEXT NOT NULL DEFAULT 'Turkana West',
                        ward TEXT NOT NULL DEFAULT '',
                        chwId TEXT NOT NULL DEFAULT '',
                        chwFacility TEXT NOT NULL DEFAULT '',
                        supervisingFacility TEXT NOT NULL DEFAULT '',
                        isVerified INTEGER NOT NULL DEFAULT 0,
                        verificationStatus TEXT NOT NULL DEFAULT 'pending',
                        assignedChildId TEXT NOT NULL DEFAULT '',
                        assignedChildName TEXT NOT NULL DEFAULT '',
                        profileCompleted INTEGER NOT NULL DEFAULT 0,
                        photoUrl TEXT NOT NULL DEFAULT '',
                        photoLocalPath TEXT NOT NULL DEFAULT '',
                        createdAt INTEGER NOT NULL DEFAULT 0,
                        lastSyncTimestamp INTEGER NOT NULL DEFAULT 0,
                        pendingSync INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_user_profiles_email ON user_profiles (email)")
                db.execSQL("ALTER TABLE children ADD COLUMN guardianUid TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE children ADD COLUMN photoLocalPath TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE children ADD COLUMN pendingSync INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE screenings ADD COLUMN ageMonths INTEGER NOT NULL DEFAULT 12")
                db.execSQL("ALTER TABLE screenings ADD COLUMN weightKg REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE screenings ADD COLUMN heightCm REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE screenings ADD COLUMN feedingCategory TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE children ADD COLUMN approvalStatus TEXT NOT NULL DEFAULT 'approved'")
                db.execSQL("ALTER TABLE children ADD COLUMN isApprovedForTracking INTEGER NOT NULL DEFAULT 1")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "afya_early_offline.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
