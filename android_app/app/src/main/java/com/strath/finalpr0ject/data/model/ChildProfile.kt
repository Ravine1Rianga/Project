package com.strath.finalpr0ject.data.model

data class ChildProfile(
    val childId: String = "",
    val name: String = "",
    val ageMonths: Int = 12,
    val sex: String = "Male",
    val county: String = "Turkana",
    val assignedChwId: String = "",
    val assignedChwName: String = "Unassigned",
    val maternalEducation: String = "None",
    val waterSource: String = "Unimproved",
    val householdSize: Int = 6,
    val currentHaz: Float = 0.0f,
    val currentWhz: Float = 0.0f,
    val currentMuac: Float = 12.5f,
    val latestRiskTier: String = "LOW RISK", // "LOW RISK", "MODERATE RISK", "HIGH RISK"
    val latestProbability: Float = 0.15f,
    val lastScreeningDate: Long = System.currentTimeMillis(),
    val motorMilestoneScore: Int = 85, // 0 - 100 for 3D visualizer
    val cognitiveMilestoneScore: Int = 80,
    val languageMilestoneScore: Int = 75
)
