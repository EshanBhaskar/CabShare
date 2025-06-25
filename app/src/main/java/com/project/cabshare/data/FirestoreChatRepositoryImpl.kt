package com.project.cabshare.data

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.snapshots
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
                "timestamp" to messageWithId.timestamp
            )
            
            docRef.set(messageMap).await()
            Log.d(TAG, "Message sent successfully: ${messageWithId.messageId}")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message", e)
            throw e
        }
    }
    
    override suspend fun getMessages(rideId: String, limit: Int, lastMessageTimestamp: Long?): List<ChatMessage> {
        try {
            var query = chatCollection
                .whereEqualTo("rideId", rideId)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .limit(limit.toLong())
            
            // If we have a last message timestamp, start after it
            if (lastMessageTimestamp != null) {
                query = query.startAfter(Date(lastMessageTimestamp))
            }
            
            val snapshot = query.get().await()
            
            return snapshot.documents.mapNotNull { doc ->
                try {
                    ChatMessage(
                        messageId = doc.id,
                        rideId = doc.getString("rideId") ?: "",
                        senderEmail = doc.getString("senderEmail") ?: "",
                        senderName = doc.getString("senderName") ?: "",
                        message = doc.getString("message") ?: "",
                        timestamp = (doc.get("timestamp") as? Timestamp)?.toDate() ?: Date()
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error converting document to ChatMessage", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting messages", e)
            throw e
        }
    }
    
    override fun observeMessages(rideId: String): Flow<List<ChatMessage>> {
        return chatCollection
            .whereEqualTo("rideId", rideId)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    try {
                        ChatMessage(
                            messageId = doc.id,
                            rideId = doc.getString("rideId") ?: "",
                            senderEmail = doc.getString("senderEmail") ?: "",
                            senderName = doc.getString("senderName") ?: "",
                            message = doc.getString("message") ?: "",
                            timestamp = (doc.get("timestamp") as? Timestamp)?.toDate() ?: Date()
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error converting document to ChatMessage", e)
                        null
                    }
                }
            }
    }
} 