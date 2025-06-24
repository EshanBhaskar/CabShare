package com.project.cabshare.data

import com.project.cabshare.models.RideHistory
import kotlinx.coroutines.flow.Flow
import java.util.Date

interface RideHistoryRepository {
    /**
     * Get all ride history entries for a specific user
     */
    suspend fun getUserRideHistory(userEmail: String): Flow<List<RideHistory>>
    
    /**
     * Add a completed ride to history
     */
    suspend fun addToHistory(rideHistory: RideHistory)
    
    /**
     * Delete a ride history entry
     */
    suspend fun deleteFromHistory(rideId: String)
    
    /**
     * Get a specific ride history entry
     */
    suspend fun getRideHistory(rideId: String): RideHistory?
    
    /**
     * Get all ride history entries between two dates
     */
    suspend fun getRideHistoryBetweenDates(startDate: Date, endDate: Date): List<RideHistory>
} 