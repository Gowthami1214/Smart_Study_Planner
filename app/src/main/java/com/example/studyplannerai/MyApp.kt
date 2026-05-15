package com.example.studyplannerai

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.*
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class MyApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        Log.i(TAG, "Firebase initialized. App Check disabled for Free tier.")

        // Schedule daily carry-forward worker
        scheduleDailyCarryForward()
    }

    private fun scheduleDailyCarryForward() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val dailyWork = PeriodicWorkRequestBuilder<com.example.studyplannerai.reminder.DailyCarryForwardWorker>(
            1, TimeUnit.DAYS
        )
            .setConstraints(constraints)
            .setInitialDelay(1, TimeUnit.HOURS) // First run 1 hour after install
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "daily_carry_forward",
            ExistingPeriodicWorkPolicy.KEEP,
            dailyWork
        )
        Log.i(TAG, "Daily carry-forward worker scheduled")
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    private companion object {
        const val TAG = "MyApp"
    }
}
