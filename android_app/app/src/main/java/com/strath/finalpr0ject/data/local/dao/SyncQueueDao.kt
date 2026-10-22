package com.strath.finalpr0ject.data.local.dao

import androidx.room.*
import com.strath.finalpr0ject.data.local.entity.SyncQueueEntity

@Dao
interface SyncQueueDao {

    @Query("SELECT * FROM sync_queue ORDER BY timestamp ASC")
    suspend fun getPendingItems(): List<SyncQueueEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueue(item: SyncQueueEntity): Long

    @Delete
    suspend fun delete(item: SyncQueueEntity)

    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE sync_queue SET retryCount = retryCount + 1 WHERE id = :id")
    suspend fun incrementRetry(id: Long)

    @Query("SELECT COUNT(*) FROM sync_queue")
    suspend fun getPendingCount(): Int
}
