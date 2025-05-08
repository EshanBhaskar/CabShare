package com.project.cabshare.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.microsoft.identity.client.*
import com.microsoft.identity.client.exception.MsalException
import com.project.cabshare.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import kotlin.coroutines.resume

// Extension property for the DataStore
val Context.dataStore by preferencesDataStore(name = "auth_prefs")

class AuthService(private val context: Context) {
    
    private val TAG = "AuthService"
    
    // Keys for the DataStore
    private object PreferencesKeys {
        val USER_EMAIL = stringPreferencesKey("user_email")
        val USER_DISPLAY_NAME = stringPreferencesKey("user_display_name")
        val USER_ROLL_NUMBER = stringPreferencesKey("user_roll_number")
    }
    
    // MSAL Public Client Application instance
    private var msalApp: ISingleAccountPublicClientApplication? = null
    
    // Initialize MSAL
    suspend fun init(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Create the MSAL application using default configuration in a background thread
                if (msalApp == null) {
                    msalApp = PublicClientApplication.createSingleAccountPublicClientApplication(
                        context,
                        R.raw.auth_config
                    )
                }
                
                // Check if user is already signed in - This must be done on a background thread
                val account = msalApp?.currentAccount
                account?.currentAccount != null
            } catch (e: MsalException) {
                Log.e(TAG, "MSAL Error initializing MSAL: ${e.message}", e)
                false
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error initializing MSAL: ${e.message}", e)
                false
            }
        }
    }
    
    // Sign in the user
    suspend fun signIn(activity: Activity): Result<UserInfo> {
        return try {
            // Ensure MSAL is initialized
            if (msalApp == null) {
                val initialized = init()
                if (!initialized) {
                    Log.e(TAG, "Failed to initialize MSAL")
                    return Result.failure(Exception("Failed to initialize Microsoft authentication"))
                }
            }
            
            // Clear any existing accounts first (on background thread)
            withContext(Dispatchers.IO) {
                try {
                    msalApp?.signOut()
                } catch (e: Exception) {
                    Log.e(TAG, "Error during sign-out before sign-in: ${e.message}", e)
                }
            }
            
            // Define scopes
            val scopes = AuthConfig.SCOPES
            
            // Use suspendCancellableCoroutine to properly wait for the authentication result
            val result = suspendCancellableCoroutine<IAuthenticationResult?> { continuation ->
                // Create authentication callback
                val authCallback = object : AuthenticationCallback {
                    override fun onSuccess(result: IAuthenticationResult) {
                        continuation.resume(result)
                    }
                    
                    override fun onError(exception: MsalException) {
                        Log.e(TAG, "Authentication error: ${exception.message}", exception)
                        continuation.resume(null)
                    }
                    
                    override fun onCancel() {
                        continuation.resume(null)
                    }
                }
                
                try {
                    // Using the builder pattern for AcquireTokenParameters
                    val parameters = AcquireTokenParameters.Builder()
                        .startAuthorizationFromActivity(activity)
                        .withScopes(scopes.toList())
                        .withCallback(authCallback)
                        // Force the user to select an account by adding SELECT_ACCOUNT prompt
                        .withPrompt(Prompt.SELECT_ACCOUNT)
                        .build()
                    
                    // Start the authentication flow - this should happen on UI thread
                    msalApp?.acquireToken(parameters)
                } catch (e: Exception) {
                    Log.e(TAG, "Error during acquireToken: ${e.message}", e)
                    continuation.resume(null)
                }
                
                // Set cancellation callback
                continuation.invokeOnCancellation {
                }
            }
            
            // Check if authentication was successful
            if (result == null) {
                Log.e(TAG, "Authentication returned null result")
                return Result.failure(Exception("Microsoft authentication failed. Please try again."))
            }
            
            val email = result.account.username.lowercase()
            
            // Enforce domain restriction
            if (!email.endsWith("@${AuthConfig.REQUIRED_EMAIL_DOMAIN}")) {
                Log.e(TAG, "Email domain not allowed: $email")
                signOut()
                return Result.failure(Exception("Only @${AuthConfig.REQUIRED_EMAIL_DOMAIN} accounts are allowed"))
            }
            
            // Extract display name if available
            val displayName = try {
                result.account.claims?.get("name")?.toString() ?: email
            } catch (e: Exception) {
                Log.e(TAG, "Error extracting display name: ${e.message}", e)
                email
            }
            
            // Extract roll number from email
            val rollNumber = extractRollNumber(email)
            
            // Save user information to DataStore
            val userInfo = UserInfo(
                email = email,
                displayName = displayName,
                rollNumber = rollNumber
            )
            
            saveUserInfo(userInfo)
            Result.success(userInfo)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during sign in: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    // Extract roll number from email (assuming formats like eshan_2301CS16@iitp.ac.in or 2301cs16_eshan@iitp.ac.in)
    private fun extractRollNumber(email: String): String {
        // Get the username part before the @ symbol
        val username = if (email.contains("@")) email.split("@")[0] else email
        
        // Pattern for roll numbers in the format 2301CS16 (4 digits followed by 2 letters followed by 2 digits)
        val rollPattern = "\\d{4}[A-Za-z]{2}\\d{2}".toRegex()
        
        // Try to find the roll number anywhere in the username
        val matchResult = rollPattern.find(username)
        
        if (matchResult != null) {
            val rollNumber = matchResult.value.uppercase()
            return rollNumber
        }
        
        // If no direct match, try alternative patterns or check for underscores
        if (username.contains("_")) {
            val parts = username.split("_")
            for (part in parts) {
                // Check if any part matches the pattern or resembles a roll number
                val partMatch = rollPattern.find(part)
                if (partMatch != null) {
                    val rollNumber = partMatch.value.uppercase()
                    return rollNumber
                }
            }
        }
        
        return ""
    }
    
    // Sign out the user
    suspend fun signOut() {
        withContext(Dispatchers.IO) {
            try {
                msalApp?.signOut()
                // Clear DataStore
                context.dataStore.edit { preferences ->
                    preferences.clear()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error signing out: ${e.message}", e)
            }
        }
    }
    
    // Get current user information
    fun getUserInfo(): Flow<UserInfo?> {
        return context.dataStore.data.map { preferences ->
            val email = preferences[PreferencesKeys.USER_EMAIL]
            val displayName = preferences[PreferencesKeys.USER_DISPLAY_NAME]
            val rollNumber = preferences[PreferencesKeys.USER_ROLL_NUMBER]
            
            if (email != null && displayName != null) {
                UserInfo(
                    email = email, 
                    displayName = displayName,
                    rollNumber = rollNumber ?: extractRollNumber(email)
                )
            } else {
                null
            }
        }
    }
    
    // Save user information to DataStore
    private suspend fun saveUserInfo(userInfo: UserInfo) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_EMAIL] = userInfo.email
            preferences[PreferencesKeys.USER_DISPLAY_NAME] = userInfo.displayName
            preferences[PreferencesKeys.USER_ROLL_NUMBER] = userInfo.rollNumber
        }
    }
    
    // Check if user is authenticated
    suspend fun isAuthenticated(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (msalApp == null) {
                    init()
                }
                
                // This MUST be called on a background thread
                val account = msalApp?.currentAccount
                val isAuth = account?.currentAccount != null
                isAuth
            } catch (e: Exception) {
                Log.e(TAG, "Error checking authentication state: ${e.message}", e)
                false
            }
        }
    }
} 