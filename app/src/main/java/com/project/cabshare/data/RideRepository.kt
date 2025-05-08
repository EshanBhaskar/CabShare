package com.project.cabshare.data

import com.project.cabshare.models.Ride
import com.project.cabshare.models.RideDirection
import com.project.cabshare.models.UserProfile
import kotlinx.coroutines.flow.Flow
import java.util.Date

// Define request status enum
enum class RequestStatus {
    PENDING, ACCEPTED, REJECTED
}

// Define join request data class
data class JoinRequest(
    val requestId: String = "",
    val rideId: String = "",
    val userId: String = "", // Email of the requester
    val userProfile: UserProfile = UserProfile(),
    val status: RequestStatus = RequestStatus.PENDING,
    val timestamp: Date = Date()
)

interface RideRepository {
    suspend fun getRide(rideId: String): Ride?
    suspend fun createRide(ride: Ride): String
    suspend fun updateRide(ride: Ride)
    suspend fun deleteRide(rideId: String)
    suspend fun getUserRides(email: String): List<Ride>
    suspend fun getRidesByDirection(direction: RideDirection): List<Ride>
    fun observeUserRides(email: String): Flow<List<Ride>>
    fun observeRidesByDirection(direction: RideDirection): Flow<List<Ride>>
    suspend fun requestToJoinRide(rideId: String, userProfile: UserProfile): String
    suspend fun acceptJoinRequest(rideId: String, requestId: String): Boolean
    suspend fun rejectJoinRequest(rideId: String, requestId: String): Boolean
    suspend fun cancelJoinRequest(rideId: String, userEmail: String)
    suspend fun getUserJoinRequests(email: String): List<JoinRequest>
    suspend fun getRidesBeforeDate(date: Date): List<Ride>
    suspend fun updateTrainDetails(rideId: String, trainNumber: String, trainName: String)
    suspend fun updateFlightDetails(rideId: String, flightNumber: String, flightName: String)
    suspend fun removePassenger(rideId: String, passengerEmail: String)
    fun observeRide(rideId: String): Flow<Ride?>
} 