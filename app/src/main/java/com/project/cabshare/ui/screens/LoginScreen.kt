package com.project.cabshare.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.project.cabshare.auth.AuthState
import com.project.cabshare.auth.AuthViewModel
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.font.FontWeight
import com.project.cabshare.ui.theme.CabShareLogo
import com.project.cabshare.ui.theme.CabShareTheme

@Composable
fun LoginScreen(
    onLoginSuccess: (Boolean) -> Unit,
    authViewModel: AuthViewModel
) {
    // Store callback references
    val currentOnLoginSuccess by rememberUpdatedState(onLoginSuccess)
    
    // Get the activity context safely
    val context = LocalContext.current
    val activity = remember {
        when (context) {
            is ComponentActivity -> context
            else -> null
        }
    }
    
    // State to track loading and errors
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Collect auth state
    val authState by authViewModel.authState.collectAsState()
    
    // Collect profile completion state
    val isProfileCompleted by authViewModel.isProfileCompleted.collectAsState()
    
    // Effect to handle navigation when auth state changes to authenticated
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> {
                isLoading = false
                errorMessage = null
                
                try {
                    // Log.d("LoginScreen", "Authentication successful, navigating")
                    
                    // Check if user has completed their profile
                    // Log.d("LoginScreen", "Profile completed: $isProfileCompleted")
                    
                    currentOnLoginSuccess(isProfileCompleted)
                } catch (e: Exception) {
                    Log.e("LoginScreen", "Navigation error: ${e.message}", e)
                    errorMessage = "Navigation error: ${e.message}"
                }
            }
            is AuthState.Error -> {
                isLoading = false
                errorMessage = (authState as AuthState.Error).message
            }
            is AuthState.Loading -> {
                isLoading = true
                errorMessage = null
            }
            is AuthState.Unauthenticated -> {
                isLoading = false
                // We're already on the login screen, so no navigation needed
            }
        }
    }
    
    // Show main login content
    LoginContent(
        isLoading = isLoading,
        errorMessage = errorMessage,
        onLoginClick = { 
            activity?.let { 
                try {
                    // Log.d("LoginScreen", "Login button clicked")
                    errorMessage = null
                    isLoading = true
                    authViewModel.signIn(it)
                } catch (e: Exception) {
                    Log.e("LoginScreen", "Error triggering sign in", e)
                    isLoading = false
                    errorMessage = "Error starting login: ${e.message}"
                }
            } ?: run {
                Log.e("LoginScreen", "Activity context is null")
                errorMessage = "Cannot start login (internal error)"
                isLoading = false
            }
        },
        onErrorDismiss = {
            errorMessage = null
        }
    )
}

@Composable
private fun LoginContent(
    isLoading: Boolean,
    errorMessage: String?,
    onLoginClick: () -> Unit,
    onErrorDismiss: () -> Unit
) {
    val currentOnLoginClick by rememberUpdatedState(onLoginClick)
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Logo - Using circular CabShareLogo
        CabShareLogo(
            modifier = Modifier
                .size(140.dp)
                .padding(bottom = 24.dp)
        )
        
        // App Title
        Text(
            text = "CabShare",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // App Description
        Text(
            text = "Share cabs with your fellow IIT Patna students",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        // Error message if any
        if (errorMessage != null) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .clickable { onErrorDismiss() }
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Authentication Error",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Tap to dismiss",
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
        
        // Login Button
        Button(
            onClick = { currentOnLoginClick() },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text("Login with Microsoft")
            }
        }
        
        // Login instructions
        Text(
            text = "Please use your IIT Patna email to login",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LoginScreenPreview() {
    CabShareTheme {
        LoginContent(
            isLoading = false,
            errorMessage = null,
            onLoginClick = {},
            onErrorDismiss = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Login Screen with Error")
@Composable
fun LoginScreenWithErrorPreview() {
    CabShareTheme {
        LoginContent(
            isLoading = false,
            errorMessage = "Invalid credentials. Please try again.",
            onLoginClick = {},
            onErrorDismiss = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Login Screen Loading")
@Composable
fun LoginScreenLoadingPreview() {
    CabShareTheme {
        LoginContent(
            isLoading = true,
            errorMessage = null,
            onLoginClick = {},
            onErrorDismiss = {}
        )
    }
} 