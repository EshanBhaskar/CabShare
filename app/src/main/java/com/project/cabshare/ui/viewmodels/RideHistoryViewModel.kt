package com.project.cabshare.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.cabshare.data.FirestoreRideHistoryRepositoryImpl
import com.project.cabshare.data.RideHistoryRepository
import com.project.cabshare.models.RideHistory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.util.Date

class RideHistoryViewModel : ViewModel() {
    private val repository: RideHistoryRepository = FirestoreRideHistoryRepositoryImpl()
    private val TAG = "RideHistoryViewModel"
    
    private val _rideHistory = MutableStateFlow<List<RideHistory>>(emptyList())
    val rideHistory: StateFlow<List<RideHistory>> = _rideHistory
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    
    fun loadUserRideHistory(userEmail: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                repository.getUserRideHistory(userEmail)
                    .catch { e ->
                        Log.e(TAG, "Error loading ride history", e)
                        _error.value = "Failed to load ride history: ${e.message}"
                    }
                    .collect { history ->
                        _rideHistory.value = history
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error in loadUserRideHistory", e)
                _error.value = "Failed to load ride history: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun deleteRideHistory(rideId: String) {
        viewModelScope.launch {
            try {
                repository.deleteFromHistory(rideId)
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
} 