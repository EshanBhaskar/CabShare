package com.project.cabshare.ui.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.project.cabshare.auth.UserInfo
import com.project.cabshare.data.FirestoreUserRepository
import com.project.cabshare.models.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing user profile information
 */
class UserViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "UserViewModel"
    
    // Repository
    private val userRepository = FirestoreUserRepository()
    
    // User information
    private val _userInfo = MutableStateFlow<UserInfo?>(null)
    val userInfo: StateFlow<UserInfo?> = _userInfo
    
    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    // Error state
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    
    /**
     * Set user information
     */
    fun setUserInfo(userInfo: UserInfo) {
        _userInfo.value = userInfo
    }
    
    /**
     * Get user profile from Firestore
     */
    fun getUserProfile(userId: String) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val profile = userRepository.getUserProfile(userId)
                if (profile != null) {
                    _userInfo.value?.let { currentUser ->
                        val updatedUserInfo = UserInfo(
                            email = currentUser.email,
                            displayName = currentUser.displayName,
                            rollNumber = currentUser.rollNumber,
                            userProfile = profile
                        )
                        _userInfo.value = updatedUserInfo
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting user profile", e)
                _error.value = "Failed to load user profile: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Save user profile to Firestore
     */
    fun saveUserProfile(profile: UserProfile) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                userRepository.saveUserProfile(profile)
                _userInfo.value?.let { currentUser ->
                    val updatedUserInfo = UserInfo(
                        email = currentUser.email,
                        displayName = currentUser.displayName,
                        rollNumber = currentUser.rollNumber,
                        userProfile = profile
                    )
                    _userInfo.value = updatedUserInfo
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving user profile", e)
                _error.value = "Failed to save user profile: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Update user profile in Firestore
     */
    fun updateUserProfile(userId: String, updates: Map<String, Any>) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                // We'll use a simple approach until we have updateUserProfile in repository
                val existingProfile = userRepository.getUserProfile(userId)
                if (existingProfile != null) {
                    // Create updated profile with applicable fields
                    val updatedProfile = existingProfile.copy(
                        // Update only what's in the updates map
                        displayName = updates["displayName"] as? String ?: existingProfile.displayName,
                        phoneNumber = updates["phoneNumber"] as? String ?: existingProfile.phoneNumber,
                        rollNumber = updates["rollNumber"] as? String ?: existingProfile.rollNumber
                    )
                    userRepository.saveUserProfile(updatedProfile)
                }
                
                // Refresh the profile
                getUserProfile(userId)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating user profile", e)
                _error.value = "Failed to update user profile: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Clear error
     */
    fun clearError() {
        _error.value = null
    }
} 