package com.strath.finalpr0ject

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.FirebaseApp
import com.strath.finalpr0ject.data.local.AppDatabase
import com.strath.finalpr0ject.data.local.OfflineSyncManager
import com.strath.finalpr0ject.data.local.SessionManager
import com.strath.finalpr0ject.data.local.SyncWorker
import com.strath.finalpr0ject.data.repository.FirebaseRepository
import com.strath.finalpr0ject.data.repository.OfflineRepository
import com.strath.finalpr0ject.ml.ONNXInferenceEngine
import java.util.concurrent.TimeUnit

class AfyaEarlyApp : Application() {

    lateinit var mlEngine: ONNXInferenceEngine
        private set

    lateinit var firebaseRepo: FirebaseRepository
        private set

    lateinit var offlineRepo: OfflineRepository
        private set

    lateinit var sessionManager: SessionManager
        private set

    lateinit var database: AppDatabase
        private set

    lateinit var syncManager: OfflineSyncManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        FirebaseApp.initializeApp(this)
        database = AppDatabase.getDatabase(this)
        sessionManager = SessionManager(this)
        offlineRepo = OfflineRepository(this, database, sessionManager)
        syncManager = OfflineSyncManager(this, database)
        mlEngine = ONNXInferenceEngine(this)
        firebaseRepo = FirebaseRepository()

        setupBackgroundSync()
    }

    private fun setupBackgroundSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(6, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "afya_offline_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }

    companion object {
        lateinit var instance: AfyaEarlyApp
            private set
    }
}
