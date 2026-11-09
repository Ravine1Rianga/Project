package com.strath.finalpr0ject.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.strath.finalpr0ject.data.model.UserProfile
import com.strath.finalpr0ject.data.model.UserRole

/**
 * UserProfileEntity
 * =================
 * Room entity that caches registered user profiles (CHWs, parents/guardians) locally.
 * This allows the app to authenticate and display user data completely offline
 * after the first successful internet-connected login.
 *
 * Indexed on email for fast offline lookup when a user signs in by email.
 */
@Entity(
    tableName = "user_profiles",
    indices = [Index(value = ["email"])]
)
data class UserProfileEntity(
    @PrimaryKey
    val uid: String,
    val email: String,
    val displayName: String,
    val role: String,                        // UserRole.name stored as String
    val phone: String = "",
    val county: String = "Turkana",
    val subCounty: String = "Turkana West",
    val ward: String = "",
    val chwId: String = "",
    val chwFacility: String = "",
    val supervisingFacility: String = "",
    val isVerified: Boolean = false,
    val verificationStatus: String = "pending",
    val assignedChildId: String = "",
    val assignedChildName: String = "",
    val profileCompleted: Boolean = false,
    val photoUrl: String = "",
    val photoLocalPath: String = "",         // Internal filesystem path for avatar
    val createdAt: Long = System.currentTimeMillis(),
    val lastSyncTimestamp: Long = 0L,        // Last time this record was synced with Firestore
    val pendingSync: Boolean = false         // True if local changes are not yet pushed to Firestore
) {
    fun toDomainModel(): UserProfile = UserProfile(
        uid = uid,
        email = email,
        displayName = displayName,
        role = try { UserRole.valueOf(role) } catch (_: Exception) { UserRole.PARENT },
        phone = phone,
        county = county,
        subCounty = subCounty,
        ward = ward,
        chwId = chwId,
        chwFacility = chwFacility,
        supervisingFacility = supervisingFacility,
        isVerified = isVerified,
        verificationStatus = verificationStatus,
        assignedChildId = assignedChildId,
        assignedChildName = assignedChildName,
        profileCompleted = profileCompleted,
        photoUrl = if (photoLocalPath.isNotBlank()) photoLocalPath else photoUrl,
        createdAt = createdAt
    )

    companion object {
        fun fromDomainModel(
            profile: UserProfile,
            photoLocalPath: String = "",
            pendingSync: Boolean = false
        ): UserProfileEntity = UserProfileEntity(
            uid = profile.uid,
            email = profile.email,
            displayName = profile.displayName,
            role = profile.role.name,
            phone = profile.phone,
            county = profile.county,
            subCounty = profile.subCounty,
            ward = profile.ward,
            chwId = profile.chwId,
            chwFacility = profile.chwFacility,
            supervisingFacility = profile.supervisingFacility,
            isVerified = profile.isVerified,
            verificationStatus = profile.verificationStatus,
            assignedChildId = profile.assignedChildId,
            assignedChildName = profile.assignedChildName,
            profileCompleted = profile.profileCompleted,
            photoUrl = profile.photoUrl,
            photoLocalPath = photoLocalPath,
            createdAt = profile.createdAt,
            lastSyncTimestamp = System.currentTimeMillis(),
            pendingSync = pendingSync
        )
    }
}
