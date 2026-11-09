package com.strath.finalpr0ject.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.strath.finalpr0ject.data.local.AppDatabase
import com.strath.finalpr0ject.data.local.SessionManager
import com.strath.finalpr0ject.data.local.entity.ChildEntity
import com.strath.finalpr0ject.data.local.entity.UserProfileEntity
import com.strath.finalpr0ject.data.model.ChildProfile
import com.strath.finalpr0ject.data.model.UserProfile
import com.strath.finalpr0ject.data.model.UserRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * OfflineRepository
 * =================
 * Coordinates Room as the single source of truth for both online and offline operation.
 * Reads return locally cached data immediately.
 * Writes persist to Room first, then attempt Firebase or let SyncWorker sync in background.
 */
class OfflineRepository(
    private val context: Context,
    private val database: AppDatabase = AppDatabase.getDatabase(context),
    private val sessionManager: SessionManager = SessionManager(context),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    // ---------------------------------------------------------
    // Session / Authentication Management
    // ---------------------------------------------------------

    fun getCachedSession(): UserProfile? = sessionManager.getSession()

    fun saveSession(profile: UserProfile) {
        sessionManager.saveSession(profile)
    }

    fun clearSession() {
        sessionManager.clearSession()
    }

    /**
     * Attempts offline sign in using Room cached profile.
     */
    suspend fun getProfileByEmailOffline(email: String): UserProfile? = withContext(Dispatchers.IO) {
        database.userProfileDao().getByEmail(email)?.toDomainModel()
    }

    suspend fun cacheUserProfile(profile: UserProfile, pendingSync: Boolean = false) = withContext(Dispatchers.IO) {
        val entity = UserProfileEntity.fromDomainModel(profile, pendingSync = pendingSync)
        database.userProfileDao().upsert(entity)
        sessionManager.saveSession(profile)
    }

    // ---------------------------------------------------------
    // Child Profile Operations
    // ---------------------------------------------------------

    suspend fun getChildProfile(childId: String): ChildProfile? = withContext(Dispatchers.IO) {
        val localChild = database.childDao().getChildById(childId)
        if (localChild != null) {
            return@withContext localChild.toDomainModel()
        }
        // Try fallback to Firestore if connected
        try {
            val doc = firestore.collection("children").document(childId).get().await()
            if (doc.exists()) {
                val profile = doc.toObject(ChildProfile::class.java)
                if (profile != null) {
                    database.childDao().insertOrUpdate(ChildEntity.fromDomainModel(profile, pendingSync = false))
                    return@withContext profile
                }
            }
        } catch (e: Exception) {
            Log.w("OfflineRepository", "Firestore fetch failed for child $childId: ${e.message}")
        }
        null
    }

    fun observeChildrenForChw(chwId: String): Flow<List<ChildProfile>> {
        return database.childDao().observeApprovedChildrenByChw(chwId).map { list ->
            list.map { it.toDomainModel() }
        }
    }

    fun observeAllChildrenForChw(chwId: String): Flow<List<ChildProfile>> {
        return database.childDao().observeChildrenByChw(chwId).map { list ->
            list.map { it.toDomainModel() }
        }
    }

    fun observePendingApprovalChildren(): Flow<List<ChildProfile>> {
        return database.childDao().observePendingApprovalChildren().map { list ->
            list.map { it.toDomainModel() }
        }
    }

    // ---------------------------------------------------------
    // Admin: Observe all registered users (live / reactive)
    // ---------------------------------------------------------

    fun observeAllCHWProfiles(): Flow<List<UserProfile>> {
        return database.userProfileDao().observeAllCHWs().map { list ->
            list.map { it.toDomainModel() }
        }
    }

    fun observeAllParentProfiles(): Flow<List<UserProfile>> {
        return database.userProfileDao().observeAllParents().map { list ->
            list.map { it.toDomainModel() }
        }
    }

    fun observeAllChildrenAdmin(): Flow<List<ChildProfile>> {
        return database.childDao().observeAllChildren().map { list ->
            list.map { it.toDomainModel() }
        }
    }

    suspend fun approveChildForTracking(childId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.childDao().approveChildForTracking(childId)
            try {
                firestore.collection("children").document(childId)
                    .set(mapOf("approvalStatus" to "approved", "isApprovedForTracking" to true), SetOptions.merge())
                    .addOnSuccessListener {
                        launchIfPossible {
                            database.childDao().markSynced(childId)
                        }
                    }
            } catch (e: Exception) {
                Log.d("OfflineRepository", "Approved child $childId offline, queued sync")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getChildForGuardian(guardianUid: String): ChildProfile? = withContext(Dispatchers.IO) {
        val local = database.childDao().getChildByGuardianUid(guardianUid)
        if (local != null) return@withContext local.toDomainModel()

        // Fallback to Firestore query
        try {
            val snapshot = firestore.collection("children")
                .whereEqualTo("guardianUid", guardianUid)
                .limit(1)
                .get()
                .await()
            if (!snapshot.isEmpty) {
                val profile = snapshot.documents.first().toObject(ChildProfile::class.java)
                if (profile != null) {
                    database.childDao().insertOrUpdate(ChildEntity.fromDomainModel(profile, pendingSync = false))
                    return@withContext profile
                }
            }
        } catch (e: Exception) {
            Log.w("OfflineRepository", "Firestore lookup for guardian child failed: ${e.message}")
        }
        null
    }

    suspend fun enrollChild(child: ChildProfile, guardianUid: String = ""): Result<ChildProfile> = withContext(Dispatchers.IO) {
        try {
            val id = if (child.childId.isNotBlank()) child.childId else "TK-${UUID.randomUUID().toString().take(6).uppercase()}"
            val finalChild = child.copy(childId = id, guardianUid = guardianUid.ifBlank { child.guardianUid })
            val entity = ChildEntity.fromDomainModel(finalChild, pendingSync = true)
            database.childDao().insertOrUpdate(entity)

            // Try optimistic Firestore write in background
            try {
                firestore.collection("children").document(id)
                    .set(finalChild, SetOptions.merge())
                    .addOnSuccessListener {
                        launchIfPossible {
                            database.childDao().markSynced(id)
                        }
                    }
            } catch (e: Exception) {
                Log.d("OfflineRepository", "Enrolled child $id offline, queued for sync: ${e.message}")
            }

            Result.success(finalChild)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateChildProfile(child: ChildProfile): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val entity = ChildEntity.fromDomainModel(child, pendingSync = true)
            database.childDao().insertOrUpdate(entity)

            try {
                firestore.collection("children").document(child.childId)
                    .set(child, SetOptions.merge())
                    .addOnSuccessListener {
                        launchIfPossible {
                            database.childDao().markSynced(child.childId)
                        }
                    }
            } catch (e: Exception) {
                Log.d("OfflineRepository", "Updated child ${child.childId} offline, queued sync")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ---------------------------------------------------------
    // Screening & Growth Monitoring Visits
    // ---------------------------------------------------------

    fun observeScreeningsForChild(childId: String): Flow<List<com.strath.finalpr0ject.data.local.entity.ScreeningEntity>> {
        return database.screeningDao().observeScreeningsForChildAsc(childId)
    }

    suspend fun getScreeningsForChild(childId: String): List<com.strath.finalpr0ject.data.local.entity.ScreeningEntity> = withContext(Dispatchers.IO) {
        database.screeningDao().getScreeningsForChildAsc(childId)
    }

    suspend fun recordMonthlyScreening(screening: com.strath.finalpr0ject.data.local.entity.ScreeningEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.screeningDao().insertScreening(screening.copy(isSynced = false))

            // Also update child's current HAZ, WHZ, MUAC and age
            val currentChild = database.childDao().getChildById(screening.childId)
            if (currentChild != null) {
                val updated = currentChild.copy(
                    ageMonths = screening.ageMonths,
                    currentHaz = screening.haz,
                    currentWhz = screening.whz,
                    currentMuac = screening.muac,
                    latestRiskTier = screening.stuntingRiskTier,
                    latestProbability = screening.predictedProbability,
                    lastScreeningDate = screening.timestamp,
                    pendingSync = true
                )
                database.childDao().insertOrUpdate(updated)
            }

            try {
                firestore.collection("screenings").document(screening.screeningId)
                    .set(screening, SetOptions.merge())
                    .addOnSuccessListener {
                        launchIfPossible {
                            database.screeningDao().markSynced(screening.screeningId)
                        }
                    }
            } catch (e: Exception) {
                Log.d("OfflineRepository", "Queued screening ${screening.screeningId} for offline sync")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ---------------------------------------------------------
    // Local Avatar / Image Storage
    // ---------------------------------------------------------

    suspend fun saveAvatarLocally(uri: Uri, uid: String): String = withContext(Dispatchers.IO) {
        try {
            val avatarsDir = File(context.filesDir, "avatars")
            if (!avatarsDir.exists()) avatarsDir.mkdirs()

            val avatarFile = File(avatarsDir, "avatar_${uid}.jpg")
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val bitmap = BitmapFactory.decodeStream(inputStream)
                FileOutputStream(avatarFile).use { outStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outStream)
                }
            }
            val absolutePath = avatarFile.absolutePath
            sessionManager.saveLocalPhotoPath(absolutePath)
            database.userProfileDao().updateLocalPhoto(uid, absolutePath)
            absolutePath
        } catch (e: Exception) {
            Log.e("OfflineRepository", "Failed saving local avatar", e)
            ""
        }
    }

    private fun launchIfPossible(block: suspend () -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try { block() } catch (_: Exception) {}
        }
    }
}
