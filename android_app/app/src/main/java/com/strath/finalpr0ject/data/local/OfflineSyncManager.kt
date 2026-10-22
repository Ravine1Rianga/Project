package com.strath.finalpr0ject.data.local

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.gson.Gson
import com.strath.finalpr0ject.data.local.entity.ChildEntity
import com.strath.finalpr0ject.data.local.entity.NutritionEntity
import com.strath.finalpr0ject.data.local.entity.ScreeningEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class OfflineSyncManager(
    private val context: Context,
    private val database: AppDatabase = AppDatabase.getDatabase(context),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val gson = Gson()

    private val _isOnline = MutableStateFlow(checkInitialConnectivity())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    init {
        registerNetworkCallback()
    }

    private fun checkInitialConnectivity(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun registerNetworkCallback() {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _isOnline.value = true
                Log.d("OfflineSyncManager", "Network connection established. Triggering sync.")
                syncPendingItems()
            }

            override fun onLost(network: Network) {
                _isOnline.value = false
                Log.d("OfflineSyncManager", "Network lost. Switching to offline-only mode.")
            }
        })
    }

    fun syncPendingItems() {
        if (!_isOnline.value || _isSyncing.value) return

        coroutineScope.launch {
            _isSyncing.value = true
            try {
                // 1. Sync pending screenings
                val unsyncedScreenings = database.screeningDao().getUnsyncedScreenings()
                for (s in unsyncedScreenings) {
                    try {
                        firestore.collection("screenings").document(s.screeningId).set(s, SetOptions.merge()).await()
                        database.screeningDao().markSynced(s.screeningId)
                        Log.d("OfflineSyncManager", "Synced screening ${s.screeningId}")
                    } catch (e: Exception) {
                        Log.w("OfflineSyncManager", "Failed to sync screening ${s.screeningId}", e)
                    }
                }

                // 2. Sync pending meals
                val unsyncedMeals = database.nutritionDao().getUnsyncedMeals()
                for (m in unsyncedMeals) {
                    try {
                        firestore.collection("nutrition_logs").document(m.logId).set(m, SetOptions.merge()).await()
                        database.nutritionDao().markSynced(m.logId)
                        Log.d("OfflineSyncManager", "Synced meal log ${m.logId}")
                    } catch (e: Exception) {
                        Log.w("OfflineSyncManager", "Failed to sync meal log ${m.logId}", e)
                    }
                }

                // 3. Sync queued generic actions
                val queueItems = database.syncQueueDao().getPendingItems()
                for (item in queueItems) {
                    try {
                        val map = gson.fromJson<Map<String, Any>>(item.payloadJson, Map::class.java)
                        firestore.collection(item.collection).document(item.documentId).set(map, SetOptions.merge()).await()
                        database.syncQueueDao().delete(item)
                    } catch (e: Exception) {
                        database.syncQueueDao().incrementRetry(item.id)
                    }
                }
            } catch (e: Exception) {
                Log.e("OfflineSyncManager", "Sync cycle encountered an error", e)
            } finally {
                _isSyncing.value = false
            }
        }
    }
}
