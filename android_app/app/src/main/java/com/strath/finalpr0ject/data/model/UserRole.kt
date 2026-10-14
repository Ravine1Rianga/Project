package com.strath.finalpr0ject.data.model

enum class UserRole {
    ADMIN,
    CHW,
    CHILD
}

data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val role: UserRole = UserRole.CHW,
    val county: String = "Turkana",
    val assignedChwId: String = "",
    val assignedChildrenIds: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)
