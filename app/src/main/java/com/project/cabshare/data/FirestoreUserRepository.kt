package com.project.cabshare.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.project.cabshare.auth.UserInfo
import com.project.cabshare.models.UserProfile // Ensure this import points to the definition in models/Ride.kt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Date

// Interface defining the repository operations
interface UserRepository {
    suspend fun saveUserProfile(userProfile: UserProfile)
    suspend fun getUserProfile(email: String): UserProfile?
    fun observeUserProfile(email: String): Flow<UserProfile?>
    suspend fun deleteUserProfile(email: String)
}

// Firestore implementation of the user repository
class FirestoreUserRepository : UserRepository {
    private val TAG = "FirestoreUserRepository"
    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("users")

    // Observable flow for user profile
    private val _userProfileFlow = MutableStateFlow<UserProfile?>(null)
    val userProfileFlow: StateFlow<UserProfile?> = _userProfileFlow

    override suspend fun saveUserProfile(userProfile: UserProfile) {
        withContext(Dispatchers.IO) {
            try {
                // Use the email as the document ID to ensure uniqueness
                val userDocument = usersCollection.document(userProfile.email)

                // Save to Firestore with merge option
                userDocument.set(userProfile, SetOptions.merge()).await()
                // Log.d(TAG, "User profile saved successfully: ${userProfile.email}") // Keep logs commented/removed

                // Update the flow
                _userProfileFlow.value = userProfile
            } catch (e: Exception) {
                Log.e(TAG, "Error saving user profile", e)
                throw e
            }
        }
    }

    override suspend fun getUserProfile(email: String): UserProfile? {
        return withContext(Dispatchers.IO) {
            try {
                val document = usersCollection.document(email).get().await()
                if (document.exists()) {
                    val userProfile = document.toObject(UserProfile::class.java)
                    // Log.d(TAG, "Retrieved user profile: $userProfile") // Keep logs commented/removed
                    userProfile
                } else {
                    // Log.d(TAG, "No user profile found for email: $email") // Keep logs commented/removed
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting user profile", e)
                null
            }
        }
    }

    override fun observeUserProfile(email: String): Flow<UserProfile?> {
        // Start a listener for the user document
        // Consider using .snapshotFlow() KTX extension if available and preferred
        usersCollection.document(email).addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error listening to user profile", error)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val userProfile = snapshot.toObject(UserProfile::class.java)
                _userProfileFlow.value = userProfile
                // Log.d(TAG, "User profile updated: $userProfile") // Keep logs commented/removed
            } else {
                _userProfileFlow.value = null
                // Log.d(TAG, "User profile doesn't exist") // Keep logs commented/removed
            }
        }

        return userProfileFlow // Return the shared flow
    }

    override suspend fun deleteUserProfile(email: String) {
        withContext(Dispatchers.IO) {
            try {
                usersCollection.document(email).delete().await()
                // Log.d(TAG, "User profile deleted successfully: $email") // Keep logs commented/removed

                // Update the flow
                _userProfileFlow.value = null
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting user profile", e)
                throw e
            }
        }
    }

    // Helper method to convert UserInfo to UserProfile
    // Ensure the UserProfile model used here matches the one in models/Ride.kt
    fun userInfoToProfile(userInfo: UserInfo, phoneNumber: String = ""): UserProfile {
        return UserProfile(
            uid = userInfo.email, // Assuming uid maps to email
            email = userInfo.email,
            userId = userInfo.email, // Assuming userId also maps to email - Check UserProfile definition
            displayName = userInfo.displayName,
            phoneNumber = phoneNumber,
            rollNumber = userInfo.rollNumber
        )
    }
} 