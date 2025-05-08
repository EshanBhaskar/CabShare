package com.project.cabshare.data

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

/**
 * Helper class to initialize Firebase manually if not using google-services.json
 * You will need to replace these values with your actual Firebase project details
 */
object FirebaseInitializer {
    private const val TAG = "FirebaseInitializer"
    
    // Your Firebase project configuration
    // Note: In a production app, these values should be stored securely
    private const val PROJECT_ID = "cabshare-dev"
    private const val APPLICATION_ID = "1:895894028369:android:a9633f80c066aca5fa3870"
    private const val API_KEY = "AIzaSyBK7yLyyT_e-XjCzP87GTHkIYzIA_xvl1k"
    
    /**
     * Initialize Firebase for the application
     */
    fun initialize(context: Context) {
        try {
            // Check if Firebase is already initialized
            try {
                FirebaseApp.getInstance()
                return
            } catch (e: IllegalStateException) {
                // Firebase not yet initialized, continue with initialization
            }
            
            // Create FirebaseOptions
            val options = FirebaseOptions.Builder()
                .setProjectId(PROJECT_ID)
                .setApplicationId(APPLICATION_ID)
                .setApiKey(API_KEY)
                .build()
            
            // Initialize Firebase with the options
            FirebaseApp.initializeApp(context, options)
            
            // Configure Firestore settings
            val settings = FirebaseFirestoreSettings.Builder()
                .build()
            
            // Apply settings to Firestore
            FirebaseFirestore.getInstance().firestoreSettings = settings
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Firebase", e)
        }
    }
} 