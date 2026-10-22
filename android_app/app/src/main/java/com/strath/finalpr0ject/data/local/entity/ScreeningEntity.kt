package com.strath.finalpr0ject.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "screenings")
data class ScreeningEntity(
    @PrimaryKey
    val screeningId: String = UUID.randomUUID().toString(),
    val childId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val haz: Float,
    val whz: Float,
    val muac: Float,
    val stuntingRiskTier: String,
    val predictedProbability: Float,
    val clinicalNotes: String = "",
    val isSynced: Boolean = false
)
