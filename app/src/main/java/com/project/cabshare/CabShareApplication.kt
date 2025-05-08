package com.project.cabshare

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.FirebaseApp
import com.project.cabshare.data.FirebaseInitializer
import com.project.cabshare.workers.RideCleanupWorker
import java.util.concurrent.TimeUnit

class CabShareApplication : Application(), Configuration.Provider {
    companion object {
        private const val TAG = "CabShareApplication"
        private const val RIDE_CLEANUP_WORK = "ride_cleanup_work"
    }

    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase
        try {
            // First try standard initialization (with google-services.json)
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            // If that fails, try manual initialization
            Log.w(TAG, "Standard Firebase initialization failed, using manual initialization")
            FirebaseInitializer.initialize(this)
        }
        
        // WorkManager will be automatically initialized through the Configuration.Provider interface
        
        // Run immediate cleanup of old rides
        runImmediateCleanup()
        
        // Schedule ride cleanup work
        scheduleRideCleanup()
    }
    
    // Implement Configuration.Provider interface
    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setMinimumLoggingLevel(Log.INFO)
            .build()
    }
    
    private fun runImmediateCleanup() {
        // Create a one-time work request to clean up rides immediately
        val immediateCleanupRequest = OneTimeWorkRequestBuilder<RideCleanupWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        
        WorkManager.getInstance(this)
            .enqueue(immediateCleanupRequest)
    }
    
    private fun scheduleRideCleanup() {
        // Create a clean-up request that runs every 1 hour
        val cleanupRequest = PeriodicWorkRequestBuilder<RideCleanupWorker>(
            1, TimeUnit.HOURS
        )
        .setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        )
        .build()
        
        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork(
                RIDE_CLEANUP_WORK,
                ExistingPeriodicWorkPolicy.UPDATE,
                cleanupRequest
            )
    }
} 