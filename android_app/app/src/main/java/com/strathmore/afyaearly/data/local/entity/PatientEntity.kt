package com.strathmore.afyaearly.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "patients")
data class PatientEntity(
    @PrimaryKey
    val childId: String,
    val name: String,
    val ageMonths: Int,
    val sex: String, // "Male" or "Female"
    val county: String, // "Turkana", "Marsabit", "Wajir", "Mandera"
    val maternalEducation: String, // "None", "Primary", "Secondary", "Higher"
    val waterSource: String, // "Improved", "Unimproved"
    val householdSize: Int = 6,
    val createdAt: Long = System.currentTimeMillis()
)
