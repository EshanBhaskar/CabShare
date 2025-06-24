package com.project.cabshare.models

import java.util.Date

data class RideHistory(
    var rideId: String = "",
    val source: String = "",
    val destination: String = "",
    val dateTime: Date = Date(),
    val maxPassengers: Int = 4,
    val creator: String = "",
    val creatorEmail: String = "",
    val direction: RideDirection = RideDirection.FROM_IITP,
    val notes: String = "",
    val passengers: List<UserProfile> = emptyList(),
    val trainNumber: String = "",
    val trainName: String = "",
    val flightNumber: String = "",
    val flightName: String = "",
    val completionStatus: RideCompletionStatus = RideCompletionStatus.COMPLETED,
    val completedAt: Date = Date()
)

enum class RideCompletionStatus {
    COMPLETED,  // Ride was completed successfully
    EXPIRED,    // Ride was not taken (date passed)
    CANCELLED   // Ride was cancelled by creator
} 