package com.strath.finalpr0ject.data.local.dao

import androidx.room.*
import com.strath.finalpr0ject.data.local.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow

/**
 * UserProfileDao
 * ==============
 * Data Access Object for locally cached user profiles.
 * Enables completely offline authentication and profile reads.
 */
@Dao
interface UserProfileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: UserProfileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<UserProfileEntity>)

    @Query("SELECT * FROM user_profiles WHERE uid = :uid LIMIT 1")
    suspend fun getByUid(uid: String): UserProfileEntity?

    @Query("SELECT * FROM user_profiles WHERE email = :email LIMIT 1")
    suspend fun getByEmail(email: String): UserProfileEntity?

    @Query("SELECT * FROM user_profiles WHERE role = 'CHW'")
    suspend fun getAllCHWs(): List<UserProfileEntity>

    @Query("SELECT * FROM user_profiles WHERE role = 'CHW'")
    fun observeAllCHWs(): Flow<List<UserProfileEntity>>

    @Query("SELECT * FROM user_profiles WHERE role IN ('PARENT', 'GUARDIAN')")
    suspend fun getAllParents(): List<UserProfileEntity>

    @Query("SELECT * FROM user_profiles WHERE role IN ('PARENT', 'GUARDIAN')")
    fun observeAllParents(): Flow<List<UserProfileEntity>>

    @Query("SELECT * FROM user_profiles WHERE pendingSync = 1")
    suspend fun getPendingSync(): List<UserProfileEntity>

    @Query("UPDATE user_profiles SET isVerified = 1, verificationStatus = 'approved', profileCompleted = 1, pendingSync = 0, lastSyncTimestamp = :timestamp WHERE uid = :uid")
    suspend fun markApproved(uid: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE user_profiles SET pendingSync = 0, lastSyncTimestamp = :timestamp WHERE uid = :uid")
    suspend fun markSynced(uid: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE user_profiles SET assignedChildId = :childId, assignedChildName = :childName, profileCompleted = 1 WHERE uid = :uid")
    suspend fun updateChildLink(uid: String, childId: String, childName: String)

    @Query("UPDATE user_profiles SET photoLocalPath = :path WHERE uid = :uid")
    suspend fun updateLocalPhoto(uid: String, path: String)

    @Query("DELETE FROM user_profiles WHERE uid = :uid")
    suspend fun delete(uid: String)

    @Query("DELETE FROM user_profiles")
    suspend fun deleteAll()
}
