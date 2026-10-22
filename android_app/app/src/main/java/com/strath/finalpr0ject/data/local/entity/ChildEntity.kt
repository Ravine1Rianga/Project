package com.strath.finalpr0ject.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.strath.finalpr0ject.data.model.ChildProfile

@Entity(tableName = "children")
data class ChildEntity(
    @PrimaryKey
    val childId: String,
    val name: String,
    val ageMonths: Int,
    val sex: String,
    val county: String,
    val assignedChwId: String,
    val assignedChwName: String,
    val maternalEducation: String,
    val waterSource: String,
    val householdSize: Int,
    val currentHaz: Float,
    val currentWhz: Float,
    val currentMuac: Float,
    val latestRiskTier: String,
    val latestProbability: Float,
    val lastScreeningDate: Long,
    val motorMilestoneScore: Int,
    val cognitiveMilestoneScore: Int,
    val languageMilestoneScore: Int,
    val isSynced: Boolean = true
) {
    fun toDomainModel(): ChildProfile = ChildProfile(
        childId = childId,
        name = name,
        ageMonths = ageMonths,
        sex = sex,
        county = county,
        assignedChwId = assignedChwId,
        assignedChwName = assignedChwName,
        maternalEducation = maternalEducation,
        waterSource = waterSource,
        householdSize = householdSize,
        currentHaz = currentHaz,
        currentWhz = currentWhz,
        currentMuac = currentMuac,
        latestRiskTier = latestRiskTier,
        latestProbability = latestProbability,
        lastScreeningDate = lastScreeningDate,
        motorMilestoneScore = motorMilestoneScore,
        cognitiveMilestoneScore = cognitiveMilestoneScore,
        languageMilestoneScore = languageMilestoneScore
    )

    companion object {
        fun fromDomainModel(child: ChildProfile, isSynced: Boolean = true): ChildEntity = ChildEntity(
            childId = child.childId,
            name = child.name,
            ageMonths = child.ageMonths,
            sex = child.sex,
            county = child.county,
            assignedChwId = child.assignedChwId,
            assignedChwName = child.assignedChwName,
            maternalEducation = child.maternalEducation,
            waterSource = child.waterSource,
            householdSize = child.householdSize,
            currentHaz = child.currentHaz,
            currentWhz = child.currentWhz,
            currentMuac = child.currentMuac,
            latestRiskTier = child.latestRiskTier,
            latestProbability = child.latestProbability,
            lastScreeningDate = child.lastScreeningDate,
            motorMilestoneScore = child.motorMilestoneScore,
            cognitiveMilestoneScore = child.cognitiveMilestoneScore,
            languageMilestoneScore = child.languageMilestoneScore,
            isSynced = isSynced
        )
    }
}
