package com.strath.finalpr0ject.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.strath.finalpr0ject.data.model.CHWProfile
import com.strath.finalpr0ject.data.model.ChildProfile
import com.strath.finalpr0ject.data.model.UserProfile
import com.strath.finalpr0ject.data.model.UserRole
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // 1. Authentication & Invitation Flow
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 2. Admin: Provision & Authorize CHW
    suspend fun adminCreateCHW(profile: CHWProfile): Result<Unit> {
        return try {
            val docId = profile.chwId.ifEmpty { "CHW-${System.currentTimeMillis() % 10000}" }
            val chwData = profile.copy(
                chwId = docId,
                status = "Pending Password Reset"
            )

            // Save to Firestore
            firestore.collection("chws").document(docId).set(chwData).await()

            // Also create User Record
            val userRecord = UserProfile(
                uid = docId,
                email = profile.email,
                displayName = profile.name,
                role = UserRole.CHW,
                county = profile.county
            )
            firestore.collection("users").document(docId).set(userRecord, SetOptions.merge()).await()

            // Trigger password setup email via Firebase Auth
            if (profile.email.isNotBlank()) {
                auth.sendPasswordResetEmail(profile.email).await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 3. Admin: Stream all CHWs
    fun getAllCHWs(): Flow<List<CHWProfile>> = callbackFlow {
        val listener = firestore.collection("chws")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { it.toObject(CHWProfile::class.java) } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    // 4. Admin & CHW: Stream all Children
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

    // 5. CHW: Stream Only Assigned Children
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

    // 6. Admin: Connect / Assign CHW to a Child
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

    // 7. Admin & CHW: Edit / Update Child Record
    suspend fun updateChildProfile(child: ChildProfile): Result<Unit> {
        return try {
            firestore.collection("children").document(child.childId).set(child, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 8. Register New Child (By CHW in field or Admin)
    suspend fun registerChild(child: ChildProfile): Result<Unit> {
        return try {
            val docId = child.childId.ifEmpty { "TK-${(1000..9999).random()}" }
            val record = child.copy(childId = docId)
            firestore.collection("children").document(docId).set(record).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
