package com.strathmore.afyaearly.data.local.dao

import androidx.room.*
import com.strathmore.afyaearly.data.local.entity.ScreeningRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScreeningDao {
    @Insert
    suspend fun insertScreening(screening: ScreeningRecordEntity): Long

    @Query("SELECT * FROM screening_records WHERE childId = :childId ORDER BY dateTimestamp DESC")
    fun getScreeningsForChild(childId: String): Flow<List<ScreeningRecordEntity>>

    @Query("SELECT COUNT(*) FROM screening_records")
    fun getTotalScreeningsCount(): Flow<Int>
}
