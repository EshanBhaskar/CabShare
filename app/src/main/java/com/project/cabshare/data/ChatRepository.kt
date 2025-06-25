package com.project.cabshare.data

import com.project.cabshare.models.ChatMessage
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    /**
     * Send a new message in a ride's chat
     */
    suspend fun sendMessage(message: ChatMessage)

    /**
     * Get messages for a ride with pagination
     * @param rideId The ID of the ride
     * @param limit Number of messages to load
     * @param lastMessageTimestamp Timestamp of the last message loaded, null for initial load
     */
    suspend fun getMessages(rideId: String, limit: Int, lastMessageTimestamp: Long?): List<ChatMessage>

    /**
     * Observe new messages in real-time for a ride
     */
    fun observeMessages(rideId: String): Flow<List<ChatMessage>>
} 