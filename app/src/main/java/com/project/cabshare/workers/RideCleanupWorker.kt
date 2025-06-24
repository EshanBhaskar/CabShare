package com.project.cabshare.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.project.cabshare.data.FirestoreRideRepositoryImpl
import com.project.cabshare.data.RideRepository
import com.project.cabshare.data.FirestoreRideHistoryRepositoryImpl
import com.project.cabshare.data.RideHistoryRepository
import com.project.cabshare.models.RideHistory
import com.project.cabshare.models.RideCompletionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Date

class RideCleanupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    private val rideRepository: RideRepository = FirestoreRideRepositoryImpl()
    private val historyRepository: RideHistoryRepository = FirestoreRideHistoryRepositoryImpl()
    private val TAG = "RideCleanupWorker"
    
    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting ride cleanup process")
        return withContext(Dispatchers.IO) {
            try {
                val now = Date()
                Log.d(TAG, "Current time: $now")
                
                // Add a small buffer (15 minutes in the past) to avoid race conditions
                val calendar = Calendar.getInstance()
                calendar.time = now
                calendar.add(Calendar.MINUTE, -15)
                val bufferTime = calendar.time
                
                Log.d(TAG, "Fetching rides before: $bufferTime")
                val oldRides = rideRepository.getRidesBeforeDate(bufferTime)
                
                if (oldRides.isEmpty()) {
                    Log.d(TAG, "No old rides found to delete")
                    return@withContext Result.success()
                }
                
                Log.d(TAG, "Found ${oldRides.size} old rides to delete")
                var successCount = 0
                var failureCount = 0
                
                for (ride in oldRides) {
                    try {
                        // First, save the ride to history
                        val rideHistory = RideHistory(
                            rideId = ride.rideId,
                            source = ride.source,
                            destination = ride.destination,
                            dateTime = ride.dateTime,
                            maxPassengers = ride.maxPassengers,
                            creator = ride.creator,
                            creatorEmail = ride.creatorEmail,
                            direction = ride.direction,
                            notes = ride.notes,
                            passengers = ride.passengers,
                            trainNumber = ride.trainNumber,
                            trainName = ride.trainName,
                            flightNumber = ride.flightNumber,
                            flightName = ride.flightName,
                            completionStatus = RideCompletionStatus.EXPIRED,
                            completedAt = now
                        )
                        
                        Log.d(TAG, "Saving ride to history: ${ride.rideId}")
                        historyRepository.addToHistory(rideHistory)
                        
                        // Then delete the original ride
                        Log.d(TAG, "Deleting ride: ${ride.rideId} with date: ${ride.dateTime}")
                        rideRepository.deleteRide(ride.rideId, isManualDeletion = false)
                        Log.d(TAG, "Successfully deleted ride: ${ride.rideId}")
                        successCount++
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to process ride ${ride.rideId}: ${e.message}")
                        failureCount++
                    }
                }
                
                Log.d(TAG, "Cleanup completed. Success: $successCount, Failures: $failureCount")
                
                if (failureCount > 0 && successCount == 0) {
                    // All operations failed, retry the work
                    Result.retry()
                } else {
                    // Some or all operations succeeded
                    Result.success()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during ride cleanup: ${e.message}", e)
                Result.retry()
            }
        }
    }
} 