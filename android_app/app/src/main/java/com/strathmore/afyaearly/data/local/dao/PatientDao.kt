package com.strathmore.afyaearly.data.local.dao

import androidx.room.*
import com.strathmore.afyaearly.data.local.entity.PatientEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PatientDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPatient(patient: PatientEntity)

    @Query("SELECT * FROM patients ORDER BY createdAt DESC")
    fun getAllPatients(): Flow<List<PatientEntity>>

    @Query("SELECT * FROM patients WHERE childId = :childId LIMIT 1")
    suspend fun getPatientById(childId: String): PatientEntity?

    @Query("SELECT * FROM patients WHERE childId LIKE '%' || :query || '%' OR name LIKE '%' || :query || '%'")
    fun searchPatients(query: String): Flow<List<PatientEntity>>

    @Query("SELECT COUNT(*) FROM patients")
    fun getPatientCount(): Flow<Int>
}
