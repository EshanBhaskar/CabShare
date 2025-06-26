package com.project.cabshare.data

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.snapshots
import com.google.firebase.firestore.FieldValue
import com.project.cabshare.models.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Date

class FirestoreChatRepositoryImpl : ChatRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val TAG = "FirestoreChatRepo"
    
    // Collection
    private val chatCollection = firestore.collection("chats")
    
    override suspend fun sendMessage(message: ChatMessage) {
        try {
            val docRef = chatCollection.document()
            val messageWithId = message.copy(messageId = docRef.id)
            
            // Create a map of the message data
            val messageMap = mapOf(
                "messageId" to messageWithId.messageId,
                "rideId" to messageWithId.rideId,
                "senderEmail" to messageWithId.senderEmail,
                "senderName" to messageWithId.senderName,
                "message" to messageWithId.message,
                "timestamp" to messageWithId.timestamp,
                "deletedByUsers" to emptyList<String>()
            )
            
            docRef.set(messageMap).await()
            Log.d(TAG, "Message sent successfully: ${messageWithId.messageId}")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message", e)
            throw e
        }
    }
    
    override suspend fun getMessages(rideId: String, limit: Int, lastMessageTimestamp: Long?, userEmail: String): List<ChatMessage> {
        try {
            Log.d(TAG, "Getting messages for ride: $rideId, limit: $limit, lastTimestamp: $lastMessageTimestamp")
            var query = chatCollection
                .whereEqualTo("rideId", rideId)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .limit(limit.toLong())
            
            // If we have a last message timestamp, start after it
            if (lastMessageTimestamp != null) {
                query = query.startAfter(Date(lastMessageTimestamp))
            }
            
            val snapshot = query.get().await()
            Log.d(TAG, "Found ${snapshot.size()} messages for ride $rideId")
            
            return snapshot.documents.mapNotNull { doc ->
                try {
                    val deletedByUsers = doc.get("deletedByUsers") as? List<String> ?: emptyList()
                    // Skip messages that have been deleted by this user
                    if (deletedByUsers.contains(userEmail)) {
                        Log.d(TAG, "Skipping deleted message ${doc.id} for user $userEmail")
                        return@mapNotNull null
                    }
                    
                    ChatMessage(
                        messageId = doc.id,
                        rideId = doc.getString("rideId") ?: "",
                        senderEmail = doc.getString("senderEmail") ?: "",
                        senderName = doc.getString("senderName") ?: "",
                        message = doc.getString("message") ?: "",
                        timestamp = (doc.get("timestamp") as? Timestamp)?.toDate() ?: Date(),
                        deletedByUsers = deletedByUsers
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error converting document to ChatMessage", e)
                    null
                }
            }.also { messages ->
                Log.d(TAG, "Returning ${messages.size} messages for ride $rideId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting messages", e)
            throw e
        }
    }
    
    override fun observeMessages(rideId: String, userEmail: String): Flow<List<ChatMessage>> {
        Log.d(TAG, "Starting to observe messages for ride: $rideId")
        return chatCollection
            .whereEqualTo("rideId", rideId)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .snapshots()
            .map { snapshot ->
                Log.d(TAG, "Received snapshot with ${snapshot.size()} messages for ride $rideId")
                snapshot.documents.mapNotNull { doc ->
                    try {
                        val deletedByUsers = doc.get("deletedByUsers") as? List<String> ?: emptyList()
                        // Skip messages that have been deleted by this user
                        if (deletedByUsers.contains(userEmail)) {
                            Log.d(TAG, "Skipping deleted message ${doc.id} for user $userEmail")
                            return@mapNotNull null
                        }
                        
                        ChatMessage(
                            messageId = doc.id,
                            rideId = doc.getString("rideId") ?: "",
                            senderEmail = doc.getString("senderEmail") ?: "",
                            senderName = doc.getString("senderName") ?: "",
                            message = doc.getString("message") ?: "",
                            timestamp = (doc.get("timestamp") as? Timestamp)?.toDate() ?: Date(),
                            deletedByUsers = deletedByUsers
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error converting document to ChatMessage", e)
                        null
                    }
                }.also { messages ->
                    Log.d(TAG, "Emitting ${messages.size} messages for ride $rideId")
                }
            }
    }
    
    override suspend fun markMessageAsDeleted(messageId: String, userEmail: String) {
        try {
            val docRef = chatCollection.document(messageId)
            docRef.update("deletedByUsers", FieldValue.arrayUnion(userEmail)).await()
            Log.d(TAG, "Successfully marked message $messageId as deleted for user $userEmail")
        } catch (e: Exception) {
            Log.e(TAG, "Error marking message as deleted", e)
            throw e
        }
    }
} 