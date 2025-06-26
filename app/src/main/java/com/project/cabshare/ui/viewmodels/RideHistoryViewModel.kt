package com.project.cabshare.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.project.cabshare.data.FirestoreRideHistoryRepositoryImpl
import com.project.cabshare.data.RideHistoryRepository
import com.project.cabshare.models.RideHistory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.util.Date

class RideHistoryViewModel(
    private val repository: RideHistoryRepository,
    private val userEmail: String
) : ViewModel() {
    private val TAG = "RideHistoryViewModel"
    
    private val _rideHistory = MutableStateFlow<List<RideHistory>>(emptyList())
    val rideHistory: StateFlow<List<RideHistory>> = _rideHistory
    
    // Add state for current ride details
    private val _currentRideHistory = MutableStateFlow<RideHistory?>(null)
    val currentRideHistory: StateFlow<RideHistory?> = _currentRideHistory
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    
    init {
        loadUserRideHistory()
    }
    
    fun loadUserRideHistory() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                Log.d(TAG, "Starting to load ride history for user: $userEmail")
                repository.getUserRideHistory(userEmail)
                    .catch { e ->
                        Log.e(TAG, "Error loading ride history", e)
                        _error.value = "Failed to load ride history: ${e.message}"
                        _rideHistory.value = emptyList()
                    }
                    .collect { history ->
                        Log.d(TAG, "Received ride history update with ${history.size} rides")
                        _rideHistory.value = history
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error in loadUserRideHistory", e)
                _error.value = "Failed to load ride history: ${e.message}"
                _rideHistory.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun loadRideHistoryDetails(rideId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _currentRideHistory.value = null // Clear previous ride details
            
            try {
                Log.d(TAG, "Loading ride history details for ride: $rideId")
                val rideDetails = repository.getRideHistory(rideId)
                if (rideDetails != null) {
                    Log.d(TAG, "Successfully loaded ride history details: $rideDetails")
                    _currentRideHistory.value = rideDetails
                    _error.value = null
                } else {
                    Log.e(TAG, "Ride history not found for ID: $rideId")
                    _error.value = "Ride not found"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading ride history details", e)
                _error.value = "Failed to load ride details: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun refreshRideHistory() {
        loadUserRideHistory()
    }
    
    fun deleteRideHistory(rideId: String) {
        viewModelScope.launch {
            try {
                repository.deleteFromHistory(rideId, userEmail)
                // Update the UI by removing the deleted ride
                _rideHistory.value = _rideHistory.value.filter { it.rideId != rideId }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting ride history", e)
                _error.value = "Failed to delete ride: ${e.message}"
            }
        }
    }
    
    fun clearError() {
        _error.value = null
    }
    
    fun getRideHistoryBetweenDates(startDate: Date, endDate: Date) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                val history = repository.getRideHistoryBetweenDates(startDate, endDate)
                _rideHistory.value = history
            } catch (e: Exception) {
                Log.e(TAG, "Error getting ride history between dates", e)
                _error.value = "Failed to load ride history: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    companion object {
        fun provideFactory(
            userEmail: String,
            repository: RideHistoryRepository = FirestoreRideHistoryRepositoryImpl()
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(RideHistoryViewModel::class.java)) {
                    return RideHistoryViewModel(repository, userEmail) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
} 