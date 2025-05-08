package com.project.cabshare.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.project.cabshare.models.Ride
import com.project.cabshare.models.RideDirection
import com.project.cabshare.models.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Date
import com.google.firebase.firestore.Transaction
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.map
import com.google.firebase.firestore.ktx.snapshots
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.MetadataChanges
import com.project.cabshare.data.Notification
import com.project.cabshare.data.NotificationType
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEmpty

class RideFullException(message: String) : Exception(message)

class FirestoreRideRepositoryImpl : RideRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val TAG = "FirestoreRideRepository"
    
    // Collections
    private val ridesCollection = firestore.collection("rides")
    private val requestsCollection = firestore.collection("joinRequests")
    private val notificationsCollection = firestore.collection("notifications")
    
    // Observable flows
    private val _userRidesFlow = MutableStateFlow<List<Ride>>(emptyList())
    private val _directionRidesFlow = MutableStateFlow<List<Ride>>(emptyList())
    
    override suspend fun getRidesBeforeDate(date: Date): List<Ride> {
        return try {
            val snapshot = ridesCollection
                .whereLessThan("dateTime", date)
                .get()
                .await()
            
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(Ride::class.java)?.apply { 
                    this.rideId = doc.id 
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting rides before date: ${e.message}")
            emptyList()
        }
    }
    
    override suspend fun createRide(ride: Ride): String {
        return try {
            // Validate that ride date is in the future
            val now = Date()
            if (ride.dateTime.before(now)) {
                throw IllegalArgumentException("Cannot create a ride with a past date/time")
            }
            
            // Create the ride in Firestore
            val docRef = ridesCollection.document()
            
            // Create a copy with both creator and creatorEmail set to the same value for compatibility
            val rideWithId = ride.copy(
                rideId = docRef.id,
                creatorEmail = ride.creator // Make sure creatorEmail is set
            )
            
            // Log the train/flight details before saving
            Log.d("RideDebug", "REPOSITORY - Before saving to Firestore: Train '${rideWithId.trainNumber}' - '${rideWithId.trainName}'")
            Log.d("RideDebug", "REPOSITORY - Before saving to Firestore: Flight '${rideWithId.flightNumber}' - '${rideWithId.flightName}'")
            Log.d("RideDebug", "REPOSITORY - Before saving to Firestore: Notes '${rideWithId.notes}'")
            
            Log.d(TAG, "Saving ride with train number: '${rideWithId.trainNumber}', train name: '${rideWithId.trainName}'")
            Log.d(TAG, "Saving ride with flight number: '${rideWithId.flightNumber}', flight name: '${rideWithId.flightName}'")
            
            // Ensure non-null values for train and flight details
            val trainNumber = rideWithId.trainNumber
            val trainName = rideWithId.trainName
            val flightNumber = rideWithId.flightNumber
            val flightName = rideWithId.flightName
            
            // Create an explicit map of all fields to ensure they're properly set in Firestore
            val rideMap = mapOf(
                "rideId" to rideWithId.rideId,
                "source" to rideWithId.source,
                "destination" to rideWithId.destination,
                "dateTime" to rideWithId.dateTime,
                "maxPassengers" to rideWithId.maxPassengers,
                "creator" to rideWithId.creator,
                "creatorEmail" to rideWithId.creatorEmail,
                "direction" to rideWithId.direction.toString(),
                "notes" to rideWithId.notes,
                "passengers" to rideWithId.passengers,
                "pendingRequests" to rideWithId.pendingRequests,
                "trainNumber" to trainNumber,
                "trainName" to trainName,
                "flightNumber" to flightNumber,
                "flightName" to flightName,
                "status" to rideWithId.status
            )
            
            // Log the map values before setting in Firestore
            Log.d("RideDebug", "REPOSITORY - Map values for train: '${rideMap["trainNumber"]}', '${rideMap["trainName"]}'")
            Log.d("RideDebug", "REPOSITORY - Map values for flight: '${rideMap["flightNumber"]}', '${rideMap["flightName"]}'")
            Log.d("RideDebug", "REPOSITORY - Map value for notes: '${rideMap["notes"]}'")
            
            // Use set with the explicit map instead of the object
            docRef.set(rideMap).await()
            Log.d("RideDebug", "REPOSITORY - Document created: ${rideWithId.rideId}")
            
            // Force immediate update of train and flight details with a separate call
            if (trainNumber.isNotBlank() || trainName.isNotBlank()) {
                try {
                    Log.d("RideDebug", "REPOSITORY - Immediately updating train details after creation")
                    // Small delay to ensure the document is created first
                    kotlinx.coroutines.delay(300)
                    updateTrainDetails(rideWithId.rideId, trainNumber, trainName)
                } catch (e: Exception) {
                    Log.e("RideDebug", "REPOSITORY - Error updating train details during creation", e)
                }
            }
            
            if (flightNumber.isNotBlank() || flightName.isNotBlank()) {
                try {
                    Log.d("RideDebug", "REPOSITORY - Immediately updating flight details after creation")
                    // Small delay to ensure the document is created first
                    kotlinx.coroutines.delay(300)
                    updateFlightDetails(rideWithId.rideId, flightNumber, flightName)
                } catch (e: Exception) {
                    Log.e("RideDebug", "REPOSITORY - Error updating flight details during creation", e)
                }
            }
            
            rideWithId.rideId
        } catch (e: Exception) {
            Log.e("RideDebug", "REPOSITORY - Error creating ride: ${e.message}")
            Log.e(TAG, "Error creating ride: ${e.message}")
            throw e
        }
    }
    
    override suspend fun getRide(rideId: String): Ride? {
        return withContext(Dispatchers.IO) {
            try {
                val document = ridesCollection.document(rideId).get().await()
                if (document.exists()) {
                    // Log all available fields in the document for debugging
                    val data = document.data
                    Log.d(TAG, "Raw ride data: $data")
                    Log.d(TAG, "Train number: ${data?.get("trainNumber")}, Train name: ${data?.get("trainName")}")
                    Log.d(TAG, "Flight number: ${data?.get("flightNumber")}, Flight name: ${data?.get("flightName")}")
                    Log.d(TAG, "Notes: ${data?.get("notes")}")
                    
                    val ride = document.toObject(Ride::class.java)?.apply {
                        this.rideId = document.id
                    }
                    
                    // Double-check ride fields
                    ride?.let {
                        Log.d(TAG, "Converted ride train number: '${it.trainNumber}', train name: '${it.trainName}'")
                        Log.d(TAG, "Converted ride flight number: '${it.flightNumber}', flight name: '${it.flightName}'")
                        Log.d(TAG, "Converted ride notes: '${it.notes}'")
                    }
                    
                    Log.d(TAG, "Retrieved ride: $ride")
                    ride
                } else {
                    Log.d(TAG, "No ride found with ID: $rideId")
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting ride", e)
                null
            }
        }
    }
    
    override suspend fun updateRide(ride: Ride) {
        withContext(Dispatchers.IO) {
            try {
                // Validate that ride date is in the future
                val now = Date()
                if (ride.dateTime.before(now)) {
                    throw IllegalArgumentException("Cannot update a ride with a past date/time")
                }
                
                // Log the train/flight details before updating
                Log.d(TAG, "Updating ride with train number: '${ride.trainNumber}', train name: '${ride.trainName}'")
                Log.d(TAG, "Updating ride with flight number: '${ride.flightNumber}', flight name: '${ride.flightName}'")
                
                // Create an explicit map of all fields to ensure they're properly set in Firestore
                val rideMap = mapOf(
                    "rideId" to ride.rideId,
                    "source" to ride.source,
                    "destination" to ride.destination,
                    "dateTime" to ride.dateTime,
                    "maxPassengers" to ride.maxPassengers,
                    "creator" to ride.creator,
                    "creatorEmail" to ride.creatorEmail,
                    "direction" to ride.direction.toString(),
                    "notes" to ride.notes,
                    "passengers" to ride.passengers,
                    "pendingRequests" to ride.pendingRequests,
                    "trainNumber" to ride.trainNumber,
                    "trainName" to ride.trainName,
                    "flightNumber" to ride.flightNumber,
                    "flightName" to ride.flightName,
                    "status" to ride.status
                )
                
                // Use set with the explicit map instead of the object
                ridesCollection.document(ride.rideId).set(rideMap).await()
                
                Log.d(TAG, "Ride updated successfully: ${ride.rideId}")
            } catch (e: Exception) {
                Log.e(TAG, "Error updating ride", e)
                throw e
            }
        }
    }
    
    override suspend fun deleteRide(rideId: String) {
        withContext(Dispatchers.IO) {
            try {
                // First, get the ride to check for associated join requests
                val rideDoc = ridesCollection.document(rideId).get().await()
                if (rideDoc.exists()) {
                    // Get all join requests for this ride
                    val joinRequestsQuery = requestsCollection
                        .whereEqualTo("rideId", rideId)
                        .get()
                        .await()
                    
                    // Delete each join request
                    joinRequestsQuery.documents.forEach { requestDoc ->
                        val requestId = requestDoc.id
                        requestsCollection.document(requestId).delete().await()
                        Log.d(TAG, "Deleted join request $requestId associated with ride $rideId")
                    }
                    
                    // Count how many requests were deleted
                    val deletedRequestsCount = joinRequestsQuery.size()
                    if (deletedRequestsCount > 0) {
                        Log.d(TAG, "Deleted $deletedRequestsCount join requests for ride $rideId")
                    }
                    
                    // Delete all contact exchanges related to this ride
                    val contactExchangesQuery = firestore.collection("contactExchanges")
                        .whereEqualTo("rideId", rideId)
                        .get()
                        .await()
                    
                    // Delete each contact exchange
                    contactExchangesQuery.documents.forEach { exchangeDoc ->
                        val exchangeId = exchangeDoc.id
                        firestore.collection("contactExchanges").document(exchangeId).delete().await()
                        Log.d(TAG, "Deleted contact exchange $exchangeId associated with ride $rideId")
                    }
                    
                    // Count how many contact exchanges were deleted
                    val deletedExchangesCount = contactExchangesQuery.size()
                    if (deletedExchangesCount > 0) {
                        Log.d(TAG, "Deleted $deletedExchangesCount contact exchanges for ride $rideId")
                    }
                    
                    // Delete all notifications related to this ride
                    val notificationsQuery = notificationsCollection
                        .whereEqualTo("relatedRideId", rideId)
                        .get()
                        .await()
                    
                    // Delete each notification
                    notificationsQuery.documents.forEach { notificationDoc ->
                        val notificationId = notificationDoc.id
                        notificationsCollection.document(notificationId).delete().await()
                        Log.d(TAG, "Deleted notification $notificationId associated with ride $rideId")
                    }
                    
                    // Count how many notifications were deleted
                    val deletedNotificationsCount = notificationsQuery.size()
                    if (deletedNotificationsCount > 0) {
                        Log.d(TAG, "Deleted $deletedNotificationsCount notifications for ride $rideId")
                    }
                }
                
                // Finally, delete the ride itself
                ridesCollection.document(rideId).delete().await()
                Log.d(TAG, "Ride deleted successfully: $rideId")
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting ride and its associated data", e)
                throw e
            }
        }
    }
    
    override suspend fun getUserRides(email: String): List<Ride> {
        return withContext(Dispatchers.IO) {
            try {
                // Get rides created by the user - check both creator and creatorEmail fields for compatibility
                val creatorRidesQuery1 = ridesCollection.whereEqualTo("creator", email).get().await()
                val creatorRidesQuery2 = ridesCollection.whereEqualTo("creatorEmail", email).get().await()
                
                val creatorRides1 = creatorRidesQuery1.documents.mapNotNull { doc -> 
                    doc.toObject(Ride::class.java)?.apply { this.rideId = doc.id } 
                }
                
                val creatorRides2 = creatorRidesQuery2.documents.mapNotNull { doc -> 
                    doc.toObject(Ride::class.java)?.apply { this.rideId = doc.id } 
                }
                
                // Combine the results
                val creatorRides = (creatorRides1 + creatorRides2).distinctBy { it.rideId }
                
                // Get all rides to filter for passengers and pending requests
                val allRidesSnapshot = ridesCollection.get().await().documents
                val allRidesObjects = allRidesSnapshot.mapNotNull { doc -> 
                    doc.toObject(Ride::class.java)?.apply { this.rideId = doc.id } 
                }
                
                // Get rides where the user is a passenger
                val passengerRides = allRidesObjects.filter { ride -> 
                    ride.passengers.any { it.email == email } 
                }
                
                // Get rides where the user has a pending request
                val pendingRequestRides = allRidesObjects.filter { ride ->
                    ride.pendingRequests.any { request -> 
                        request.userId == email && request.status == RequestStatus.PENDING 
                    }
                }
                
                // Combine all unique rides
                val allRides = (creatorRides + passengerRides + pendingRequestRides).distinctBy { it.rideId }
                Log.d(TAG, "Retrieved ${allRides.size} rides for user: $email (including ${pendingRequestRides.size} with pending requests)")
                
                // Update the flow
                _userRidesFlow.value = allRides.sortedByDescending { it.dateTime }
                
                allRides
            } catch (e: Exception) {
                Log.e(TAG, "Error getting user rides", e)
                emptyList()
            }
        }
    }
    
    override suspend fun getRidesByDirection(direction: RideDirection): List<Ride> {
        return withContext(Dispatchers.IO) {
            try {
                // Get rides by direction
                val directionRides = ridesCollection
                    .whereEqualTo("direction", direction.toString())
                    .get()
                    .await()
                    .documents
                    .mapNotNull { doc -> 
                        doc.toObject(Ride::class.java)?.apply { this.rideId = doc.id } 
                    }
                    .sortedByDescending { it.dateTime }
                
                Log.d(TAG, "Retrieved ${directionRides.size} rides for direction: $direction")
                
                // Update the flow
                _directionRidesFlow.value = directionRides
                
                directionRides
            } catch (e: Exception) {
                Log.e(TAG, "Error getting rides by direction", e)
                emptyList()
            }
        }
    }
    
    override fun observeUserRides(email: String): Flow<List<Ride>> {
        // Start a listener for rides created by this user
        ridesCollection
            .whereEqualTo("creator", email)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to user rides", error)
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    // Get the current rides
                    val creatorRides = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Ride::class.java)?.apply { this.rideId = doc.id }
                    }
                    
                    // Get rides where the user is a passenger or has pending requests
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val allRidesSnapshot = ridesCollection.get().await().documents
                            val allRidesObjects = allRidesSnapshot.mapNotNull { doc -> 
                                doc.toObject(Ride::class.java)?.apply { this.rideId = doc.id } 
                            }
                            
                            // Rides where user is a passenger
                            val passengerRides = allRidesObjects.filter { ride -> 
                                ride.passengers.any { it.email == email } 
                            }
                            
                            // Rides where user has a pending request
                            val pendingRequestRides = allRidesObjects.filter { ride ->
                                ride.creatorEmail != email &&  // Don't include rides the user created
                                ride.pendingRequests.any { request -> 
                                    request.userId == email && request.status == RequestStatus.PENDING 
                                }
                            }
                            
                            // Combine all rides and remove duplicates
                            val allRides = (creatorRides + passengerRides + pendingRequestRides)
                                .distinctBy { it.rideId }
                                .sortedByDescending { it.dateTime }
                            
                            _userRidesFlow.value = allRides
                            Log.d(TAG, "User rides updated: ${allRides.size} rides (including ${pendingRequestRides.size} with pending requests)")
                        } catch (e: Exception) {
                            Log.e(TAG, "Error fetching user rides", e)
                        }
                    }
                }
            }
        
        return _userRidesFlow
    }
    
    override fun observeRidesByDirection(direction: RideDirection): Flow<List<Ride>> {
        // Listen for rides in the specified direction
        ridesCollection
            .whereEqualTo("direction", direction.toString())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to direction rides", error)
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val directionRides = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Ride::class.java)?.apply { this.rideId = doc.id }
                    }.sortedByDescending { it.dateTime }
                    
                    _directionRidesFlow.value = directionRides
                    Log.d(TAG, "Direction rides updated: ${directionRides.size} rides")
                }
            }
        
        return _directionRidesFlow
    }
    
    override suspend fun requestToJoinRide(rideId: String, userProfile: UserProfile): String {
        return withContext(Dispatchers.IO) {
            try {
                // First, check if the ride exists
                val rideDoc = ridesCollection.document(rideId).get().await()
                if (!rideDoc.exists()) {
                    throw IllegalArgumentException("Ride $rideId does not exist")
                }
                
                // Get ride details for the notification
                val ride = rideDoc.toObject(Ride::class.java)
                if (ride == null) {
                    throw IllegalArgumentException("Invalid ride data")
                }
                
                // Create a join request
                val requestId = requestsCollection.document().id
                val request = JoinRequest(
                    requestId = requestId,
                    rideId = rideId,
                    userId = userProfile.email,
                    userProfile = userProfile,
                    status = RequestStatus.PENDING,
                    timestamp = Date()
                )
                
                // Save the request
                requestsCollection.document(requestId).set(request).await()
                
                // Also add the pending request to the ride document
                val updatedPendingRequests = ride.pendingRequests.toMutableList()
                updatedPendingRequests.add(request)
                
                // Update the ride with the new pending request
                ridesCollection.document(rideId).update(
                    "pendingRequests", updatedPendingRequests
                ).await()
                
                Log.d(TAG, "Join request created: $requestId for ride: $rideId")
                
                // Create a notification for the ride creator - this is new
                val creatorEmail = ride.creatorEmail
                if (creatorEmail.isNotEmpty()) {
                    Log.d(TAG, "Creating notification for ride creator: $creatorEmail")
                    val rideInfo = "from ${ride.source} to ${ride.destination}"
                    val userInfo = "${userProfile.displayName}"
                    
                    val notification = Notification(
                        userId = creatorEmail,
                        message = "$userInfo has requested to join your ride $rideInfo.",
                        type = NotificationType.REQUEST_RECEIVED,
                        relatedRideId = rideId
                    )
                    
                    // Use CoroutineScope to launch the suspend function
                    CoroutineScope(Dispatchers.IO).launch {
                        createNotification(notification)
                    }
                } else {
                    Log.w(TAG, "Could not create notification: creator email is empty")
                }
                
                requestId
            } catch (e: Exception) {
                Log.e(TAG, "Error creating join request", e)
                throw e
            }
        }
    }
    
    override suspend fun acceptJoinRequest(rideId: String, requestId: String): Boolean {
        val rideRef = ridesCollection.document(rideId)
        val requestRef = requestsCollection.document(requestId)
        var requesterUserId: String? = null
        var rideSource: String = ""
        var rideDestination: String = ""

        try {
            Log.d(TAG, "Starting acceptJoinRequest for ride $rideId, request $requestId")
            
            firestore.runTransaction { transaction ->
                // Read the current state of the ride
                val rideSnapshot = transaction.get(rideRef)
                val ride = rideSnapshot.toObject(Ride::class.java)
                    ?: throw FirebaseFirestoreException(
                        "Ride $rideId not found or invalid data",
                        FirebaseFirestoreException.Code.NOT_FOUND
                    )
                
                // Store ride info for notification
                rideSource = ride.source
                rideDestination = ride.destination
                Log.d(TAG, "Found ride: ${ride.rideId} from $rideSource to $rideDestination")
                
                // Read the current state of the request
                val requestSnapshot = transaction.get(requestRef)
                val request = requestSnapshot.toObject(JoinRequest::class.java)
                    ?: throw FirebaseFirestoreException(
                        "Request $requestId not found or invalid data",
                        FirebaseFirestoreException.Code.NOT_FOUND
                    )
                requesterUserId = request.userId // Store user ID
                Log.d(TAG, "Found request from user: $requesterUserId, status: ${request.status}")

                // --- Validation within the transaction ---
                if (request.status != RequestStatus.PENDING) {
                    Log.w(TAG, "Attempted to accept non-pending request: $requestId")
                    return@runTransaction null // Exit gracefully
                }

                // 2. Check if ride is full
                if (ride.passengers.size >= ride.maxPassengers) {
                    Log.w(TAG, "Attempted to accept request $requestId for full ride $rideId")
                    throw RideFullException("Ride is already full. Cannot accept request $requestId.")
                }

                // --- If validations pass, proceed with updates ---
                val acceptedRequest = request.copy(status = RequestStatus.ACCEPTED)
                transaction.set(requestRef, acceptedRequest)

                val updatedPassengers = ride.passengers.toMutableList().apply { add(request.userProfile) }
                val updatedPendingRequests = ride.pendingRequests.filter { it.requestId != requestId }

                transaction.update(rideRef, mapOf(
                    "passengers" to updatedPassengers,
                    "pendingRequests" to updatedPendingRequests
                ))
                Log.d(TAG, "Transaction: Join request accepted: $requestId for ride: $rideId")
                null // Indicate success

            }.await()

            Log.d(TAG, "Transaction successfully committed for accepting $requestId")

            // --- Create Notification (after successful transaction) ---
            if (requesterUserId != null && rideSource.isNotEmpty() && rideDestination.isNotEmpty()) {
                Log.d(TAG, "Preparing to create notification for accepted request: User=$requesterUserId")
                val rideInfo = "from $rideSource to $rideDestination"
                val notification = Notification(
                    userId = requesterUserId!!,
                    message = "Your request to join the ride $rideInfo has been accepted.",
                    type = NotificationType.REQUEST_ACCEPTED,
                    relatedRideId = rideId
                )
                // Use CoroutineScope to launch the suspend function
                CoroutineScope(Dispatchers.IO).launch {
                    createNotification(notification)
                }
            } else {
                Log.w(TAG, "Could not create notification: userId=$requesterUserId, source=$rideSource, dest=$rideDestination")
            }

            // Contact exchange logic remains here...
            try {
                val ride = getRide(rideId) // Re-fetch ride to get creator info if needed
                val request = requestRef.get().await().toObject(JoinRequest::class.java)
                if (ride != null && request != null) {
                    val contactExchangeId = "${rideId}_${request.userId}"
                    val contactExchange = hashMapOf(
                        "rideId" to rideId,
                        "rideCreator" to ride.creatorEmail,
                        // Fetching creator phone might require another read if not stored directly
                        "rideCreatorPhone" to (ride.passengers.find { it.email == ride.creatorEmail }?.phoneNumber ?: ""),
                        "passenger" to request.userProfile.email,
                        "passengerPhone" to request.userProfile.phoneNumber,
                        "exchangeTime" to Date(),
                        "status" to "ACTIVE"
                    )
                     firestore.collection("contactExchanges").document(contactExchangeId)
                         .set(contactExchange).await()
                     Log.d(TAG, "Created contact exchange record for ride: $rideId (post-transaction)")
                }
            } catch (e: Exception) {
                 Log.e(TAG, "Error creating contact exchange post-transaction", e)
            }
            return true // Return true on success
        } catch (e: Exception) {
            Log.e(TAG, "Error accepting join request", e)
            return false
        }
    }
    
    override suspend fun rejectJoinRequest(rideId: String, requestId: String): Boolean {
        var requesterUserId: String? = null
        var rideSource: String = ""
        var rideDestination: String = ""

        val success = withContext(Dispatchers.IO) {
            try {
                // Get request data to find the user ID
                val requestDoc = requestsCollection.document(requestId).get().await()
                if (!requestDoc.exists()) {
                    throw IllegalArgumentException("Request $requestId does not exist")
                }
                val request = requestDoc.toObject(JoinRequest::class.java)
                if (request == null) {
                    throw IllegalArgumentException("Invalid request data")
                }
                requesterUserId = request.userId // Store user ID for notification

                // Get ride data for the notification message
                val rideDoc = ridesCollection.document(rideId).get().await()
                if (rideDoc.exists()) {
                    val ride = rideDoc.toObject(Ride::class.java)
                    if (ride != null) {
                        rideSource = ride.source
                        rideDestination = ride.destination
                    }
                }

                // Update the request status
                val updatedRequest = request.copy(status = RequestStatus.REJECTED)
                requestsCollection.document(requestId).set(updatedRequest).await()

                // Remove from the ride's pending requests (if ride exists)
                if (rideDoc.exists() && rideDoc.toObject(Ride::class.java) != null) {
                    val ride = rideDoc.toObject(Ride::class.java)!!
                    val updatedPendingRequests = ride.pendingRequests
                        .filter { it.requestId != requestId }
                        .toList()
                    ridesCollection.document(rideId).update("pendingRequests", updatedPendingRequests).await()
                }

                Log.d(TAG, "Join request rejected: $requestId for ride: $rideId")
                true // Indicate success
            } catch (e: Exception) {
                Log.e(TAG, "Error rejecting join request", e)
                false // Indicate failure
            }
        }

        // --- Create Notification (if rejection was successful) ---
        if (success && requesterUserId != null && rideSource.isNotEmpty() && rideDestination.isNotEmpty()) {
            val rideInfo = "from $rideSource to $rideDestination"
            val notification = Notification(
                userId = requesterUserId!!,
                message = "Your request to join the ride $rideInfo was rejected.",
                type = NotificationType.REQUEST_REJECTED,
                relatedRideId = rideId
            )
            // Use CoroutineScope to launch the suspend function
            CoroutineScope(Dispatchers.IO).launch {
                createNotification(notification)
            }
        }

        return success
    }
    
    override suspend fun getUserJoinRequests(email: String): List<JoinRequest> {
        return withContext(Dispatchers.IO) {
            try {
                // Get requests where the user is the requester
                val requestsSnapshot = requestsCollection
                    .whereEqualTo("userId", email)
                    .get()
                    .await()
                
                val requests = requestsSnapshot.documents.mapNotNull { doc ->
                    doc.toObject(JoinRequest::class.java)
                }
                
                Log.d(TAG, "Retrieved ${requests.size} join requests for user: $email")
                requests
            } catch (e: Exception) {
                Log.e(TAG, "Error getting user join requests", e)
                emptyList()
            }
        }
    }
    
    override suspend fun cancelJoinRequest(rideId: String, userEmail: String) {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Cancelling join request for user $userEmail from ride $rideId")
                
                // Get the ride document
                val rideDoc = ridesCollection.document(rideId).get().await()
                if (!rideDoc.exists()) {
                    throw IllegalArgumentException("Ride $rideId does not exist")
                }
                
                val ride = rideDoc.toObject(Ride::class.java)
                if (ride != null) {
                    // Find the pending request for this user
                    val requestToCancel = ride.pendingRequests.find { 
                        it.userId == userEmail && it.status == RequestStatus.PENDING
                    }
                    
                    if (requestToCancel != null) {
                        // Update the request status to CANCELLED
                        val updatedRequest = requestToCancel.copy(status = RequestStatus.REJECTED)
                        requestsCollection.document(requestToCancel.requestId).set(updatedRequest).await()
                        
                        // Remove from the ride's pending requests
                        val updatedPendingRequests = ride.pendingRequests
                            .filter { it.requestId != requestToCancel.requestId }
                            .toList()
                        
                        // Update the ride
                        ridesCollection.document(rideId).update(
                            "pendingRequests", updatedPendingRequests
                        ).await()
                        
                        Log.d(TAG, "Join request cancelled: ${requestToCancel.requestId} for ride: $rideId")
                    } else {
                        Log.d(TAG, "No pending request found for user $userEmail in ride $rideId")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error cancelling join request", e)
                throw e
            }
        }
    }
    
    override suspend fun updateTrainDetails(rideId: String, trainNumber: String, trainName: String) {
        withContext(Dispatchers.IO) {
            try {
                // Log the train details update
                Log.d("RideDebug", "REPOSITORY - Updating train details: '${trainNumber}', '${trainName}' for ride ${rideId}")
                Log.d(TAG, "Explicitly updating train details for ride $rideId")
                Log.d(TAG, "Setting train number: '$trainNumber', train name: '$trainName'")
                
                // Create a map with just the train details
                val updates = mapOf(
                    "trainNumber" to trainNumber,
                    "trainName" to trainName
                )
                
                // Use update to specifically set just these fields
                ridesCollection.document(rideId).update(updates).await()
                Log.d("RideDebug", "REPOSITORY - First update completed")
                
                // Fallback - Just to be extra sure, try a direct write for the "trainNumber" field
                try {
                    if (trainNumber.isNotBlank()) {
                        Log.d("RideDebug", "REPOSITORY - DIRECT WRITE FOR TRAIN NUMBER: '$trainNumber'")
                        Log.d(TAG, "DIRECT WRITE FOR TRAIN NUMBER: '$trainNumber'")
                        ridesCollection.document(rideId).update("trainNumber", trainNumber).await()
                        Log.d("RideDebug", "REPOSITORY - trainNumber direct update successful")
                    }
                } catch (e: Exception) {
                    Log.e("RideDebug", "REPOSITORY - Error in direct write for trainNumber", e)
                    Log.e(TAG, "Error in direct write for trainNumber", e)
                }
                
                // Fallback - Just to be extra sure, try a direct write for the "trainName" field
                try {
                    if (trainName.isNotBlank()) {
                        Log.d("RideDebug", "REPOSITORY - DIRECT WRITE FOR TRAIN NAME: '$trainName'")
                        Log.d(TAG, "DIRECT WRITE FOR TRAIN NAME: '$trainName'")
                        ridesCollection.document(rideId).update("trainName", trainName).await()
                        Log.d("RideDebug", "REPOSITORY - trainName direct update successful")
                    }
                } catch (e: Exception) {
                    Log.e("RideDebug", "REPOSITORY - Error in direct write for trainName", e)
                    Log.e(TAG, "Error in direct write for trainName", e)
                }
                
                Log.d("RideDebug", "REPOSITORY - Train details updated successfully")
                Log.d(TAG, "Train details updated successfully for ride: $rideId")
            } catch (e: Exception) {
                Log.e("RideDebug", "REPOSITORY - Error updating train details", e)
                Log.e(TAG, "Error updating train details", e)
                throw e
            }
        }
    }
    
    override suspend fun updateFlightDetails(rideId: String, flightNumber: String, flightName: String) {
        withContext(Dispatchers.IO) {
            try {
                // Log the flight details update
                Log.d("RideDebug", "REPOSITORY - Updating flight details: '${flightNumber}', '${flightName}' for ride ${rideId}")
                Log.d(TAG, "Explicitly updating flight details for ride $rideId")
                Log.d(TAG, "Setting flight number: '$flightNumber', flight name: '$flightName'")
                
                // Create a map with just the flight details
                val updates = mapOf(
                    "flightNumber" to flightNumber,
                    "flightName" to flightName
                )
                
                // Use update to specifically set just these fields
                ridesCollection.document(rideId).update(updates).await()
                Log.d("RideDebug", "REPOSITORY - First update completed")
                
                // Fallback - Just to be extra sure, try a direct write for the "flightNumber" field
                try {
                    if (flightNumber.isNotBlank()) {
                        Log.d("RideDebug", "REPOSITORY - DIRECT WRITE FOR FLIGHT NUMBER: '$flightNumber'")
                        Log.d(TAG, "DIRECT WRITE FOR FLIGHT NUMBER: '$flightNumber'")
                        ridesCollection.document(rideId).update("flightNumber", flightNumber).await()
                        Log.d("RideDebug", "REPOSITORY - flightNumber direct update successful")
                    }
                } catch (e: Exception) {
                    Log.e("RideDebug", "REPOSITORY - Error in direct write for flightNumber", e)
                    Log.e(TAG, "Error in direct write for flightNumber", e)
                }
                
                // Fallback - Just to be extra sure, try a direct write for the "flightName" field
                try {
                    if (flightName.isNotBlank()) {
                        Log.d("RideDebug", "REPOSITORY - DIRECT WRITE FOR FLIGHT NAME: '$flightName'")
                        Log.d(TAG, "DIRECT WRITE FOR FLIGHT NAME: '$flightName'")
                        ridesCollection.document(rideId).update("flightName", flightName).await()
                        Log.d("RideDebug", "REPOSITORY - flightName direct update successful")
                    }
                } catch (e: Exception) {
                    Log.e("RideDebug", "REPOSITORY - Error in direct write for flightName", e)
                    Log.e(TAG, "Error in direct write for flightName", e)
                }
                
                Log.d("RideDebug", "REPOSITORY - Flight details updated successfully")
                Log.d(TAG, "Flight details updated successfully for ride: $rideId")
            } catch (e: Exception) {
                Log.e("RideDebug", "REPOSITORY - Error updating flight details", e)
                Log.e(TAG, "Error updating flight details", e)
                throw e
            }
        }
    }

    override suspend fun removePassenger(rideId: String, passengerEmail: String) {
        val rideRef = ridesCollection.document(rideId)
        try {
            // Use FieldValue.arrayRemove to remove the user from the passengers list
            // We need to query the user profile first to remove the correct object
            Log.d(TAG, "Attempting to remove passenger $passengerEmail from ride $rideId")
            
            // Get the current ride data to find the correct passenger object to remove
            val ride = getRide(rideId)
            if (ride == null) {
                Log.e(TAG, "Ride not found: $rideId")
                throw Exception("Ride not found")
            }
            
            val passengerToRemove = ride.passengers.find { it.email == passengerEmail }
            
            if (passengerToRemove != null) {
                Log.d(TAG, "Found passenger object to remove: ${passengerToRemove.displayName}")
                rideRef.update("passengers", FieldValue.arrayRemove(passengerToRemove))
                    .await()
                Log.i(TAG, "Successfully removed passenger $passengerEmail from ride $rideId")
            } else {
                Log.w(TAG, "Passenger $passengerEmail not found in ride $rideId passengers list.")
                // Optionally throw an error or handle as needed
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing passenger $passengerEmail from ride $rideId", e)
            throw e
        }
    }

    // Function to observe a single ride in real-time
    @Suppress("DEPRECATION")
    override fun observeRide(rideId: String): Flow<Ride?> {
        return ridesCollection.document(rideId)
            .snapshots(MetadataChanges.INCLUDE)
            .map { documentSnapshot: DocumentSnapshot ->
                if (documentSnapshot.exists()) {
                    // Verify the document ID matches the requested ID - should always be true but checking for safety
                    if (documentSnapshot.id != rideId) {
                        return@map null
                    }
                    
                    val ride = documentSnapshot.toObject(Ride::class.java)
                    ride?.let {
                        // Set the rideId manually
                        it.rideId = documentSnapshot.id
                    }
                    
                    ride?.copy(rideId = documentSnapshot.id)
                } else {
                    null // Document doesn't exist or was deleted
                }
            }
    }

    // Helper function to create a notification document
    private suspend fun createNotification(notification: Notification) {
        try {
            Log.d(TAG, "Creating notification: userId=${notification.userId}, type=${notification.type}, message=${notification.message}")
            
            val docRef = notificationsCollection.document() // Auto-generate ID
            // Use a map to ensure correct field names and types, especially for Timestamp and Enum
            val notificationMap = mapOf(
                "id" to docRef.id, // Store the generated ID in the document too
                "userId" to notification.userId,
                "message" to notification.message,
                "timestamp" to FieldValue.serverTimestamp(), // Use server timestamp for consistency
                "type" to notification.type.name, // Store enum as String
                "relatedRideId" to notification.relatedRideId,
                "read" to notification.isRead // Field name is 'read'
            )
            docRef.set(notificationMap).await()
            Log.d(TAG, "Notification created successfully: ${docRef.id} for user ${notification.userId}")
        } catch (e: Exception) {
            Log.e(TAG, "Error creating notification for user ${notification.userId}", e)
            // Decide how to handle this error - maybe log or report it?
        }
    }

    // Function to observe notifications for a specific user
    fun observeUserNotifications(userId: String): Flow<List<Notification>> {
        Log.d(TAG, "Observing notifications for user: $userId")
        
        // Make sure userId is not empty
        if (userId.isBlank()) {
            Log.e(TAG, "Cannot observe notifications for blank userId")
            return flowOf(emptyList())
        }
        
        return flow {
            try {
                // First, try the simple get() approach instead of real-time updates
                // This doesn't require the composite index
                val snapshot = notificationsCollection
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()
                
                Log.d(TAG, "Fetched notifications once: ${snapshot.documents.size} documents")
                
                val notifications = snapshot.documents.mapNotNull { doc ->
                    try {
                        // Manually map to handle potential enum/timestamp issues
                        val data = doc.data
                        if (data == null) {
                            Log.w(TAG, "Document ${doc.id} has no data")
                            return@mapNotNull null
                        }
                        
                        val uid = data["userId"] as? String
                        if (uid == null) {
                            Log.w(TAG, "Document ${doc.id} has no userId field")
                            return@mapNotNull null
                        }
                        
                        val typeString = data["type"] as? String
                        val type = try {
                            typeString?.let { NotificationType.valueOf(it) } ?: NotificationType.OTHER
                        } catch (e: IllegalArgumentException) {
                            Log.w(TAG, "Invalid notification type string: $typeString, defaulting to OTHER")
                            NotificationType.OTHER
                        }
                        
                        val timestamp = data["timestamp"] as? Timestamp ?: Timestamp.now()
                        val message = data["message"] as? String ?: ""
                        val relatedRideId = data["relatedRideId"] as? String
                        val isRead = data["read"] as? Boolean ?: false
                        
                        Notification(
                            id = doc.id,
                            userId = uid,
                            message = message,
                            timestamp = timestamp,
                            type = type,
                            relatedRideId = relatedRideId,
                            isRead = isRead
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing notification document: ${doc.id}", e)
                        null // Skip invalid documents
                    }
                }
                
                // Sort manually since we can't use orderBy without the index
                val sortedNotifications = notifications.sortedByDescending { it.timestamp.toDate() }
                
                Log.d(TAG, "Successfully mapped and sorted ${sortedNotifications.size} notification objects")
                emit(sortedNotifications)
                
                // Set up a listener for just new notifications without ordering
                // This is a fallback that will work without the composite index
                try {
                    notificationsCollection
                        .whereEqualTo("userId", userId)
                        .addSnapshotListener { newSnapshot, error ->
                            if (error != null) {
                                Log.e(TAG, "Error listening for new notifications", error)
                                return@addSnapshotListener
                            }
                            
                            if (newSnapshot != null && !newSnapshot.isEmpty) {
                                Log.d(TAG, "New notification snapshot received with ${newSnapshot.documents.size} documents")
                                
                                // Launch a coroutine to fetch all notifications again
                                CoroutineScope(Dispatchers.IO).launch {
                                    try {
                                        val refreshedSnapshot = notificationsCollection
                                            .whereEqualTo("userId", userId)
                                            .get()
                                            .await()
                                        
                                        val refreshedNotifications = refreshedSnapshot.documents.mapNotNull { doc ->
                                            try {
                                                val data = doc.data ?: return@mapNotNull null
                                                val uid = data["userId"] as? String ?: return@mapNotNull null
                                                val typeString = data["type"] as? String
                                                val type = try {
                                                    typeString?.let { NotificationType.valueOf(it) } ?: NotificationType.OTHER
                                                } catch (e: IllegalArgumentException) {
                                                    NotificationType.OTHER
                                                }
                                                val timestamp = data["timestamp"] as? Timestamp ?: Timestamp.now()
                                                val message = data["message"] as? String ?: ""
                                                val relatedRideId = data["relatedRideId"] as? String
                                                val isRead = data["read"] as? Boolean ?: false
                                                
                                                Notification(
                                                    id = doc.id,
                                                    userId = uid,
                                                    message = message,
                                                    timestamp = timestamp,
                                                    type = type,
                                                    relatedRideId = relatedRideId,
                                                    isRead = isRead
                                                )
                                            } catch (e: Exception) {
                                                Log.e(TAG, "Error parsing notification in refresh: ${doc.id}", e)
                                                null
                                            }
                                        }
                                        
                                        // Sort manually
                                        val sortedRefreshedNotifications = refreshedNotifications.sortedByDescending { 
                                            it.timestamp.toDate() 
                                        }
                                        
                                        Log.d(TAG, "Refreshed notifications: ${sortedRefreshedNotifications.size}")
                                        emit(sortedRefreshedNotifications)
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error refreshing notifications", e)
                                    }
                                }
                            }
                        }
                } catch (e: Exception) {
                    Log.e(TAG, "Error setting up notification listener", e)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in observeUserNotifications", e)
                emit(emptyList<Notification>())
            }
        }.catch { exception ->
            Log.e(TAG, "Exception in notification flow", exception)
            emit(emptyList<Notification>())
        }
    }
} 
 