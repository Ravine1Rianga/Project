package com.strathmore.afyaearly.data.local.dao

import androidx.room.*
import com.strathmore.afyaearly.data.local.entity.PredictionResultEntity
import com.strathmore.afyaearly.data.local.entity.SHAPExplanationEntity
import kotlinx.coroutines.flow.Flow

data class FullScreeningResult(
    @Embedded val prediction: PredictionResultEntity,
    @Relation(
        parentColumn = "predictionId",
        entityColumn = "predictionId"
    )
    val shapExplanation: SHAPExplanationEntity?
)

@Dao
interface PredictionDao {
    @Insert
    suspend fun insertPrediction(prediction: PredictionResultEntity): Long

    @Insert
    suspend fun insertSHAPExplanation(explanation: SHAPExplanationEntity)

    @Transaction
    @Query("SELECT * FROM prediction_results WHERE screeningId = :screeningId LIMIT 1")
    suspend fun getResultForScreening(screeningId: Long): FullScreeningResult?

    @Query("SELECT COUNT(*) FROM prediction_results WHERE riskTier = 'HIGH RISK'")
    fun getHighRiskCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM prediction_results")
    fun getTotalPredictionsCount(): Flow<Int>
}
