package com.strath.finalpr0ject.data.model

data class CHWProfile(
    val chwId: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val county: String = "Turkana",
    val subCounty: String = "Kakuma West",
    val assignedChildrenCount: Int = 0,
    val activeHighRiskCount: Int = 0,
    val status: String = "Active", // "Active", "Pending Password Reset", "Inactive"
    val createdAt: Long = System.currentTimeMillis()
)
