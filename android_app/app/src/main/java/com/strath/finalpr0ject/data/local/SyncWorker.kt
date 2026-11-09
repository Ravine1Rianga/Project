package com.strath.finalpr0ject.data.local

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/**
 * SyncWorker
 * ==========
 * Background worker scheduled via WorkManager.
 * Triggers automatically when network connectivity is established.
 * Uploads offline pending entities (screenings, nutrition, children, profiles)
 * and pulls down updated verification statuses from Firestore.
 */
class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val database = AppDatabase.getDatabase(appContext)
    private val firestore = FirebaseFirestore.getInstance()

    override suspend fun doWork(): Result {
        Log.d("SyncWorker", "Starting background sync job...")

        var hadErrors = false

        // 1. Sync pending children enrolled offline
        try {
            val pendingChildren = database.childDao().getPendingSync()
            Log.d("SyncWorker", "Found ${pendingChildren.size} pending children to sync")
            for (child in pendingChildren) {
                try {
                    val childMap = mapOf(
                        "childId" to child.childId,
                        "name" to child.name,
                        "ageMonths" to child.ageMonths,
                        "sex" to child.sex,
                        "county" to child.county,
                        "assignedChwId" to child.assignedChwId,
                        "assignedChwName" to child.assignedChwName,
                        "maternalEducation" to child.maternalEducation,
                        "waterSource" to child.waterSource,
                        "householdSize" to child.householdSize,
                        "currentHaz" to child.currentHaz,
                        "currentWhz" to child.currentWhz,
                        "currentMuac" to child.currentMuac,
                        "latestRiskTier" to child.latestRiskTier,
                        "latestProbability" to child.latestProbability,
                        "lastScreeningDate" to child.lastScreeningDate,
                        "motorMilestoneScore" to child.motorMilestoneScore,
                        "cognitiveMilestoneScore" to child.cognitiveMilestoneScore,
                        "languageMilestoneScore" to child.languageMilestoneScore,
                        "guardianUid" to child.guardianUid,
                        "approvalStatus" to child.approvalStatus,
                        "isApprovedForTracking" to child.isApprovedForTracking
                    )
                    firestore.collection("children").document(child.childId)
                        .set(childMap, SetOptions.merge()).await()
                    database.childDao().markSynced(child.childId)
                    Log.d("SyncWorker", "Successfully synced child ${child.childId}")
                } catch (e: Exception) {
                    Log.e("SyncWorker", "Error syncing child ${child.childId}", e)
                    hadErrors = true
                }
            }
        } catch (e: Exception) {
            Log.e("SyncWorker", "Error querying pending children", e)
            hadErrors = true
        }

        // 2. Sync pending user profiles
        try {
            val pendingProfiles = database.userProfileDao().getPendingSync()
            Log.d("SyncWorker", "Found ${pendingProfiles.size} pending user profiles to sync")
            for (p in pendingProfiles) {
                try {
                    val profileMap = mapOf(
                        "uid" to p.uid,
                        "email" to p.email,
                        "displayName" to p.displayName,
                        "role" to p.role,
                        "phone" to p.phone,
                        "county" to p.county,
                        "subCounty" to p.subCounty,
                        "ward" to p.ward,
                        "chwId" to p.chwId,
                        "chwFacility" to p.chwFacility,
                        "supervisingFacility" to p.supervisingFacility,
                        "isVerified" to p.isVerified,
                        "verificationStatus" to p.verificationStatus,
                        "assignedChildId" to p.assignedChildId,
                        "assignedChildName" to p.assignedChildName,
                        "profileCompleted" to p.profileCompleted,
                        "photoUrl" to p.photoUrl
                    )
                    firestore.collection("users").document(p.uid)
                        .set(profileMap, SetOptions.merge()).await()
                    database.userProfileDao().markSynced(p.uid)
                    Log.d("SyncWorker", "Successfully synced profile for ${p.email}")
                } catch (e: Exception) {
                    Log.e("SyncWorker", "Error syncing profile ${p.uid}", e)
                    hadErrors = true
                }
            }
        } catch (e: Exception) {
            Log.e("SyncWorker", "Error querying pending profiles", e)
            hadErrors = true
        }

        // 3. Sync pending screenings
        try {
            val unsyncedScreenings = database.screeningDao().getUnsyncedScreenings()
            for (s in unsyncedScreenings) {
                try {
                    val screeningMap = mapOf(
                        "screeningId" to s.screeningId,
                        "childId" to s.childId,
                        "timestamp" to s.timestamp,
                        "ageMonths" to s.ageMonths,
                        "weightKg" to s.weightKg,
                        "heightCm" to s.heightCm,
                        "haz" to s.haz,
                        "whz" to s.whz,
                        "muac" to s.muac,
                        "stuntingRiskTier" to s.stuntingRiskTier,
                        "predictedProbability" to s.predictedProbability,
                        "clinicalNotes" to s.clinicalNotes,
                        "feedingCategory" to s.feedingCategory
                    )
                    firestore.collection("screenings").document(s.screeningId)
                        .set(screeningMap, SetOptions.merge()).await()
                    database.screeningDao().markSynced(s.screeningId)
                    Log.d("SyncWorker", "Synced screening ${s.screeningId}")
                } catch (e: Exception) {
                    Log.w("SyncWorker", "Failed to sync screening ${s.screeningId}", e)
                    hadErrors = true
                }
            }
        } catch (e: Exception) {
            Log.e("SyncWorker", "Error syncing screenings", e)
            hadErrors = true
        }

        // 4. Sync pending meals
        try {
            val unsyncedMeals = database.nutritionDao().getUnsyncedMeals()
            for (m in unsyncedMeals) {
                try {
                    firestore.collection("nutrition_logs").document(m.logId)
                        .set(m, SetOptions.merge()).await()
                    database.nutritionDao().markSynced(m.logId)
                    Log.d("SyncWorker", "Synced meal log ${m.logId}")
                } catch (e: Exception) {
                    Log.w("SyncWorker", "Failed to sync meal log ${m.logId}", e)
                    hadErrors = true
                }
            }
        } catch (e: Exception) {
            Log.e("SyncWorker", "Error syncing nutrition", e)
            hadErrors = true
        }

        // 5. Pull down updated verification status for cached CHWs
        try {
            val cachedChws = database.userProfileDao().getAllCHWs()
            for (chw in cachedChws) {
                if (chw.verificationStatus != "approved") {
                    try {
                        val doc = firestore.collection("users").document(chw.uid).get().await()
                        if (doc.exists()) {
                            val status = doc.getString("verificationStatus") ?: "pending"
                            val isVerified = doc.getBoolean("isVerified") ?: false
                            if (status == "approved" || isVerified) {
                                database.userProfileDao().markApproved(chw.uid)
                                Log.d("SyncWorker", "CHW ${chw.uid} remote approval cached to Room")
                            }
                        }
                    } catch (e: Exception) {
                        Log.w("SyncWorker", "Failed to fetch remote status for CHW ${chw.uid}", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SyncWorker", "Error refreshing CHW verification statuses", e)
        }

        return if (hadErrors) Result.retry() else Result.success()
    }
}
