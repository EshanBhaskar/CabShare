package com.project.cabshare.ui.viewmodels

import android.app.Application
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.project.cabshare.auth.AuthViewModel
import com.project.cabshare.data.FirestoreRideRepositoryImpl
import com.project.cabshare.data.FirestoreUserRepository
import com.project.cabshare.data.JoinRequest
import com.project.cabshare.data.RideRepository
import com.project.cabshare.data.RequestStatus
import com.project.cabshare.data.RideFullException
import com.project.cabshare.models.Ride
import com.project.cabshare.models.RideDirection
import com.project.cabshare.models.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import com.project.cabshare.data.Notification

class RideViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "RideViewModel"
    
    // Maximum number of rides a user can create
    private val MAX_RIDES_PER_USER = 5
    
    // Repositories
    private val rideRepository: RideRepository = FirestoreRideRepositoryImpl()
    private val userRepository = FirestoreUserRepository()
    
    // Ride-related state
    private val _rides = MutableStateFlow<List<Ride>>(emptyList())
    val rides: StateFlow<List<Ride>> = _rides
    
    private val _destinationFilter = MutableStateFlow<String?>(null)
    val destinationFilter = _destinationFilter.asStateFlow()
    
    // Add date filter
    private val _dateFilter = MutableStateFlow<Date?>(null)
    val dateFilter = _dateFilter.asStateFlow()
    
    private val _filteredRides = MutableStateFlow<List<Ride>>(emptyList())
    val filteredRides = _filteredRides.asStateFlow()
    
    private val _currentRide = MutableStateFlow<Ride?>(null)
    val currentRide: StateFlow<Ride?> = _currentRide
    
    private val _joinRequests = MutableStateFlow<List<JoinRequest>>(emptyList())
    val joinRequests: StateFlow<List<JoinRequest>> = _joinRequests
    
    // State for creator profile
    private val _creatorProfile = MutableStateFlow<UserProfile?>(null)
    val creatorProfile: StateFlow<UserProfile?> = _creatorProfile
    
    // State for fetched passenger profiles
    private val _refreshedPassengerProfiles = MutableStateFlow<Map<String, UserProfile>>(emptyMap())
    val refreshedPassengerProfiles: StateFlow<Map<String, UserProfile>> = _refreshedPassengerProfiles
    
    // UI state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    // Make _error accessible but keep the read-only error flow
    val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    // Add StateFlow for notifications
    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()
    
    // Ride creation form state
    val rideSource = mutableStateOf("")
    val rideDestination = mutableStateOf("")
    val rideDate = mutableStateOf(Date())
    val rideTime = mutableStateOf(Date())
    val rideMaxPassengers = mutableStateOf("4")
    val rideNotes = mutableStateOf("")
    val rideDirection = mutableStateOf(RideDirection.FROM_IITP)
    val rideTrainNumber = mutableStateOf("")
    val rideTrainName = mutableStateOf("")
    val rideFlightNumber = mutableStateOf("")
    val rideFlightName = mutableStateOf("")
    
    // Get rides by direction
    fun getRidesByDirection(direction: RideDirection) {
        _isLoading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                val directionRides = rideRepository.getRidesByDirection(direction)
                
                // Update rides and apply filters
                _rides.value = directionRides
                applyFilters()
            } catch (e: Exception) {
                Log.e(TAG, "Error loading rides by direction", e)
                _error.value = "Failed to load rides: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Observe rides by direction with filtering
    fun observeRidesByDirection(direction: RideDirection) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            rideDirection.value = direction
            
            try {
                rideRepository.observeRidesByDirection(direction)
                    .catch { e -> 
                        Log.e(TAG, "Error observing rides by direction", e)
                        _error.value = "Error observing rides: ${e.message}"
                        _isLoading.value = false
                    }
                    .collect { directionRides ->
                        _rides.value = directionRides
                        applyFilters()
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in observe flow", e)
                _error.value = "Error in ride observation: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    // Get user rides (created by the user or joined by the user)
    fun getUserRides(email: String) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val userRides = rideRepository.getUserRides(email)
                _rides.value = userRides
            } catch (e: Exception) {
                Log.e(TAG, "Error loading user rides", e)
                _error.value = "Failed to load your rides: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Observe user rides (real-time updates)
    fun observeUserRides(email: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null // Clear error when starting observation
            // Call the correct repository method observeUserRides
            rideRepository.observeUserRides(email)
                .catch { e ->
                    Log.e(TAG, "Error observing user rides", e)
                    _error.value = "Error observing rides: ${e.message}"
                    _rides.value = emptyList() // Clear rides on error
                    _isLoading.value = false
                }
                .collect { userRides ->
                    _rides.value = userRides // Update the main rides list
                    // If needed, apply filters here too, or ensure MyRidesScreen observes _rides directly
                    _isLoading.value = false
                }
        }
    }
    
    // Get a specific ride by ID
    fun observeRideDetails(rideId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null // Clear previous errors
            
            // Clear current ride data to prevent stale data from being displayed
            // and to avoid confusion when switching between rides
            _currentRide.value = null
            
            rideRepository.observeRide(rideId)
                .catch { e ->
                    Log.e(TAG, "Error observing ride $rideId", e)
                    _error.value = "Error observing ride details: ${e.message}"
                    _isLoading.value = false
                    _currentRide.value = null // Set to null on error
                }
                .collect { ride ->
                    if (ride != null) {
                        // Verify the ride ID matches what we requested
                        if (ride.rideId != rideId) {
                            // Don't update the UI with incorrect data
                            return@collect
                        }
                    }
                    
                    _currentRide.value = ride
                    if (ride == null) {
                        _error.value = "Ride not found or was deleted."
                    } else {
                        // Fetch related data only if ride exists
                        fetchCreatorProfile(ride.creatorEmail)
                        if (ride.passengers.isNotEmpty()) {
                            fetchPassengerProfiles(ride.passengers.map { it.email })
                        }
                        _error.value = null // Clear error if ride is loaded
                    }
                    _isLoading.value = false
                }
        }
    }
    
    // Get the count of rides created by the user
    suspend fun getUserCreatedRidesCount(email: String): Int {
        return try {
            val userRides = rideRepository.getUserRides(email)
            // Filter to only include rides created by this user (not joined rides)
            userRides.filter { it.creator == email || it.creatorEmail == email }.size
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user created rides count", e)
            0 // Default to 0 on error to avoid blocking ride creation
        }
    }
    
    // Check if user has reached their ride limit
    suspend fun hasReachedRideLimit(email: String): Boolean {
        val count = getUserCreatedRidesCount(email)
        return count >= MAX_RIDES_PER_USER
    }
    
    // Create a new ride
    fun createRide(userEmail: String, onSuccess: (String) -> Unit) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                // Log the values in ViewModel state for debugging
                Log.d("RideDebug", "VIEWMODEL - Raw train values: '${rideTrainNumber.value}', '${rideTrainName.value}'")
                Log.d("RideDebug", "VIEWMODEL - Raw flight values: '${rideFlightNumber.value}', '${rideFlightName.value}'")
                Log.d("RideDebug", "VIEWMODEL - Raw notes value: '${rideNotes.value}'")
                
                Log.d("RideDebug", "VIEWMODEL - Train values before creating ride: '${rideTrainNumber.value}', '${rideTrainName.value}'")
                Log.d("RideDebug", "VIEWMODEL - Flight values before creating ride: '${rideFlightNumber.value}', '${rideFlightName.value}'")
                Log.d("RideDebug", "VIEWMODEL - Notes value before creating ride: '${rideNotes.value}'")
                
                // Get the current date/time
                val calendar = Calendar.getInstance()
                calendar.time = rideDate.value
                val timeCalendar = Calendar.getInstance()
                timeCalendar.time = rideTime.value
                calendar.set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY))
                calendar.set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE))
                
                // Save current train and flight values to local variables to prevent them from changing during async operations
                val currentTrainNumber = rideTrainNumber.value.trim()
                val currentTrainName = rideTrainName.value.trim()
                val currentFlightNumber = rideFlightNumber.value.trim()
                val currentFlightName = rideFlightName.value.trim()
                val currentNotes = rideNotes.value
                
                // Create the ride object
                val ride = Ride(
                    creator = userEmail,
                    direction = rideDirection.value,
                    source = rideSource.value,
                    destination = rideDestination.value,
                    dateTime = calendar.time,
                    maxPassengers = rideMaxPassengers.value.toIntOrNull() ?: 4,
                    notes = currentNotes,
                    trainNumber = currentTrainNumber,
                    trainName = currentTrainName,
                    flightNumber = currentFlightNumber,
                    flightName = currentFlightName,
                    status = "SCHEDULED"
                )
                
                // Debug log for the ride object
                Log.d("RideDebug", "VIEWMODEL - Created ride object with train details: '${ride.trainNumber}', '${ride.trainName}'")
                Log.d("RideDebug", "VIEWMODEL - Created ride object with flight details: '${ride.flightNumber}', '${ride.flightName}'")
                Log.d("RideDebug", "VIEWMODEL - Created ride object with notes: '${ride.notes}'")
                
                // Debug log for optional fields
                Log.d(TAG, "Creating ride with train #: '${ride.trainNumber}', train name: '${ride.trainName}'")
                Log.d(TAG, "Creating ride with flight #: '${ride.flightNumber}', flight name: '${ride.flightName}'")
                Log.d(TAG, "Creating ride with notes: '${ride.notes}'")
                
                // Ensure values are actually being set
                if (ride.trainNumber.isNotBlank() || ride.trainName.isNotBlank()) {
                    Log.d(TAG, "This ride has train details that should be saved!")
                }
                
                val rideId = rideRepository.createRide(ride)
                Log.d("RideDebug", "VIEWMODEL - Ride created with ID: $rideId")
                
                // Reset form fields
                resetRideForm()
                
                // Call the success callback with the ride ID
                onSuccess(rideId)
            } catch (e: Exception) {
                Log.e(TAG, "Error creating ride", e)
                Log.e("RideDebug", "VIEWMODEL - Error creating ride: ${e.message}", e)
                _error.value = "Failed to create ride: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Delete a ride
    fun deleteRide(rideId: String, onSuccess: () -> Unit) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                rideRepository.deleteRide(rideId)
                onSuccess()
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting ride", e)
                _error.value = "Failed to delete ride: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Request to join a ride
    fun requestToJoinRide(rideId: String, userEmail: String, onSuccess: (String) -> Unit) {
        _isLoading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                // Fetch the complete UserProfile first
                val userProfile = userRepository.getUserProfile(userEmail)
                if (userProfile == null) {
                    throw Exception("User profile not found.")
                }
                
                // Now make the request with the full profile
                val requestId = rideRepository.requestToJoinRide(rideId, userProfile)
                onSuccess(requestId)
                // Refresh ride details to show pending request
                observeRideDetails(rideId)
            } catch (e: Exception) {
                Log.e(TAG, "Error requesting to join ride", e)
                _error.value = "Failed to send join request: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Accept a join request
    fun acceptJoinRequest(rideId: String, requestId: String, onResult: (Boolean) -> Unit = { _ -> }) {
        _isLoading.value = true
        _error.value = null // Clear previous errors

        viewModelScope.launch {
            try {
                // First, check if the request still exists and is still pending (Good practice)
                val currentRide = rideRepository.getRide(rideId)
                val requestStillPending = currentRide?.pendingRequests?.any { 
                    it.requestId == requestId && it.status == RequestStatus.PENDING 
                } ?: false
                
                if (!requestStillPending) {
                    Log.w(TAG, "Join request $requestId is no longer valid or pending for accept action.")
                    _error.value = "This request is no longer valid." // Message for Snackbar
                    observeRideDetails(rideId) // Refresh the ride data
                    onResult(false)
                    return@launch
                }

                // Proceed with accepting
                rideRepository.acceptJoinRequest(rideId, requestId)
                // Refresh the current ride to see the updated passengers list
                observeRideDetails(rideId)
                onResult(true) // Report success

            } catch (e: RideFullException) { // *** Catch specific exception ***
                Log.w(TAG, "Failed to accept join request: ${e.message}")
                // *** Set specific message for Snackbar ***
                _error.value = "Ride is full. Cannot accept more passengers."
                onResult(false) // Report failure

            } catch (e: Exception) { // Catch other potential errors
                _error.value = "Error accepting join request: ${e.message}" // General error
                Log.e(TAG, "Error accepting join request", e)
                onResult(false) // Report failure

            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Reject a join request
    fun rejectJoinRequest(rideId: String, requestId: String, onResult: (Boolean) -> Unit = { _ -> }) {
        _isLoading.value = true
        _error.value = null
        
        viewModelScope.launch {
            try {
                // First, check if the request still exists and is still pending
                val currentRide = rideRepository.getRide(rideId)
                val requestStillPending = currentRide?.pendingRequests?.any { 
                    it.requestId == requestId && it.status == RequestStatus.PENDING 
                } ?: false
                
                if (!requestStillPending) {
                    Log.w(TAG, "Join request $requestId is no longer valid or pending")
                    _error.value = "This request is no longer valid. It may have been canceled."
                    observeRideDetails(rideId) // Refresh the ride data
                    onResult(false)
                    return@launch
                }
                
                // If request is still valid, proceed with rejecting
                rideRepository.rejectJoinRequest(rideId, requestId)
                // Refresh the current ride to see the updated request list
                observeRideDetails(rideId)
                onResult(true)
            } catch (e: Exception) {
                _error.value = "Error rejecting join request: ${e.message}"
                Log.e(TAG, "Error rejecting join request", e)
                onResult(false)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Cancel a user's own join request
    fun cancelJoinRequest(rideId: String, userEmail: String, onComplete: () -> Unit = {}) {
        _isLoading.value = true
        _error.value = null
        
        viewModelScope.launch {
            try {
                rideRepository.cancelJoinRequest(rideId, userEmail)
                // Refresh the current ride to see the updated request list
                observeRideDetails(rideId)
                onComplete()
            } catch (e: Exception) {
                _error.value = "Error cancelling join request: ${e.message}"
                Log.e(TAG, "Error cancelling join request", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Get join requests for a user
    fun getUserJoinRequests(email: String) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val requests = rideRepository.getUserJoinRequests(email)
                _joinRequests.value = requests
            } catch (e: Exception) {
                Log.e(TAG, "Error loading join requests", e)
                _error.value = "Failed to load join requests: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Reset ride form fields
    private fun resetRideForm() {
        rideSource.value = ""
        rideDestination.value = ""
        rideDate.value = Date()
        rideTime.value = Date()
        rideMaxPassengers.value = "4"
        rideNotes.value = ""
        rideTrainNumber.value = ""
        rideTrainName.value = ""
        rideFlightNumber.value = ""
        rideFlightName.value = ""
    }
    
    // Clear error
    fun clearError() {
        _error.value = null
    }
    
    // Function to set destination filter
    fun setDestinationFilter(destination: String) {
        _destinationFilter.value = destination
        // Apply filter to current rides
        applyFilters()
    }
    
    // Function to clear destination filter
    fun clearDestinationFilter() {
        _destinationFilter.value = null
        // Reset to show all rides
        _filteredRides.value = _rides.value
    }
    
    // Helper function to apply filters
    private fun applyFilters() {
        val currentFilter = _destinationFilter.value
        val currentDirection = rideDirection.value
        val currentDateFilter = _dateFilter.value
        val allRides = _rides.value
        
        var filteredResult = allRides
        
        // Apply destination filter if set
        if (currentFilter != null) {
            filteredResult = filteredResult.filter { ride ->
                when (currentDirection) {
                    RideDirection.FROM_IITP -> ride.destination == currentFilter
                    RideDirection.TO_IITP -> ride.source == currentFilter
                }
            }
        }
        
        // Apply date filter if set
        if (currentDateFilter != null) {
            filteredResult = filteredResult.filter { ride ->
                // Compare year, month, and day only (ignoring time)
                val rideCalendar = Calendar.getInstance().apply { time = ride.dateTime }
                val filterCalendar = Calendar.getInstance().apply { time = currentDateFilter }
                
                rideCalendar.get(Calendar.YEAR) == filterCalendar.get(Calendar.YEAR) &&
                rideCalendar.get(Calendar.MONTH) == filterCalendar.get(Calendar.MONTH) &&
                rideCalendar.get(Calendar.DAY_OF_MONTH) == filterCalendar.get(Calendar.DAY_OF_MONTH)
            }
        }
        
        _filteredRides.value = filteredResult
    }
    
    // Function to set date filter
    fun setDateFilter(date: Date) {
        _dateFilter.value = date
        // Apply filter to current rides
        applyFilters()
    }
    
    // Function to clear date filter
    fun clearDateFilter() {
        _dateFilter.value = null
        // Apply remaining filters
        applyFilters()
    }
    
    // Check ride limit and update UI accordingly
    fun checkRideLimitAndShowDialog(userEmail: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val count = getUserCreatedRidesCount(userEmail)
                if (count >= MAX_RIDES_PER_USER) {
                    // _error.value = "You cannot create more than $MAX_RIDES_PER_USER rides. Please delete some of your existing rides first." // Don't set the general error here
                    onResult(false)
                } else {
                    onResult(true)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking ride limit", e)
                _error.value = "Failed to check ride limit: ${e.message}" // Set error for actual exceptions
                onResult(true) // Allow creation on error checking limit, but show the error
            }
        }
    }
    
    // Update train details for a specific ride
    fun updateTrainDetails(rideId: String, trainNumber: String, trainName: String, refreshRide: Boolean = false) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                rideRepository.updateTrainDetails(rideId, trainNumber, trainName)
                
                // Only refresh the ride if specifically requested
                if (refreshRide) {
                    observeRideDetails(rideId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating train details", e)
                _error.value = "Failed to update train details: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Update flight details for a specific ride
    fun updateFlightDetails(rideId: String, flightNumber: String, flightName: String, refreshRide: Boolean = false) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                rideRepository.updateFlightDetails(rideId, flightNumber, flightName)
                
                // Only refresh the ride if specifically requested
                if (refreshRide) {
                    observeRideDetails(rideId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating flight details", e)
                _error.value = "Failed to update flight details: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Update comprehensive ride details
    fun updateRideDetails(ride: Ride) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                Log.d(TAG, "Updating ride details for ride ${ride.rideId}")
                
                // Update the ride document
                rideRepository.updateRide(ride)
                
                // Also force update train details if present (to work around Firestore issues)
                if (!ride.trainNumber.isNullOrBlank() || !ride.trainName.isNullOrBlank()) {
                    Log.d(TAG, "Also forcing train details update: ${ride.trainNumber} - ${ride.trainName}")
                    rideRepository.updateTrainDetails(ride.rideId, ride.trainNumber, ride.trainName)
                }
                
                // Also force update flight details if present
                if (!ride.flightNumber.isNullOrBlank() || !ride.flightName.isNullOrBlank()) {
                    Log.d(TAG, "Also forcing flight details update: ${ride.flightNumber} - ${ride.flightName}")
                    rideRepository.updateFlightDetails(ride.rideId, ride.flightNumber, ride.flightName)
                }
                
                // Refresh the ride data
                observeRideDetails(ride.rideId)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating ride details", e)
                _error.value = "Failed to update ride details: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Remove passenger (Creator action) or leave ride (Participant action)
    fun removeOrLeavePassenger(rideId: String, passengerEmail: String, onSuccess: () -> Unit = {}) {
        _isLoading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                Log.d(TAG, "Removing/Leaving passenger: $passengerEmail from ride: $rideId")
                rideRepository.removePassenger(rideId, passengerEmail)
                Log.i(TAG, "Passenger $passengerEmail successfully removed/left ride $rideId")
                // Optionally, refresh the specific ride data after removal
                observeRideDetails(rideId) 
                onSuccess()
            } catch (e: Exception) {
                Log.e(TAG, "Error removing/leaving passenger $passengerEmail from ride $rideId", e)
                _error.value = "Failed to update ride: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Fetch creator profile
    fun fetchCreatorProfile(email: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Fetching profile for creator: $email")
                _creatorProfile.value = userRepository.getUserProfile(email)
                Log.d(TAG, "Fetched creator profile: ${_creatorProfile.value?.displayName}")
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching creator profile for $email", e)
                _creatorProfile.value = null // Reset on error
            }
        }
    }
    
    // Fetch profiles for a list of passenger emails
    fun fetchPassengerProfiles(passengerEmails: List<String>) {
        if (passengerEmails.isEmpty()) {
            _refreshedPassengerProfiles.value = emptyMap() // Clear if no passengers
            return
        }
        viewModelScope.launch {
            try {
                Log.d(TAG, "Fetching profiles for passengers: $passengerEmails")
                val profileMap = mutableMapOf<String, UserProfile>()
                passengerEmails.forEach { email ->
                    val profile = userRepository.getUserProfile(email)
                    if (profile != null) {
                        profileMap[email] = profile
                    } else {
                        Log.w(TAG, "Could not fetch profile for passenger: $email")
                        // Optionally, keep the old snapshot data by not adding null
                    }
                }
                _refreshedPassengerProfiles.value = profileMap
                Log.d(TAG, "Finished fetching passenger profiles. Found: ${profileMap.size}")
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching passenger profiles", e)
                // Decide error handling - clear map or keep potentially stale data?
                // Clearing might be safer to avoid showing incorrect data.
                 _refreshedPassengerProfiles.value = emptyMap()
            }
        }
    }
    
    // Update notes for a specific ride
    fun updateNotes(rideId: String, notes: String, refreshRide: Boolean = true) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                Log.d(TAG, "Updating notes for ride $rideId")
                Log.d(TAG, "Notes: '$notes'")
                
                // Get the current ride
                val currentRide = rideRepository.getRide(rideId)
                
                if (currentRide != null) {
                    // Create an updated ride with the new notes
                    val updatedRide = currentRide.copy(notes = notes)
                    
                    // Update the entire ride
                    rideRepository.updateRide(updatedRide)
                    
                    // Also force update train details if present to prevent them from being lost
                    if (!currentRide.trainNumber.isNullOrBlank() || !currentRide.trainName.isNullOrBlank()) {
                        Log.d(TAG, "Also preserving train details during note update: ${currentRide.trainNumber} - ${currentRide.trainName}")
                        rideRepository.updateTrainDetails(rideId, currentRide.trainNumber, currentRide.trainName)
                    }
                    
                    // Also force update flight details if present to prevent them from being lost
                    if (!currentRide.flightNumber.isNullOrBlank() || !currentRide.flightName.isNullOrBlank()) {
                        Log.d(TAG, "Also preserving flight details during note update: ${currentRide.flightNumber} - ${currentRide.flightName}")
                        rideRepository.updateFlightDetails(rideId, currentRide.flightNumber, currentRide.flightName)
                    }
                    
                    // Refresh the ride if requested
                    if (refreshRide) {
                        observeRideDetails(rideId)
                    }
                } else {
                    _error.value = "Failed to update notes: Ride not found"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating notes", e)
                _error.value = "Failed to update notes: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Add function to observe notifications
    fun observeUserNotifications(userId: String) {
        viewModelScope.launch {
            Log.d(TAG, "Starting to observe notifications for user: $userId")
            
            // Ensure repository is FirestoreRideRepositoryImpl to access the method
            val firestoreRepo = rideRepository as? FirestoreRideRepositoryImpl
            if (firestoreRepo == null) {
                Log.e(TAG, "RideRepository is not FirestoreRideRepositoryImpl, cannot observe notifications")
                _error.value = "Internal error: Cannot load notifications."
                return@launch
            }

            try {
                // Observe notifications using the repository method
                firestoreRepo.observeUserNotifications(userId)
                    .catch { e ->
                        Log.e(TAG, "Error observing notifications for user $userId", e)
                        _error.value = "Failed to load notifications: ${e.message}"
                        _notifications.value = emptyList() // Clear notifications on error
                    }
                    .collect { userNotifications ->
                        Log.d(TAG, "Received ${userNotifications.size} notifications for user $userId")
                        if (userNotifications.isNotEmpty()) {
                            // Log details of first few notifications for debugging
                            userNotifications.take(3).forEach { notification ->
                                Log.d(TAG, "Notification: id=${notification.id}, type=${notification.type}, " +
                                        "message=${notification.message}, timestamp=${notification.timestamp}")
                            }
                        } else {
                            Log.d(TAG, "No notifications found for user $userId")
                        }
                        
                        _notifications.value = userNotifications
                        _error.value = null // Clear error on successful update
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error in observeUserNotifications", e)
                _error.value = "An unexpected error occurred: ${e.message}"
                _notifications.value = emptyList()
            }
        }
    }
} 