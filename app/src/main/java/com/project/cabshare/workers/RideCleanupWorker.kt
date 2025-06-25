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
import java.util.TimeZone
import java.text.SimpleDateFormat
import java.util.Locale

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
                // Get current time in device's timezone
                val now = Date()
                val deviceTimeZone = TimeZone.getDefault()
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.getDefault())
                dateFormat.timeZone = deviceTimeZone
                
                Log.d(TAG, "Current time: ${dateFormat.format(now)}")
                Log.d(TAG, "Device timezone: ${deviceTimeZone.id} (offset: ${deviceTimeZone.rawOffset / 3600000}h)")
                
                // Look for rides that ended 15 minutes ago
                val calendar = Calendar.getInstance(deviceTimeZone)
                calendar.time = now
                calendar.add(Calendar.MINUTE, -15) // Subtract 15 minutes
                val cutoffTime = calendar.time
                
                Log.d(TAG, "Cutoff time for old rides: ${dateFormat.format(cutoffTime)}")
                val oldRides = rideRepository.getRidesBeforeDate(cutoffTime)
                
                if (oldRides.isEmpty()) {
                    Log.d(TAG, "No old rides found to delete")
                    return@withContext Result.success()
                }
                
                Log.d(TAG, "Found ${oldRides.size} old rides to delete")
                
                // Log details of each ride found
                oldRides.forEach { ride ->
                    Log.d(TAG, """
                        Found old ride to process:
                        - ID: ${ride.rideId}
                        - DateTime: ${dateFormat.format(ride.dateTime)}
                        - Source: ${ride.source}
                        - Destination: ${ride.destination}
                        - Creator: ${ride.creatorEmail}
                    """.trimIndent())
                }
                
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
                            completionStatus = RideCompletionStatus.COMPLETED,  // Always COMPLETED for automatic deletion
                            completedAt = now
                        )
                        
                        Log.d(TAG, "Saving ride to history: ${ride.rideId} with status: COMPLETED")
                        Log.d(TAG, "Ride dateTime: ${dateFormat.format(ride.dateTime)}")
                        
                        // Make sure history is saved before deleting the ride
                        try {
                            historyRepository.addToHistory(rideHistory)
                            Log.d(TAG, "Successfully saved ride to history: ${ride.rideId}")
                            
                            // Only delete the ride if history was saved successfully
                            Log.d(TAG, "Deleting ride: ${ride.rideId}")
                            rideRepository.deleteRide(ride.rideId, isManualDeletion = false)
                            Log.d(TAG, "Successfully deleted ride: ${ride.rideId}")
                            successCount++
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to save ride to history: ${ride.rideId}", e)
                            Log.e(TAG, "Error details: ${e.message}")
                            e.printStackTrace()
                            failureCount++
                            continue // Skip to next ride if history save fails
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to process ride ${ride.rideId}: ${e.message}")
                        Log.e(TAG, "Error details: ${e.message}")
                        e.printStackTrace()
                        failureCount++
                    }
                }
                
                Log.d(TAG, """
                    Cleanup completed:
                    - Success: $successCount
                    - Failures: $failureCount
                    - Total rides processed: ${oldRides.size}
                    - Current time: ${dateFormat.format(Date())}
                """.trimIndent())
                
                if (failureCount > 0 && successCount == 0) {
                    // All operations failed, retry the work
                    Log.w(TAG, "All operations failed, requesting retry")
                    Result.retry()
                } else {
                    // Some or all operations succeeded
                    Result.success()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during ride cleanup: ${e.message}")
                Log.e(TAG, "Error details: ${e.message}")
                e.printStackTrace()
                Result.retry()
            }
        }
    }
} 