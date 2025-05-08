package com.project.cabshare.auth

import android.app.Activity
import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.project.cabshare.data.FirestoreUserRepository
import com.project.cabshare.models.UserProfile

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "AuthViewModel"
    private val authService = AuthService(application)
    private val firestoreRepository = FirestoreUserRepository()
    
    // Authentication state
    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState
    
    // User information
    private val _userInfo = MutableStateFlow<UserInfo?>(null)
    val userInfo: StateFlow<UserInfo?> = _userInfo
    
    // Profile completion state
    private val _isProfileCompleted = MutableStateFlow<Boolean>(false)
    val isProfileCompleted: StateFlow<Boolean> = _isProfileCompleted
    
    init {
        // Log.d(TAG, "Initializing AuthViewModel")
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Initialize MSAL and load authentication state on a background thread
                authService.init()
                // Log.d(TAG, "Auth service initialized successfully: $initialized")
                
                // Check if user is authenticated (this runs on background thread due to withContext in authService)
                val isAuthenticated = authService.isAuthenticated()
                
                // Update state on main thread
                withContext(Dispatchers.Main) {
                    if (isAuthenticated) {
                        _authState.value = AuthState.Authenticated
                        
                        // Collect user info
                        viewModelScope.launch {
                            authService.getUserInfo()
                                .catch { e -> 
                                    _authState.value = AuthState.Error("Error loading user info: ${e.message}")
                                }
                                .collectLatest { userInfo ->
                                    _userInfo.value = userInfo
                                    // Check if profile is completed in Firestore
                                    checkFirestoreProfileCompletion(userInfo?.email ?: "")
                                }
                        }
                    } else {
                        _authState.value = AuthState.Unauthenticated
                    }
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Error during initialization: ${e.message}")
            }
        }
    }
    
    // Sign in the user
    fun signIn(activity: Activity) {
        _authState.value = AuthState.Loading
        
        viewModelScope.launch {
            try {
                val result = authService.signIn(activity)
                
                if (result.isSuccess) {
                    val userInfo = result.getOrNull()
                    if (userInfo != null) {
                        _userInfo.value = userInfo
                        _authState.value = AuthState.Authenticated
                        
                        // Create user profile in Firestore if it doesn't exist
                        createOrUpdateUserProfile(userInfo)
                        
                        // Check profile completion
                        checkFirestoreProfileCompletion(userInfo.email)
                    } else {
                        _authState.value = AuthState.Error("Authentication failed: No user info returned")
                    }
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Unknown error"
                    _authState.value = AuthState.Error("Authentication failed: $error")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Error during sign in: ${e.message}")
            }
        }
    }
    
    // Sign out the user
    fun signOut() {
        _authState.value = AuthState.Loading
        
        viewModelScope.launch {
            try {
                authService.signOut()
                _userInfo.value = null
                _isProfileCompleted.value = false
                _authState.value = AuthState.Unauthenticated
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Error during sign out: ${e.message}")
            }
        }
    }
    
    // Check if Firestore profile is complete
    private suspend fun checkFirestoreProfileCompletion(email: String) {
        if (email.isBlank()) {
            _isProfileCompleted.value = false
            return
        }
        
        try {
            // Check if profile exists in Firestore
            val profile = firestoreRepository.getUserProfile(email)
            if (profile != null) {
                // Consider profile complete if we have name, roll, and phone number
                val isComplete = profile.displayName.isNotBlank() && 
                                profile.rollNumber.isNotBlank() && 
                                profile.phoneNumber.length == 10
                
                _isProfileCompleted.value = isComplete
                // Log.d(TAG, "Firestore profile completion check: $isComplete")
            } else {
                _isProfileCompleted.value = false
                // Log.d(TAG, "No Firestore profile found")
            }
        } catch (e: Exception) {
            _isProfileCompleted.value = false
        }
    }
    
    // Create or update user profile in Firestore
    private suspend fun createOrUpdateUserProfile(userInfo: UserInfo) {
        try {
            // Check if profile already exists
            val existingProfile = firestoreRepository.getUserProfile(userInfo.email)
            
            if (existingProfile == null) {
                // Create new profile with basic info from auth
                val newProfile = firestoreRepository.userInfoToProfile(userInfo)
                firestoreRepository.saveUserProfile(newProfile)
                // Log.d(TAG, "Created new user profile in Firestore: ${userInfo.email}")
            } else {
                // Profile exists, don't overwrite user-entered data like phone number
                // Log.d(TAG, "User profile already exists in Firestore, not overwriting")
            }
        } catch (e: Exception) {
        }
    }
    
    // Public method to check profile completion (called from UI)
    fun checkProfileCompletion() {
        viewModelScope.launch {
            val email = _userInfo.value?.email ?: ""
            checkFirestoreProfileCompletion(email)
        }
    }
}

// Authentication state
sealed class AuthState {
    object Loading : AuthState()
    object Authenticated : AuthState()
    object Unauthenticated : AuthState()
    data class Error(val message: String) : AuthState()
} 