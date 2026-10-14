package com.strathmore.afyaearly.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "prediction_results",
    foreignKeys = [
        ForeignKey(
            entity = ScreeningRecordEntity::class,
            parentColumns = ["screeningId"],
            childColumns = ["screeningId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("screeningId")]
)
data class PredictionResultEntity(
    @PrimaryKey(autoGenerate = true)
    val predictionId: Long = 0,
    val screeningId: Long,
    val rawProbability: Float,
    val calibratedProbability: Float,
    val riskTier: String, // "LOW RISK", "MODERATE RISK", "HIGH RISK"
    val recommendedAction: String,
    val isSynced: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
