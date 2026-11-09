package com.strath.finalpr0ject.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.strath.finalpr0ject.data.model.UserProfile
import com.strath.finalpr0ject.data.model.UserRole

/**
 * SessionManager
 * ==============
 * Persists the authenticated user's profile in AES-256 encrypted SharedPreferences
 * so the app remains fully functional offline after the first internet-connected login.
 *
 * Session lifetime: 30 days (CHW field cycle).
 * Encryption: AES256-GCM (keys stored in Android Keystore).
 */
class SessionManager(context: Context) {

    companion object {
        private const val PREFS_FILE = "afya_session_encrypted"
        private const val KEY_UID = "s_uid"
        private const val KEY_EMAIL = "s_email"
        private const val KEY_DISPLAY_NAME = "s_display_name"
        private const val KEY_ROLE = "s_role"
        private const val KEY_PHONE = "s_phone"
        private const val KEY_COUNTY = "s_county"
        private const val KEY_SUB_COUNTY = "s_sub_county"
        private const val KEY_WARD = "s_ward"
        private const val KEY_CHW_ID = "s_chw_id"
        private const val KEY_CHW_FACILITY = "s_chw_facility"
        private const val KEY_IS_VERIFIED = "s_is_verified"
        private const val KEY_VERIFICATION_STATUS = "s_verification_status"
        private const val KEY_ASSIGNED_CHILD_ID = "s_assigned_child_id"
        private const val KEY_ASSIGNED_CHILD_NAME = "s_assigned_child_name"
        private const val KEY_PROFILE_COMPLETED = "s_profile_completed"
        private const val KEY_PHOTO_URL = "s_photo_url"
        private const val KEY_PHOTO_LOCAL_PATH = "s_photo_local_path"
        private const val KEY_SAVED_AT = "s_saved_at"
        // Session validity period: 30 days in milliseconds
        private const val SESSION_TTL_MS = 30L * 24 * 60 * 60 * 1000L
    }

    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                PREFS_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Fallback to regular prefs if encryption fails (should not happen on modern devices)
            Log.e("SessionManager", "EncryptedSharedPreferences init failed, using plain prefs", e)
            context.getSharedPreferences(PREFS_FILE + "_plain", Context.MODE_PRIVATE)
        }
    }

    /** Save a UserProfile to the encrypted local session cache. */
    fun saveSession(profile: UserProfile) {
        prefs.edit().apply {
            putString(KEY_UID, profile.uid)
            putString(KEY_EMAIL, profile.email)
            putString(KEY_DISPLAY_NAME, profile.displayName)
            putString(KEY_ROLE, profile.role.name)
            putString(KEY_PHONE, profile.phone)
            putString(KEY_COUNTY, profile.county)
            putString(KEY_SUB_COUNTY, profile.subCounty)
            putString(KEY_WARD, profile.ward)
            putString(KEY_CHW_ID, profile.chwId)
            putString(KEY_CHW_FACILITY, profile.chwFacility)
            putBoolean(KEY_IS_VERIFIED, profile.isVerified)
            putString(KEY_VERIFICATION_STATUS, profile.verificationStatus)
            putString(KEY_ASSIGNED_CHILD_ID, profile.assignedChildId)
            putString(KEY_ASSIGNED_CHILD_NAME, profile.assignedChildName)
            putBoolean(KEY_PROFILE_COMPLETED, profile.profileCompleted)
            putString(KEY_PHOTO_URL, profile.photoUrl)
            putLong(KEY_SAVED_AT, System.currentTimeMillis())
        }.apply()
        Log.d("SessionManager", "Session saved for ${profile.email} (${profile.role})")
    }

    /** Save a local avatar photo path separately (updated after photo capture). */
    fun saveLocalPhotoPath(path: String) {
        prefs.edit().putString(KEY_PHOTO_LOCAL_PATH, path).apply()
    }

    /** Retrieve the cached session. Returns null if expired or not present. */
    fun getSession(): UserProfile? {
        val savedAt = prefs.getLong(KEY_SAVED_AT, 0L)
        if (savedAt == 0L) return null

        val ageMs = System.currentTimeMillis() - savedAt
        if (ageMs > SESSION_TTL_MS) {
            Log.d("SessionManager", "Session expired after ${ageMs / (1000 * 60 * 60 * 24)} days")
            return null
        }

        val uid = prefs.getString(KEY_UID, null) ?: return null
        val email = prefs.getString(KEY_EMAIL, "") ?: ""
        val roleName = prefs.getString(KEY_ROLE, UserRole.PARENT.name) ?: UserRole.PARENT.name
        val role = try { UserRole.valueOf(roleName) } catch (_: Exception) { UserRole.PARENT }

        return UserProfile(
            uid = uid,
            email = email,
            displayName = prefs.getString(KEY_DISPLAY_NAME, "") ?: "",
            role = role,
            phone = prefs.getString(KEY_PHONE, "") ?: "",
            county = prefs.getString(KEY_COUNTY, "Turkana") ?: "Turkana",
            subCounty = prefs.getString(KEY_SUB_COUNTY, "Turkana West") ?: "Turkana West",
            ward = prefs.getString(KEY_WARD, "") ?: "",
            chwId = prefs.getString(KEY_CHW_ID, "") ?: "",
            chwFacility = prefs.getString(KEY_CHW_FACILITY, "") ?: "",
            isVerified = prefs.getBoolean(KEY_IS_VERIFIED, false),
            verificationStatus = prefs.getString(KEY_VERIFICATION_STATUS, "pending") ?: "pending",
            assignedChildId = prefs.getString(KEY_ASSIGNED_CHILD_ID, "") ?: "",
            assignedChildName = prefs.getString(KEY_ASSIGNED_CHILD_NAME, "") ?: "",
            profileCompleted = prefs.getBoolean(KEY_PROFILE_COMPLETED, false),
            photoUrl = prefs.getString(KEY_PHOTO_URL, "") ?: ""
        )
    }

    /** Returns the locally stored avatar file path (for offline photo display). */
    fun getLocalPhotoPath(): String? = prefs.getString(KEY_PHOTO_LOCAL_PATH, null)

    /** Returns the cached user's role without deserializing the full profile. */
    fun getCachedRole(): UserRole? {
        val roleName = prefs.getString(KEY_ROLE, null) ?: return null
        return try { UserRole.valueOf(roleName) } catch (_: Exception) { null }
    }

    /** Returns the cached UID, used for Room queries offline. */
    fun getCachedUid(): String? = prefs.getString(KEY_UID, null)?.takeIf { it.isNotBlank() }

    /** Returns the cached email. */
    fun getCachedEmail(): String? = prefs.getString(KEY_EMAIL, null)?.takeIf { it.isNotBlank() }

    /** True if a valid (non-expired) session exists. */
    fun isSessionValid(): Boolean = getSession() != null

    /**
     * Update approval status in the cached session without a full re-login.
     * Called after SyncWorker fetches an updated approval status from Firestore.
     */
    fun markApproved() {
        prefs.edit()
            .putBoolean(KEY_IS_VERIFIED, true)
            .putString(KEY_VERIFICATION_STATUS, "approved")
            .putBoolean(KEY_PROFILE_COMPLETED, true)
            .apply()
        Log.d("SessionManager", "Session approval status updated to approved.")
    }

    /** Update the assigned child ID and name in the cached session. */
    fun updateChildLink(childId: String, childName: String) {
        prefs.edit()
            .putString(KEY_ASSIGNED_CHILD_ID, childId)
            .putString(KEY_ASSIGNED_CHILD_NAME, childName)
            .putBoolean(KEY_PROFILE_COMPLETED, true)
            .apply()
    }

    /** Clear the session on sign-out. */
    fun clearSession() {
        prefs.edit().clear().apply()
        Log.d("SessionManager", "Session cleared.")
    }
}
