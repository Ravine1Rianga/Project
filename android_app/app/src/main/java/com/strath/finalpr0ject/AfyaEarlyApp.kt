package com.strath.finalpr0ject

import android.app.Application
import com.google.firebase.FirebaseApp
import com.strath.finalpr0ject.data.local.AppDatabase
import com.strath.finalpr0ject.data.local.OfflineSyncManager
import com.strath.finalpr0ject.data.repository.FirebaseRepository
import com.strath.finalpr0ject.ml.ONNXInferenceEngine

class AfyaEarlyApp : Application() {

    lateinit var mlEngine: ONNXInferenceEngine
        private set

    lateinit var firebaseRepo: FirebaseRepository
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
        syncManager = OfflineSyncManager(this, database)
        mlEngine = ONNXInferenceEngine(this)
        firebaseRepo = FirebaseRepository()
    }

    companion object {
        lateinit var instance: AfyaEarlyApp
            private set
    }
}
