package com.project.cabshare.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.project.cabshare.models.RideHistory
import com.project.cabshare.models.RideCompletionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Date
import com.google.firebase.firestore.FieldValue

class FirestoreRideHistoryRepositoryImpl : RideHistoryRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val TAG = "FirestoreRideHistoryRepo"
    
    // Collection
    private val historyCollection = firestore.collection("rideHistory")
    
    override suspend fun getUserRideHistory(userEmail: String): Flow<List<RideHistory>> = flow {
        try {
            Log.d(TAG, "Loading ride history for user: $userEmail")
            
            // Get all rides first
            val snapshot = historyCollection
                .orderBy("completedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()
            
            Log.d(TAG, "Found ${snapshot.size()} total rides in history")
            
            // Filter rides where user is either creator or passenger AND hasn't deleted the ride
            val rideHistory = snapshot.documents.mapNotNull { doc ->
                doc.toObject(RideHistory::class.java)?.apply {
                    this.rideId = doc.id
                }
            }.filter { ride ->
                val isCreator = ride.creatorEmail == userEmail
                val isPassenger = ride.passengers.any { it.email == userEmail }
                val hasNotDeleted = !ride.deletedByUsers.contains(userEmail)
                Log.d(TAG, "Ride ${ride.rideId}: isCreator=$isCreator, isPassenger=$isPassenger, hasNotDeleted=$hasNotDeleted")
                (isCreator || isPassenger) && hasNotDeleted
            }
            
            Log.d(TAG, "Filtered to ${rideHistory.size} rides for user")
            
            emit(rideHistory)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user ride history: ${e.message}")
            emit(emptyList())
        }
    }
    
    override suspend fun addToHistory(rideHistory: RideHistory) {
        withContext(Dispatchers.IO) {
            try {
                val docRef = if (rideHistory.rideId.isNotEmpty()) {
                    historyCollection.document(rideHistory.rideId)
                } else {
                    historyCollection.document()
                }
                
                val rideWithId = rideHistory.copy(
                    rideId = docRef.id,
                    // Ensure deletedByUsers is initialized as empty list
                    deletedByUsers = emptyList()
                )
                docRef.set(rideWithId).await()
                Log.d(TAG, "Successfully added ride to history: ${rideWithId.rideId}")
            } catch (e: Exception) {
                Log.e(TAG, "Error adding ride to history: ${e.message}")
                throw e
            }
        }
    }
    
    override suspend fun deleteFromHistory(rideId: String, userEmail: String) {
        withContext(Dispatchers.IO) {
            try {
                // Instead of deleting the document, add the user to deletedByUsers array
                val docRef = historyCollection.document(rideId)
                docRef.update("deletedByUsers", FieldValue.arrayUnion(userEmail)).await()
                Log.d(TAG, "Successfully marked ride $rideId as deleted for user $userEmail")
            } catch (e: Exception) {
                Log.e(TAG, "Error marking ride as deleted: ${e.message}")
                throw e
            }
        }
    }
    
    override suspend fun getRideHistory(rideId: String): RideHistory? {
        return withContext(Dispatchers.IO) {
            try {
                val document = historyCollection.document(rideId).get().await()
                if (document.exists()) {
                    document.toObject(RideHistory::class.java)?.apply {
                        this.rideId = document.id
                    }
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting ride history: ${e.message}")
                null
            }
        }
    }
    
    override suspend fun getRideHistoryBetweenDates(startDate: Date, endDate: Date): List<RideHistory> {
        return withContext(Dispatchers.IO) {
            try {
                val snapshot = historyCollection
                    .whereGreaterThanOrEqualTo("dateTime", startDate)
                    .whereLessThanOrEqualTo("dateTime", endDate)
                    .orderBy("dateTime", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .get()
                    .await()
                
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(RideHistory::class.java)?.apply {
                        this.rideId = doc.id
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting ride history between dates: ${e.message}")
                emptyList()
            }
        }
    }
} 