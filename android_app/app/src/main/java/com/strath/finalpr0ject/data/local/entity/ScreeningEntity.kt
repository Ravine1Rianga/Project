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
    val ageMonths: Int = 12,
    val weightKg: Float = 0f,
    val heightCm: Float = 0f,
    val haz: Float = 0f,
    val whz: Float = 0f,
    val muac: Float = 0f,
    val stuntingRiskTier: String = "LOW RISK",
    val predictedProbability: Float = 0f,
    val clinicalNotes: String = "",
    val feedingCategory: String = "", // e.g., "Exclusive Breastfeeding (0-6m)", "Complementary Weaning (6-9m)"
    val isSynced: Boolean = false
)

