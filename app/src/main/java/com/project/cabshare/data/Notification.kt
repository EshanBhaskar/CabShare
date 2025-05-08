package com.project.cabshare.data

import com.google.firebase.Timestamp

enum class NotificationType {
    REQUEST_RECEIVED, // Someone requested to join your ride
    REQUEST_ACCEPTED, // Your request to join a ride was accepted
    REQUEST_REJECTED, // Your request to join a ride was rejected
    PASSENGER_JOINED, // Someone else joined a ride you are part of
    RIDE_CANCELLED,   // A ride you created or joined was cancelled
    OTHER             // General notifications
}

data class Notification(
    val id: String = "", // Document ID from Firestore
    val userId: String = "", // User ID this notification belongs to
    val message: String = "", // The notification text
    val timestamp: Timestamp = Timestamp.now(), // When the notification was created
    val type: NotificationType = NotificationType.OTHER, // Type of notification
    val relatedRideId: String? = null, // Optional: ID of the ride this notification relates to
    val isRead: Boolean = false // To track if the user has seen it
) 