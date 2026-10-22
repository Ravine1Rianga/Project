package com.strath.finalpr0ject.data.local.dao

import androidx.room.*
import com.strath.finalpr0ject.data.local.entity.ScreeningEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScreeningDao {

    @Query("SELECT * FROM screenings WHERE childId = :childId ORDER BY timestamp DESC")
    suspend fun getScreeningsForChild(childId: String): List<ScreeningEntity>

    @Query("SELECT * FROM screenings WHERE childId = :childId ORDER BY timestamp DESC")
    fun observeScreeningsForChild(childId: String): Flow<List<ScreeningEntity>>

    @Query("SELECT * FROM screenings WHERE isSynced = 0 ORDER BY timestamp ASC")
    suspend fun getUnsyncedScreenings(): List<ScreeningEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScreening(screening: ScreeningEntity)

    @Query("UPDATE screenings SET isSynced = 1 WHERE screeningId = :id")
    suspend fun markSynced(id: String)
}
