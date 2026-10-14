package com.strathmore.afyaearly.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "shap_explanations",
    foreignKeys = [
        ForeignKey(
            entity = PredictionResultEntity::class,
            parentColumns = ["predictionId"],
            childColumns = ["predictionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("predictionId")]
)
data class SHAPExplanationEntity(
    @PrimaryKey(autoGenerate = true)
    val explanationId: Long = 0,
    val predictionId: Long,
    val topFeature1: String,
    val impact1: Float,
    val statement1: String,
    val topFeature2: String,
    val impact2: Float,
    val statement2: String,
    val topFeature3: String,
    val impact3: Float,
    val statement3: String
)
