package com.strath.finalpr0ject.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "nutrition_logs")
data class NutritionEntity(
    @PrimaryKey
    val logId: String = UUID.randomUUID().toString(),
    val childId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val mealDescription: String,
    val categoriesString: String, // Comma-separated categories
    val calculatedDdi: Float,
    val isSynced: Boolean = false
)
