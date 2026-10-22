package com.strath.finalpr0ject.data.model

enum class UserRole {
    ADMIN,
    CHW,
    PARENT,
    GUARDIAN, // Backward compatibility alias
    CHILD     // Backward compatibility alias
}

enum class VerificationStatus {
    PENDING,
    APPROVED,
    REJECTED
}

data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val role: UserRole = UserRole.PARENT,
    val phone: String = "",
    val photoUrl: String = "",
    val county: String = "Turkana",
    val subCounty: String = "Turkana West",
    val ward: String = "Kakuma",
    val chwId: String = "",              // CHW ID / Community Unit
    val chwFacility: String = "",        // Supervising Health Facility
    val chwCode: String = "",
    val supervisingFacility: String = "",
    val numChildren: Int = 1,
    val consentGiven: Boolean = false,
    val consentTimestamp: Long = 0L,
    val verificationStatus: String = "pending", // "pending" for new CHWs, "approved" for verified
    val isVerified: Boolean = false,             // false for new CHWs until supervisor approval
    val assignedChwId: String = "",
    val assignedChildrenIds: List<String> = emptyList(),
    val assignedChildId: String = "",
    val assignedChildName: String = "",
    val profileCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
