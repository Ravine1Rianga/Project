package com.strath.finalpr0ject.data.local.dao

import androidx.room.*
import com.strath.finalpr0ject.data.local.entity.ChildEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChildDao {

    @Query("SELECT * FROM children WHERE childId = :childId LIMIT 1")
    suspend fun getChildById(childId: String): ChildEntity?

    @Query("SELECT * FROM children WHERE childId = :childId LIMIT 1")
    fun observeChildById(childId: String): Flow<ChildEntity?>

    @Query("SELECT * FROM children ORDER BY lastScreeningDate DESC")
    suspend fun getAllChildren(): List<ChildEntity>

    @Query("SELECT * FROM children ORDER BY lastScreeningDate DESC")
    fun observeAllChildren(): Flow<List<ChildEntity>>

    @Query("SELECT * FROM children WHERE assignedChwId = :chwId ORDER BY lastScreeningDate DESC")
    suspend fun getChildrenByChw(chwId: String): List<ChildEntity>

    @Query("SELECT * FROM children WHERE name LIKE '%' || :query || '%' OR childId LIKE '%' || :query || '%'")
    suspend fun searchChildren(query: String): List<ChildEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(child: ChildEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(children: List<ChildEntity>)

    @Query("DELETE FROM children WHERE childId = :childId")
    suspend fun deleteById(childId: String)
}
