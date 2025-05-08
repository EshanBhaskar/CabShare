package com.project.cabshare.auth

// Microsoft authorization configuration
object AuthConfig {
    // TODO: Replace with your actual application (client) ID from Azure Portal
    const val CLIENT_ID = "f0d42e11-abce-43c3-94d6-00ab706fc954"
    
    // Authority URL for Microsoft login
    const val AUTHORITY = "https://login.microsoftonline.com/common"
    
    // Scopes required for the application
    val SCOPES = arrayOf("User.Read")
    
    // The redirect URI after authentication
    const val REDIRECT_URI = "msauth://com.project.cabshare/callback"
    
    // Email domain to restrict authentication
    const val REQUIRED_EMAIL_DOMAIN = "iitp.ac.in"
} 