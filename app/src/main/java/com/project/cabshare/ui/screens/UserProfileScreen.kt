package com.project.cabshare.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.project.cabshare.ui.theme.CabShareLogo
import com.project.cabshare.ui.theme.CabShareTheme
import androidx.lifecycle.viewmodel.compose.viewModel
import com.project.cabshare.auth.AuthViewModel
import android.util.Log
import com.project.cabshare.data.FirestoreUserRepository
import com.project.cabshare.models.UserProfile
import kotlinx.coroutines.launch
import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    onProfileComplete: () -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {
    var name by remember { mutableStateOf("") }
    var roll by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var isEditMode by remember { mutableStateOf(false) }
    var isFirstTime by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(true) }
    var showPhoneInfoDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    
    // Create a Firestore repository instance
    val firestoreRepository = remember { FirestoreUserRepository() }

    // Create a coroutine scope
    val coroutineScope = rememberCoroutineScope()
    
    // Get user info from AuthViewModel
    val userInfo by authViewModel.userInfo.collectAsState()
    
    // User profile from Firestore
    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    
    // Set roll number from userInfo if available
    LaunchedEffect(userInfo) {
        userInfo?.let {
            // Get the email from auth
            val email = it.email
            
            if (email.isNotBlank()) {
                // Load user profile from Firestore
                val profile = firestoreRepository.getUserProfile(email)
                
                // If profile exists in Firestore, use it
                if (profile != null) {
                    userProfile = profile
                    name = profile.displayName
                    roll = profile.rollNumber
                    phone = profile.phoneNumber
                    isFirstTime = false
                } else {
                    // Profile doesn't exist yet, use auth data and mark as first time
                    name = it.displayName
                    
                    // Rely solely on rollNumber from userInfo (which should be populated by AuthService)
                    roll = it.rollNumber
                    
                    isFirstTime = true
                    isEditMode = true
                }
            }
            
            isLoading = false
        }
    }
    
    val scrollState = rememberScrollState()
    
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile header
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // App Logo in circular frame
                    CabShareLogo(
                        modifier = Modifier
                            .size(180.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Profile title
                    Text(
                        text = if (isFirstTime) "Complete Your Profile" else "Your Profile",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (!isFirstTime && !isEditMode) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "View or edit your profile information",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Profile fields card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isEditMode) {
                        // Edit Mode UI
                        
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Full Name") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Name"
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            shape = RoundedCornerShape(12.dp)
                        )
                        
                        // Roll number field - auto-filled and not editable
                        OutlinedTextField(
                            value = roll,
                            onValueChange = { }, // No changes allowed
                            label = { Text("Roll Number") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.AccountBox,
                                    contentDescription = "Roll"
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Auto-filled",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            readOnly = true,
                            supportingText = { 
                                Text("Auto-filled from your Microsoft ID") 
                            },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                disabledBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                disabledLeadingIconColor = MaterialTheme.colorScheme.primary,
                                disabledTrailingIconColor = MaterialTheme.colorScheme.primary,
                                disabledLabelColor = MaterialTheme.colorScheme.primary,
                            )
                        )
                        
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { 
                                // Only allow digits and limit to 10 characters
                                if (it.all { char -> char.isDigit() } && it.length <= 10) {
                                    phone = it 
                                }
                            },
                            label = { Text("Phone Number") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = "Phone"
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { showPhoneInfoDialog = true }) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Phone Number Privacy",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            supportingText = {
                                Text("Your number will be shared when you join or accept rides")
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            shape = RoundedCornerShape(12.dp)
                        )
                    } else {
                        // View Mode UI - Show profile info with styling
                        
                        // Name Field
                        ProfileField(
                            label = "Name",
                            value = name,
                            icon = Icons.Default.Person
                        )
                        
                        // Roll Number Field
                        ProfileField(
                            label = "Roll Number",
                            value = roll,
                            icon = Icons.Default.AccountBox
                        )
                        
                        // Phone Field
                        ProfileField(
                            label = "Phone",
                            value = phone,
                            icon = Icons.Default.Phone,
                            showInfoIcon = true,
                            onInfoClick = { showPhoneInfoDialog = true }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    if (isEditMode) {
                        // Save button in edit mode
                        Button(
                            onClick = {
                                // Use roll from userInfo if available
                                val finalRoll = userInfo?.rollNumber?.takeIf { it.isNotBlank() } ?: roll
                                
                                // Save profile to Firestore - get base info from userInfo
                                coroutineScope.launch {
                                    try {
                                        val userEmail = userInfo?.email ?: ""
                                        if(userEmail.isBlank()) {
                                            return@launch
                                        }
                                        
                                        // Create a UserProfile object
                                        val profile = UserProfile(
                                            userId = userEmail,
                                            email = userEmail,
                                            displayName = name,
                                            rollNumber = finalRoll,
                                            phoneNumber = phone
                                        )
                                        
                                        // Save to Firestore
                                        firestoreRepository.saveUserProfile(profile)
                                        
                                        // Update local reference
                                        userProfile = profile
                                        
                                        // Exit edit mode
                                        isEditMode = false
                                        isFirstTime = false
                                        
                                        // Trigger profile completion check in ViewModel
                                        authViewModel.checkProfileCompletion()
                                        
                                        if (isFirstTime) {
                                            onProfileComplete()
                                        }
                                    } catch (e: Exception) {
                                        error = e.message
                                        showErrorDialog = true
                                    }
                                }
                            },
                            enabled = name.isNotBlank() && 
                                    roll.isNotBlank() && 
                                    phone.length == 10,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Save",
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = if (isFirstTime) "Continue" else "Save Changes",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    } else {
                        // Edit button in view mode
                        Button(
                            onClick = { isEditMode = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit",
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = "Edit Profile",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Continue button in view mode
                        OutlinedButton(
                            onClick = { onProfileComplete() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Continue",
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = "Continue to App",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showPhoneInfoDialog) {
        PhoneInfoDialog(onDismiss = { showPhoneInfoDialog = false })
    }

    // Show a dialog if there's an error with the profile
    if (showErrorDialog) {
        val errorMessage = error ?: "Unknown error occurred while processing your profile"
        
        AlertDialog(
            onDismissRequest = {
                showErrorDialog = false
            },
            title = { 
                Text(
                    "Error",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                ) 
            },
            text = { 
                Text(
                    errorMessage,
                    style = MaterialTheme.typography.bodyMedium
                ) 
            },
            confirmButton = {
                TextButton(onClick = {
                    showErrorDialog = false
                }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun ProfileField(
    label: String,
    value: String,
    icon: ImageVector,
    showInfoIcon: Boolean = false,
    onInfoClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        
        if (showInfoIcon) {
            IconButton(
                onClick = onInfoClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Information",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
    
    HorizontalDivider(
        modifier = Modifier.padding(start = 40.dp),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    )
}

@Composable
fun PhoneInfoDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                "Phone Number Privacy",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            ) 
        },
        text = { 
            Text(
                "Your phone number will be shared with other users when you join someone's ride or when you accept someone to join your ride. This is to facilitate communication between riders.",
                style = MaterialTheme.typography.bodyMedium
            ) 
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got it")
            }
        }
    )
}

@Preview(showBackground = true, showSystemUi = true, name = "User Profile - Edit Mode")
@Composable
fun UserProfileScreenEditPreview() {
    CabShareTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile header
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // App Logo in circular frame
                        CabShareLogo(
                            modifier = Modifier.size(180.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Profile title
                        Text(
                            text = "Complete Your Profile",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Profile fields card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Sample edit fields
                        OutlinedTextField(
                            value = "John Doe",
                            onValueChange = { },
                            label = { Text("Full Name") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Name"
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            shape = RoundedCornerShape(12.dp)
                        )
                        
                        OutlinedTextField(
                            value = "2001CS01",
                            onValueChange = { },
                            label = { Text("Roll Number") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.AccountBox,
                                    contentDescription = "Roll"
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "User Profile - View Mode")
@Composable
fun UserProfileScreenViewPreview() {
    CabShareTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile header
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // App Logo in circular frame
                        CabShareLogo(
                            modifier = Modifier.size(180.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Profile title
                        Text(
                            text = "Your Profile",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "View or edit your profile information",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
} 