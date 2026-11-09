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

    @Query("SELECT * FROM children WHERE assignedChwId = :chwId ORDER BY lastScreeningDate DESC")
    fun observeChildrenByChw(chwId: String): Flow<List<ChildEntity>>

    @Query("SELECT * FROM children WHERE assignedChwId = :chwId AND isApprovedForTracking = 1 ORDER BY lastScreeningDate DESC")
    fun observeApprovedChildrenByChw(chwId: String): Flow<List<ChildEntity>>

    @Query("SELECT * FROM children WHERE assignedChwId = :chwId AND isApprovedForTracking = 1 ORDER BY lastScreeningDate DESC")
    suspend fun getApprovedChildrenByChw(chwId: String): List<ChildEntity>

    @Query("SELECT * FROM children WHERE approvalStatus = 'pending_approval' ORDER BY lastScreeningDate DESC")
    fun observePendingApprovalChildren(): Flow<List<ChildEntity>>

    @Query("UPDATE children SET isApprovedForTracking = 1, approvalStatus = 'approved', pendingSync = 1 WHERE childId = :childId")
    suspend fun approveChildForTracking(childId: String)

    @Query("SELECT * FROM children WHERE guardianUid = :guardianUid LIMIT 1")
    suspend fun getChildByGuardianUid(guardianUid: String): ChildEntity?

    @Query("SELECT * FROM children WHERE pendingSync = 1")
    suspend fun getPendingSync(): List<ChildEntity>

    @Query("UPDATE children SET pendingSync = 0 WHERE childId = :childId")
    suspend fun markSynced(childId: String)

    @Query("DELETE FROM children WHERE childId = :childId")
    suspend fun deleteById(childId: String)
}
