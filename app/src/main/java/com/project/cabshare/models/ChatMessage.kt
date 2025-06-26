package com.project.cabshare.models

import java.util.Date

data class ChatMessage(
    val messageId: String = "",
    val rideId: String = "",
    val senderEmail: String = "",
    val senderName: String = "",
    val message: String = "",
    val timestamp: Date = Date(),
    val deletedByUsers: List<String> = emptyList()
) 