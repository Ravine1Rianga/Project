package com.strathmore.afyaearly

import android.app.Application
import com.strathmore.afyaearly.data.local.AppDatabase
import com.strathmore.afyaearly.ml.ONNXInferenceEngine

class AfyaEarlyApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var mlEngine: ONNXInferenceEngine
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = AppDatabase.getInstance(this)
        mlEngine = ONNXInferenceEngine(this)
    }

    companion object {
        lateinit var instance: AfyaEarlyApp
            private set
    }
}
