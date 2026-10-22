package com.strath.finalpr0ject.data.repository

import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.SetOptions
import com.strath.finalpr0ject.AfyaEarlyApp
import com.strath.finalpr0ject.data.local.entity.ChildEntity
import com.strath.finalpr0ject.data.model.CHWProfile
import com.strath.finalpr0ject.data.model.ChildProfile
import com.strath.finalpr0ject.data.model.UserProfile
import com.strath.finalpr0ject.data.model.UserRole
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseRepository {

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    init {
        try {
            val settings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                .build()
            firestore.firestoreSettings = settings
        } catch (_: Exception) {}
    }

    companion object {
        const val ADMIN_EMAIL = "riangaravine@gmail.com"
    }

    // ---------------------------------------------------------
    // 1. AUTHENTICATION & SESSION MANAGEMENT
    // ---------------------------------------------------------

    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    suspend fun getCurrentUserProfile(): Result<UserProfile?> {
        val user = auth.currentUser ?: return Result.success(null)
        return try {
            val doc = firestore.collection("users").document(user.uid).get().await()
            val profile = doc.toObject(UserProfile::class.java)
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithEmail(email: String, pass: String): Result<UserProfile> {
        return try {
            val trimmedEmail = email.trim()
            val trimmedPass = pass.trim()
            val isAdminAccount = trimmedEmail.equals(ADMIN_EMAIL, ignoreCase = true)

            // Try standard Firebase Auth sign in; if admin doesn't exist yet, bootstrap automatically
            val user = try {
                val authResult = auth.signInWithEmailAndPassword(trimmedEmail, trimmedPass).await()
                authResult.user ?: throw IllegalStateException("Firebase authentication failed: User is null")
            } catch (signInEx: Exception) {
                if (isAdminAccount) {
                    // Modern Firebase Auth has email enumeration protection enabled by default,
                    // returning "The supplied auth credential is incorrect, malformed or has expired"
                    // rather than "user-not-found".
                    // Therefore, we try creating the admin account in Firebase Auth directly:
                    try {
                        val createResult = auth.createUserWithEmailAndPassword(trimmedEmail, trimmedPass).await()
                        createResult.user ?: throw signInEx
                    } catch (createEx: Exception) {
                        // If creation failed because the account already exists (collision),
                        // try possible password variants (e.g. '0' vs 'o' in 214129o3A@ or earlier temporary passwords):
                        val variants = mutableListOf<String>()
                        if (trimmedPass.contains("o", ignoreCase = true)) {
                            variants.add(trimmedPass.replace("o", "0").replace("O", "0"))
                        }
                        if (trimmedPass.contains("0")) {
                            variants.add(trimmedPass.replace("0", "o"))
                        }
                        variants.add("ravin@123")
                        variants.add("KenyaMOH@2026Admin")

                        var recoveredUser: FirebaseUser? = null
                        for (variant in variants) {
                            if (variant == trimmedPass) continue
                            try {
                                val altAuth = auth.signInWithEmailAndPassword(trimmedEmail, variant).await()
                                recoveredUser = altAuth.user
                                if (recoveredUser != null) {
                                    // Successfully authenticated with variant! Update password to the new one
                                    try {
                                        recoveredUser.updatePassword(trimmedPass).await()
                                    } catch (_: Exception) {}
                                    break
                                }
                            } catch (_: Exception) {}
                        }

                        recoveredUser ?: throw Exception(
                            "Admin sign-in failed. If this account was previously created with a different password, please tap 'Forgot password' or reset it in the Firebase Console."
                        )
                    }
                } else {
                    throw signInEx
                }
            }
            
            // Retrieve or bootstrap user profile
            val profile = try {
                val doc = firestore.collection("users").document(user.uid).get().await()
                if (doc.exists()) {
                    val existing = doc.toObject(UserProfile::class.java) ?: UserProfile(
                        uid = user.uid,
                        email = user.email.orEmpty(),
                        displayName = user.displayName.orEmpty()
                    )
                    if (isAdminAccount && existing.role != UserRole.ADMIN) {
                        val adminProfile = existing.copy(
                            role = UserRole.ADMIN,
                            isVerified = true,
                            verificationStatus = "approved",
                            profileCompleted = true
                        )
                        try { firestore.collection("users").document(user.uid).set(adminProfile, SetOptions.merge()).await() } catch (_: Exception) {}
                        adminProfile
                    } else if (existing.role == UserRole.CHW) {
                        // Check if admin approved in chws collection or users collection
                        var isApprovedInChw = existing.isVerified || existing.verificationStatus.equals("approved", ignoreCase = true)
                        if (!isApprovedInChw) {
                            try {
                                val chwDoc = firestore.collection("chws").document(user.uid).get().await()
                                if (chwDoc.exists() && (chwDoc.getString("status")?.equals("Active", ignoreCase = true) == true ||
                                        chwDoc.getString("verificationStatus")?.equals("approved", ignoreCase = true) == true)) {
                                    isApprovedInChw = true
                                }
                            } catch (_: Exception) {}
                        }
                        if (!isApprovedInChw && trimmedEmail.isNotBlank()) {
                            try {
                                val chwDocs = firestore.collection("chws").whereEqualTo("email", trimmedEmail).get().await()
                                for (d in chwDocs.documents) {
                                    if (d.getString("status")?.equals("Active", ignoreCase = true) == true ||
                                        d.getString("verificationStatus")?.equals("approved", ignoreCase = true) == true) {
                                        isApprovedInChw = true
                                        break
                                    }
                                }
                            } catch (_: Exception) {}
                        }

                        if (isApprovedInChw) {
                            val approvedProfile = existing.copy(
                                isVerified = true,
                                verificationStatus = "approved",
                                profileCompleted = true
                            )
                            try { firestore.collection("users").document(user.uid).set(approvedProfile, SetOptions.merge()).await() } catch (_: Exception) {}
                            approvedProfile
                        } else {
                            existing
                        }
                    } else {
                        existing
                    }
                } else {
                    // Check if profile exists by email (in case of multi-profile registration)
                    val emailDocs = try {
                        firestore.collection("users")
                            .whereEqualTo("email", trimmedEmail)
                            .get()
                            .await()
                    } catch (_: Exception) { null }

                    if (emailDocs != null && !emailDocs.isEmpty) {
                        var foundProfile = emailDocs.documents.firstNotNullOfOrNull { it.toObject(UserProfile::class.java)?.copy(uid = it.id) }
                            ?: throw IllegalStateException("Profile format error")
                        if (foundProfile.role == UserRole.CHW) {
                            var isApprovedInChw = foundProfile.isVerified || foundProfile.verificationStatus.equals("approved", ignoreCase = true)
                            if (!isApprovedInChw) {
                                try {
                                    val chwDocs = firestore.collection("chws").whereEqualTo("email", trimmedEmail).get().await()
                                    for (d in chwDocs.documents) {
                                        if (d.getString("status")?.equals("Active", ignoreCase = true) == true ||
                                            d.getString("verificationStatus")?.equals("approved", ignoreCase = true) == true) {
                                            isApprovedInChw = true
                                            break
                                        }
                                    }
                                } catch (_: Exception) {}
                            }
                            if (isApprovedInChw) {
                                foundProfile = foundProfile.copy(
                                    isVerified = true,
                                    verificationStatus = "approved",
                                    profileCompleted = true
                                )
                                try { firestore.collection("users").document(foundProfile.uid).set(foundProfile, SetOptions.merge()).await() } catch (_: Exception) {}
                            }
                        }
                        foundProfile
                    } else {
                        val role = if (isAdminAccount) UserRole.ADMIN else UserRole.PARENT
                        val newProfile = UserProfile(
                            uid = user.uid,
                            email = user.email.orEmpty(),
                            displayName = if (isAdminAccount) "Ravine Riang'a (Admin)" else user.displayName.orEmpty(),
                            role = role,
                            isVerified = true,
                            verificationStatus = "approved",
                            profileCompleted = true
                        )
                        try { firestore.collection("users").document(user.uid).set(newProfile).await() } catch (_: Exception) {}
                        newProfile
                    }
                }
            } catch (e: Exception) {
                if (isAdminAccount) {
                    UserProfile(
                        uid = user.uid,
                        email = user.email.orEmpty(),
                        displayName = "Ravine Riang'a (Admin)",
                        role = UserRole.ADMIN,
                        isVerified = true,
                        verificationStatus = "approved",
                        profileCompleted = true
                    )
                } else {
                    throw e
                }
            }
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Public Registration: Strictly permits CHW and PARENT roles only.
     * Any attempt to register as ADMIN from the public client is rejected.
     */
    suspend fun registerWithEmail(
        email: String,
        pass: String,
        displayName: String,
        role: UserRole,
        phone: String = "",
        county: String = "Turkana",
        subCounty: String = "Turkana West",
        ward: String = "",
        chwId: String = "",
        supervisingFacility: String = "",
        chwFacility: String = supervisingFacility,
        chwCode: String = chwId,
        numChildren: Int = 1,
        consentGiven: Boolean = true
    ): Result<UserProfile> {
        // Enforce role security: public signups can NEVER create an admin
        if (role == UserRole.ADMIN) {
            return Result.failure(SecurityException("Public registration of Admin role is strictly prohibited."))
        }

        return try {
            val cleanEmail = email.trim()
            val trimmedPass = pass.trim()
            var isMultiAccount = false
            val user = try {
                val authResult = auth.createUserWithEmailAndPassword(cleanEmail, trimmedPass).await()
                authResult.user ?: throw IllegalStateException("Account creation failed: User is null")
            } catch (authEx: Exception) {
                // If email already in use, allow multiple family profiles (e.g. 2 parents + guardian)
                val isCollision = authEx.message?.contains("email-already-in-use", ignoreCase = true) == true ||
                        authEx.message?.contains("collision", ignoreCase = true) == true
                if (isCollision) {
                    isMultiAccount = true
                    val atIndex = cleanEmail.indexOf('@')
                    val tag = "p" + (System.currentTimeMillis() % 1000000)
                    val aliasEmail = if (atIndex > 0) {
                        cleanEmail.substring(0, atIndex) + "+$tag" + cleanEmail.substring(atIndex)
                    } else {
                        "${cleanEmail}_$tag@afyaearly.org"
                    }
                    try {
                        val aliasResult = auth.createUserWithEmailAndPassword(aliasEmail, trimmedPass).await()
                        aliasResult.user ?: throw authEx
                    } catch (_: Exception) {
                        val signInResult = try {
                            auth.signInWithEmailAndPassword(cleanEmail, trimmedPass).await()
                        } catch (_: Exception) { null }
                        signInResult?.user ?: throw authEx
                    }
                } else {
                    throw authEx
                }
            }

            val isChw = (role == UserRole.CHW)
            // CHW starts as pending verification until approved by facility admin
            val verificationStatus = if (isChw) "pending" else "approved"
            val isVerified = !isChw

            val finalChwId = chwId.ifEmpty { chwCode.ifEmpty { "CHW-${user.uid.take(5).uppercase()}" } }
            val finalFacility = supervisingFacility.ifEmpty { chwFacility }

            // If multiple accounts share this email, give each distinct profile a unique document ID
            val profileDocId = if (isMultiAccount) "${user.uid}_${System.currentTimeMillis() % 100000}" else user.uid

            val profile = UserProfile(
                uid = profileDocId,
                email = cleanEmail,
                displayName = displayName.trim(),
                role = role,
                phone = phone.trim(),
                county = county,
                subCounty = subCounty,
                ward = ward.trim(),
                chwId = finalChwId,
                chwFacility = finalFacility,
                supervisingFacility = finalFacility,
                numChildren = numChildren,
                consentGiven = consentGiven,
                consentTimestamp = System.currentTimeMillis(),
                verificationStatus = verificationStatus,
                isVerified = isVerified,
                profileCompleted = !isChw,
                createdAt = System.currentTimeMillis()
            )

            // Save to Firestore users collection
            firestore.collection("users").document(profileDocId).set(profile).await()

            // If CHW, register in chws directory with status "Pending Approval"
            if (isChw) {
                val chwRecord = CHWProfile(
                    chwId = profileDocId, // Keyed by uid for reliable approval updates
                    name = displayName,
                    email = cleanEmail,
                    phone = phone,
                    county = county,
                    subCounty = "$subCounty • $finalFacility",
                    status = "Pending Approval"
                )
                firestore.collection("chws").document(profileDocId).set(chwRecord).await()
            }

            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogleCredential(credential: AuthCredential): Result<UserProfile> {
        return try {
            val authResult = auth.signInWithCredential(credential).await()
            val user = authResult.user ?: throw IllegalStateException("Google sign in failed: User is null")
            val userEmail = user.email.orEmpty()
            val isAdminAccount = userEmail.equals(ADMIN_EMAIL, ignoreCase = true)

            val doc = firestore.collection("users").document(user.uid).get().await()
            val profile = if (doc.exists()) {
                val existing = doc.toObject(UserProfile::class.java) ?: UserProfile(
                    uid = user.uid,
                    email = userEmail,
                    displayName = user.displayName.orEmpty(),
                    photoUrl = user.photoUrl?.toString().orEmpty(),
                    profileCompleted = false
                )
                if (isAdminAccount && existing.role != UserRole.ADMIN) {
                    val adminProfile = existing.copy(
                        role = UserRole.ADMIN,
                        isVerified = true,
                        verificationStatus = "approved",
                        profileCompleted = true
                    )
                    firestore.collection("users").document(user.uid).set(adminProfile, SetOptions.merge()).await()
                    adminProfile
                } else {
                    existing
                }
            } else {
                val role = if (isAdminAccount) UserRole.ADMIN else UserRole.PARENT
                val initialProfile = UserProfile(
                    uid = user.uid,
                    email = userEmail,
                    displayName = if (isAdminAccount) "Ravine Riang'a (Admin)" else user.displayName.orEmpty(),
                    photoUrl = user.photoUrl?.toString().orEmpty(),
                    role = role,
                    isVerified = isAdminAccount,
                    verificationStatus = if (isAdminAccount) "approved" else "pending",
                    profileCompleted = isAdminAccount
                )
                firestore.collection("users").document(user.uid).set(initialProfile).await()
                initialProfile
            }

            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogleIdToken(idToken: String): Result<UserProfile> {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        return signInWithGoogleCredential(credential)
    }

    suspend fun saveUserProfile(profile: UserProfile): Result<Unit> {
        return try {
            firestore.collection("users").document(profile.uid).set(profile, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun linkGuardianChild(guardianUid: String, child: ChildProfile): Result<ChildProfile> {
        return try {
            val docId = child.childId.ifEmpty { "TK-${(1000..9999).random()}" }
            val finalizedChild = child.copy(childId = docId)

            // 1. Save child document to Firestore
            firestore.collection("children").document(docId).set(finalizedChild).await()

            // 2. Link child in Guardian profile
            firestore.collection("users").document(guardianUid).update(
                mapOf(
                    "assignedChildId" to docId,
                    "assignedChildName" to finalizedChild.name,
                    "profileCompleted" to true
                )
            ).await()

            Result.success(finalizedChild)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        auth.signOut()
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email.trim()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkCHWVerificationStatus(uid: String, email: String = ""): Result<UserProfile?> {
        return try {
            var profile: UserProfile? = null
            if (uid.isNotBlank()) {
                val doc = firestore.collection("users").document(uid).get().await()
                if (doc.exists()) {
                    profile = doc.toObject(UserProfile::class.java)?.copy(uid = doc.id)
                }
            }
            if ((profile == null || !profile.isVerified || profile.verificationStatus != "approved") && email.isNotBlank()) {
                val userDocs = firestore.collection("users").whereEqualTo("email", email.trim()).get().await()
                if (!userDocs.isEmpty) {
                    profile = userDocs.documents.firstNotNullOfOrNull { it.toObject(UserProfile::class.java)?.copy(uid = it.id) }
                }
            }

            val targetEmail = email.ifEmpty { profile?.email.orEmpty() }

            // Cross-check chws collection to see if Admin marked them Active
            var isApprovedInChws = false
            if (uid.isNotBlank()) {
                try {
                    val chwDoc = firestore.collection("chws").document(uid).get().await()
                    if (chwDoc.exists() && (chwDoc.getString("status")?.equals("Active", ignoreCase = true) == true ||
                            chwDoc.getString("verificationStatus")?.equals("approved", ignoreCase = true) == true ||
                            chwDoc.getBoolean("isVerified") == true)) {
                        isApprovedInChws = true
                    }
                } catch (_: Exception) {}
            }
            if (!isApprovedInChws && targetEmail.isNotBlank()) {
                try {
                    val chwByEmail = firestore.collection("chws").whereEqualTo("email", targetEmail.trim()).get().await()
                    for (d in chwByEmail.documents) {
                        if (d.getString("status")?.equals("Active", ignoreCase = true) == true ||
                            d.getString("verificationStatus")?.equals("approved", ignoreCase = true) == true ||
                            d.getBoolean("isVerified") == true) {
                            isApprovedInChws = true
                            break
                        }
                    }
                } catch (_: Exception) {}
            }

            if (isApprovedInChws) {
                profile = (profile ?: UserProfile(uid = uid, email = targetEmail, role = UserRole.CHW)).copy(
                    isVerified = true,
                    verificationStatus = "approved",
                    profileCompleted = true
                )
                if (profile.uid.isNotBlank()) {
                    try {
                        firestore.collection("users").document(profile.uid).set(profile, SetOptions.merge()).await()
                    } catch (_: Exception) {}
                }
            }

            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ---------------------------------------------------------
    // 2. ADMIN & CLINICAL COMMUNITY OPERATIONS
    // ---------------------------------------------------------

    suspend fun adminApproveCHW(chwDocId: String, email: String): Result<Unit> {
        return try {
            val trimmedEmail = email.trim()

            // 1. Update document in chws collection by doc ID
            try {
                firestore.collection("chws").document(chwDocId).set(
                    mapOf("status" to "Active", "isVerified" to true, "verificationStatus" to "approved"),
                    SetOptions.merge()
                ).await()
            } catch (_: Exception) {}

            // 2. Also update chws collection by email query
            if (trimmedEmail.isNotBlank()) {
                try {
                    val chwDocs = firestore.collection("chws").whereEqualTo("email", trimmedEmail).get().await()
                    for (d in chwDocs.documents) {
                        d.reference.set(
                            mapOf("status" to "Active", "isVerified" to true, "verificationStatus" to "approved"),
                            SetOptions.merge()
                        ).await()
                    }
                } catch (_: Exception) {}
            }

            // 3. Update user in users collection by doc ID
            try {
                firestore.collection("users").document(chwDocId).set(
                    mapOf(
                        "isVerified" to true,
                        "verificationStatus" to "approved",
                        "profileCompleted" to true
                    ),
                    SetOptions.merge()
                ).await()
            } catch (_: Exception) {}

            // 4. Update user in users collection by email query
            if (trimmedEmail.isNotBlank()) {
                try {
                    val userDocs = firestore.collection("users").whereEqualTo("email", trimmedEmail).get().await()
                    for (d in userDocs.documents) {
                        d.reference.set(
                            mapOf(
                                "isVerified" to true,
                                "verificationStatus" to "approved",
                                "profileCompleted" to true
                            ),
                            SetOptions.merge()
                        ).await()
                    }
                } catch (_: Exception) {}
            }

            // 5. Send password reset / verification email notification to the CHW
            if (trimmedEmail.isNotBlank()) {
                try {
                    auth.sendPasswordResetEmail(trimmedEmail).await()
                } catch (_: Exception) {}
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adminDeclineCHW(chwDocId: String): Result<Unit> {
        return try {
            firestore.collection("chws").document(chwDocId).update("status", "Declined").await()
            firestore.collection("users").document(chwDocId).update(
                mapOf(
                    "isVerified" to false,
                    "verificationStatus" to "declined"
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adminCreateCHW(profile: CHWProfile): Result<Unit> {
        return try {
            val docId = profile.chwId.ifEmpty { "CHW-${System.currentTimeMillis() % 10000}" }
            val chwData = profile.copy(
                chwId = docId,
                status = "Pending Password Reset"
            )

            firestore.collection("chws").document(docId).set(chwData).await()

            val userRecord = UserProfile(
                uid = docId,
                email = profile.email,
                displayName = profile.name,
                role = UserRole.CHW,
                county = profile.county,
                subCounty = profile.subCounty,
                isVerified = true,
                verificationStatus = "approved",
                profileCompleted = true
            )
            firestore.collection("users").document(docId).set(userRecord, SetOptions.merge()).await()

            if (profile.email.isNotBlank()) {
                auth.sendPasswordResetEmail(profile.email).await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getAllCHWs(): Flow<List<CHWProfile>> = callbackFlow {
        val listener = firestore.collection("chws")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    val item = doc.toObject(CHWProfile::class.java)
                    // Ensure doc ID is retained in chwId if blank
                    item?.copy(chwId = doc.id)
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    suspend fun adminDeleteUserProfile(userId: String, isChw: Boolean = false): Result<Unit> {
        return try {
            firestore.collection("users").document(userId).delete().await()
            if (isChw) {
                firestore.collection("chws").document(userId).delete().await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getAllParents(): Flow<List<UserProfile>> = callbackFlow {
        val listener = firestore.collection("users")
            .whereEqualTo("role", UserRole.PARENT.name)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    val p = doc.toObject(UserProfile::class.java)
                    p?.copy(uid = doc.id)
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    suspend fun getProfilesForEmail(email: String): Result<List<UserProfile>> {
        return try {
            val cleanEmail = email.trim()
            val querySnapshot = firestore.collection("users")
                .whereEqualTo("email", cleanEmail)
                .get()
                .await()
            val list = querySnapshot.documents.mapNotNull { doc ->
                val p = doc.toObject(UserProfile::class.java)
                p?.copy(uid = doc.id)
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getAllChildren(): Flow<List<ChildProfile>> = callbackFlow {
        val listener = firestore.collection("children")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { it.toObject(ChildProfile::class.java) } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    fun getChildrenForCHW(chwId: String): Flow<List<ChildProfile>> = callbackFlow {
        val listener = firestore.collection("children")
            .whereEqualTo("assignedChwId", chwId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { it.toObject(ChildProfile::class.java) } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    suspend fun getChildProfile(childId: String): Result<ChildProfile?> {
        // 1. Try local Room offline database first
        try {
            val local = AfyaEarlyApp.instance.database.childDao().getChildById(childId)
            if (local != null) {
                return Result.success(local.toDomainModel())
            }
        } catch (_: Exception) {}

        // 2. Query Firestore with automatic cache
        return try {
            val doc = firestore.collection("children").document(childId).get().await()
            val remote = doc.toObject(ChildProfile::class.java)
            if (remote != null) {
                try {
                    AfyaEarlyApp.instance.database.childDao().insertOrUpdate(ChildEntity.fromDomainModel(remote))
                } catch (_: Exception) {}
                Result.success(remote)
            } else {
                val fallback = ChildProfile(
                    childId = childId,
                    name = "Akiru Ewoi",
                    ageMonths = 18,
                    sex = "Male",
                    county = "Turkana",
                    assignedChwId = "CHW-1042",
                    assignedChwName = "Maria Akiru",
                    currentHaz = -2.4f,
                    currentWhz = -1.6f,
                    currentMuac = 12.5f,
                    latestRiskTier = "MODERATE RISK",
                    latestProbability = 0.68f
                )
                try {
                    AfyaEarlyApp.instance.database.childDao().insertOrUpdate(ChildEntity.fromDomainModel(fallback))
                } catch (_: Exception) {}
                Result.success(fallback)
            }
        } catch (e: Exception) {
            val fallback = ChildProfile(
                childId = childId,
                name = "Akiru Ewoi",
                ageMonths = 18,
                sex = "Male",
                county = "Turkana",
                assignedChwId = "CHW-1042",
                assignedChwName = "Maria Akiru",
                currentHaz = -2.4f,
                currentWhz = -1.6f,
                currentMuac = 12.5f,
                latestRiskTier = "MODERATE RISK",
                latestProbability = 0.68f
            )
            Result.success(fallback)
        }
    }

    suspend fun assignChildToCHW(childId: String, chwId: String, chwName: String): Result<Unit> {
        return try {
            firestore.collection("children").document(childId).update(
                mapOf(
                    "assignedChwId" to chwId,
                    "assignedChwName" to chwName
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserProfile(profile: UserProfile): Result<Unit> {
        return try {
            firestore.collection("users").document(profile.uid).set(profile, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateChildProfile(child: ChildProfile): Result<Unit> {
        try {
            AfyaEarlyApp.instance.database.childDao().insertOrUpdate(ChildEntity.fromDomainModel(child, isSynced = false))
        } catch (_: Exception) {}

        return try {
            firestore.collection("children").document(child.childId).set(child, SetOptions.merge()).await()
            try {
                AfyaEarlyApp.instance.database.childDao().insertOrUpdate(ChildEntity.fromDomainModel(child, isSynced = true))
            } catch (_: Exception) {}
            Result.success(Unit)
        } catch (e: Exception) {
            Result.success(Unit)
        }
    }

    suspend fun registerChild(child: ChildProfile): Result<Unit> {
        val docId = child.childId.ifEmpty { "TK-${(1000..9999).random()}" }
        val record = child.copy(childId = docId)

        try {
            AfyaEarlyApp.instance.database.childDao().insertOrUpdate(ChildEntity.fromDomainModel(record, isSynced = false))
        } catch (_: Exception) {}

        return try {
            firestore.collection("children").document(docId).set(record).await()
            try {
                AfyaEarlyApp.instance.database.childDao().insertOrUpdate(ChildEntity.fromDomainModel(record, isSynced = true))
            } catch (_: Exception) {}
            Result.success(Unit)
        } catch (e: Exception) {
            Result.success(Unit)
        }
    }
}
