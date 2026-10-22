package com.strath.finalpr0ject.data.local.dao

import androidx.room.*
import com.strath.finalpr0ject.data.local.entity.NutritionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NutritionDao {

    @Query("SELECT * FROM nutrition_logs WHERE childId = :childId ORDER BY timestamp DESC")
    suspend fun getMealsForChild(childId: String): List<NutritionEntity>

    @Query("SELECT * FROM nutrition_logs WHERE childId = :childId ORDER BY timestamp DESC")
    fun observeMealsForChild(childId: String): Flow<List<NutritionEntity>>

    @Query("SELECT * FROM nutrition_logs WHERE isSynced = 0 ORDER BY timestamp ASC")
    suspend fun getUnsyncedMeals(): List<NutritionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeal(meal: NutritionEntity)

    @Query("UPDATE nutrition_logs SET isSynced = 1 WHERE logId = :id")
    suspend fun markSynced(id: String)
}
