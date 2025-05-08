package com.project.cabshare.auth

import com.project.cabshare.models.UserProfile

/**
 * Data class for user information from authentication
 */
data class UserInfo(
    val email: String,
    val displayName: String,
    val rollNumber: String = "",  // Optional field
    val userProfile: UserProfile? = null // User profile from Firestore
) 