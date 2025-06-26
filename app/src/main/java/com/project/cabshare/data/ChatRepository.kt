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
     * @param userEmail The email of the current user to filter out deleted messages
     */
    suspend fun getMessages(rideId: String, limit: Int, lastMessageTimestamp: Long?, userEmail: String): List<ChatMessage>

    /**
     * Observe new messages in real-time for a ride
     * @param rideId The ID of the ride
     * @param userEmail The email of the current user to filter out deleted messages
     */
    fun observeMessages(rideId: String, userEmail: String): Flow<List<ChatMessage>>

    /**
     * Mark a message as deleted for a user
     * @param messageId The ID of the message to mark as deleted
     * @param userEmail The email of the user who is deleting the message
     */
    suspend fun markMessageAsDeleted(messageId: String, userEmail: String)
} 