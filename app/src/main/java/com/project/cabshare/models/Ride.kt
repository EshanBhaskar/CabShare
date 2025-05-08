package com.project.cabshare.models

import java.util.Date

data class Ride(
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
    val pendingRequests: List<com.project.cabshare.data.JoinRequest> = emptyList(),
    val trainNumber: String = "",
    val trainName: String = "",
    val flightNumber: String = "",
    val flightName: String = "",
    val status: String = "SCHEDULED"
)

enum class RideDirection {
    FROM_IITP,
    TO_IITP
}

data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val userId: String = "",
    val displayName: String = "",
    val phoneNumber: String = "",
    val rollNumber: String = ""
) 