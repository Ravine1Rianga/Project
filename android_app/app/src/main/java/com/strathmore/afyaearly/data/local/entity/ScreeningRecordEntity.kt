package com.strathmore.afyaearly.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "screening_records",
    foreignKeys = [
        ForeignKey(
            entity = PatientEntity::class,
            parentColumns = ["childId"],
            childColumns = ["childId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("childId")]
)
data class ScreeningRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val screeningId: Long = 0,
    val childId: String,
    val dateTimestamp: Long = System.currentTimeMillis(),
    val weightKg: Float,
    val heightCm: Float,
    val muacCm: Float,
    val haz: Float,
    val waz: Float,
    val whz: Float,
    val edema: Int = 0,
    val ddiScore: Int,
    val isMad: Int,
    val diarrhoeaRecent: Int,
    val feverRecent: Int,
    val coughRecent: Int,
    val dewormed: Int,
    val vitASupp: Int,
    val vaccinationComplete: Int
)
