package com.project.cabshare.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.DatePicker
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.project.cabshare.auth.AuthViewModel
import com.project.cabshare.data.JoinRequest
import com.project.cabshare.data.RequestStatus
import com.project.cabshare.models.Ride
import com.project.cabshare.models.RideDirection
import com.project.cabshare.models.UserProfile
import com.project.cabshare.ui.navigation.AppRoutes
import com.project.cabshare.ui.viewmodels.RideViewModel
import com.project.cabshare.ui.viewmodels.UserViewModel
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.BorderStroke
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideDetailsScreen(
    navController: NavController,
    rideId: String,
    authViewModel: AuthViewModel = viewModel(),
    rideViewModel: RideViewModel = viewModel()
) {
    val currentRide by rideViewModel.currentRide.collectAsState()
    val isLoading by rideViewModel.isLoading.collectAsState()
    val error by rideViewModel.error.collectAsState()
    
    // Get userInfo from AuthViewModel
    val userInfo by authViewModel.userInfo.collectAsState()
    // Get creator profile
    val creatorProfile by rideViewModel.creatorProfile.collectAsState()
    // Get refreshed passenger profiles
    val refreshedPassengerProfiles by rideViewModel.refreshedPassengerProfiles.collectAsState()
    
    // Load ride details using the observer
    LaunchedEffect(rideId) {
        rideViewModel.observeRideDetails(rideId)
    }
    
    // Debug logging for ride details
    LaunchedEffect(currentRide) {
        currentRide?.let { ride ->
            // Fetch creator profile when ride data is available
            if (ride.creatorEmail.isNotBlank()) {
                rideViewModel.fetchCreatorProfile(ride.creatorEmail)
            }
            // Fetch passenger profiles when ride data is available
            if (ride.passengers.isNotEmpty()) {
                rideViewModel.fetchPassengerProfiles(ride.passengers.map { it.email })
            }
        }
    }
    
    // Fix for train/flight details not saving to Firebase - with loop prevention
    val fixAttemptedMap = remember { mutableMapOf<String, Boolean>() }
    
    LaunchedEffect(rideId, currentRide?.rideId) {
        currentRide?.let { ride ->
            // First, verify that we're actually looking at the requested ride
            if (ride.rideId != rideId) {
                // Force refresh to get the correct ride
                rideViewModel.observeRideDetails(rideId)
                return@let
            }
            
            // Only proceed if we haven't already attempted to fix this specific ride
            val rideKey = "${ride.rideId}_${ride.trainNumber}_${ride.trainName}"
            if (fixAttemptedMap[rideKey] != true && userInfo?.email == ride.creatorEmail) {
                // Mark this ride as having been checked, regardless of whether we update it
                fixAttemptedMap[rideKey] = true
                
                // Only for train destinations and only if data needs to be fixed
                if ((ride.destination == "Patna Station" || ride.destination == "Bihta Station") 
                    && (ride.trainNumber.isNotBlank() || ride.trainName.isNotBlank())) {
                    rideViewModel.updateTrainDetails(ride.rideId, ride.trainNumber, ride.trainName)
                } 
                // Only for airport destination and only if data needs to be fixed
                else if (ride.destination == "Patna Airport"
                    && (ride.flightNumber.isNotBlank() || ride.flightName.isNotBlank())) {
                    rideViewModel.updateFlightDetails(ride.rideId, ride.flightNumber, ride.flightName)
                }
            }
        }
    }
    
    // Dialog states
    var showDeleteRideDialog by remember { mutableStateOf(false) }
    var showViewRequestsDialog by remember { mutableStateOf(false) }
    
    // State for confirmation dialogs
    var showLeaveConfirmationDialog by remember { mutableStateOf(false) }
    var showRemoveConfirmationDialog by remember { mutableStateOf<UserProfile?>(null) } // Store passenger to remove
    
    // Determine if user is creator or already joined
    val isCreator = userInfo?.email == currentRide?.creatorEmail
    val isPassenger = currentRide?.passengers?.any { it.email == userInfo?.email } == true
    val hasRequested = currentRide?.pendingRequests?.any { 
        it.userId == userInfo?.email && it.status == RequestStatus.PENDING 
    } == true

    // Determine if the current user can see phone numbers
    val canSeePhoneNumbers = isCreator || isPassenger

    // Format date and time
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val formattedDate = currentRide?.dateTime?.let { dateFormat.format(it) } ?: "N/A"
    val formattedTime = currentRide?.dateTime?.let { timeFormat.format(it) } ?: "N/A"

    // Add Snackbar state and scope
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Add LaunchedEffect to show Snackbar for specific error messages
    LaunchedEffect(error) {
        error?.let { errorMessage ->
            // Show snackbar for ride full error
            if (errorMessage.contains("Ride is full")) {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = errorMessage,
                        duration = SnackbarDuration.Short // Changed to Short
                    )
                    rideViewModel.clearError() // Clear error after showing
                }
            } 
            // Optional: Show snackbar for request no longer valid error
            else if (errorMessage.contains("This request is no longer valid")) {
                 scope.launch {
                    snackbarHostState.showSnackbar(
                        message = errorMessage,
                        duration = SnackbarDuration.Short
                    )
                    rideViewModel.clearError() // Clear error after showing
                }
            }
            // Let other errors be handled by the full screen display
        }
    }

    // Delete confirmation dialog
    if (showDeleteRideDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteRideDialog = false },
            title = { Text("Delete Ride") },
            text = { Text("Are you sure you want to delete this ride? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteRideDialog = false
                        rideViewModel.deleteRide(rideId) {
                            navController.popBackStack()
                        }
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteRideDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Ride Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (isCreator) {
                ExtendedFloatingActionButton(
                    onClick = { showDeleteRideDialog = true },
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.error,
                    icon = { Icon(Icons.Default.Delete, contentDescription = "Delete Ride") },
                    text = { Text("Delete Ride") }
                )
            } else if (!isPassenger && !hasRequested &&
                (currentRide?.passengers?.size ?: 0) < (currentRide?.maxPassengers ?: 0)) {
                ExtendedFloatingActionButton(
                    onClick = { 
                        // Use the user's email to request join
                        userInfo?.email?.let { email ->
                            rideViewModel.requestToJoinRide(rideId, email) {
                                // Optional: Show success message or handle UI update
                            }
                        } ?: run {
                            // Handle case where userInfo or email is null
                            Log.e("RideDetailsScreen", "Cannot request join: User info or email is null")
                            // Optionally show an error message to the user
                        }
                    },
                    icon = { Icon(Icons.Filled.DirectionsCarFilled, "Join Ride") },
                    text = { Text("Join Ride") }
                )
            } else if (hasRequested) {
                ExtendedFloatingActionButton(
                    onClick = { 
                        userInfo?.email?.let { email ->
                            rideViewModel.cancelJoinRequest(rideId, email) {
                                // Optional: Show success message or handle UI update
                            }
                        }
                    },
                    icon = { Icon(Icons.Filled.Cancel, "Cancel Request") },
                    text = { Text("Cancel Request") },
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.error
                )
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Capture the error value locally to prevent smart cast issues
            val currentError = error

            when {
                isLoading -> {
                    // Loading state
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                // Use the local variable for checks
                currentError != null && 
                 !currentError.contains("Ride is full") && 
                 !currentError.contains("This request is no longer valid") -> {
                    // Error state
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Error Loading Ride Details", // Keep title generic
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (currentError.isNullOrEmpty()) "An unknown error occurred." else currentError,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { rideViewModel.observeRideDetails(rideId) }) { // Retry loads the ride again
                                Text("Retry")
                            }
                        }
                    }
                }
                currentRide == null -> {
                    // Ride not found
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(imageVector = Icons.Default.SearchOff, contentDescription = null, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Ride not found.", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
                else -> {
                    // Ride details
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background) 
                            .padding(horizontal = 16.dp)
                    ) {
                        // Ride info card
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 14.dp),
                                shape = RoundedCornerShape(14.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(18.dp)
                                ) {
                                    // Header with direction pill and date/time
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = when(currentRide?.direction) {
                                                RideDirection.FROM_IITP -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                RideDirection.TO_IITP -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                                                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                            }
                                        ) {
                                            Text(
                                                text = when(currentRide?.direction) {
                                                    RideDirection.FROM_IITP -> "FROM IITP"
                                                    RideDirection.TO_IITP -> "TO IITP"
                                                    else -> "UNKNOWN"
                                                },
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = when(currentRide?.direction) {
                                                    RideDirection.FROM_IITP -> MaterialTheme.colorScheme.primary
                                                    RideDirection.TO_IITP -> MaterialTheme.colorScheme.tertiary
                                                    else -> MaterialTheme.colorScheme.outline
                                                }
                                            )
                                        }
                                        
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.DateRange,
                                                    contentDescription = "Date",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = formattedDate,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                            
                                            Spacer(modifier = Modifier.width(12.dp))
                                            
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Filled.Schedule,
                                                    contentDescription = "Time",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = formattedTime,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(16.dp))
                                    HorizontalDivider(thickness = 0.5.dp)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    // Route info in a balanced format
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.LocationOn,
                                                contentDescription = "Location",
                                                tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(22.dp)
                                            )
                                            
                                        Spacer(modifier = Modifier.width(10.dp))
                                            
                                            Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "From: ",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = currentRide?.source ?: "",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                                
                                            Spacer(modifier = Modifier.height(6.dp))
                                                
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "To: ",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = currentRide?.destination ?: "",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }
                                    
                                    // Train details with better spacing
                                    if (!currentRide?.trainNumber.isNullOrBlank() || !currentRide?.trainName.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        HorizontalDivider(thickness = 0.5.dp)
                                        Spacer(modifier = Modifier.height(16.dp))
                                        
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(12.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Train,
                                                        contentDescription = "Train",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                    
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    
                                                    Text(
                                                        text = "Train Details",
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                                    
                                                Spacer(modifier = Modifier.height(8.dp))
                                                    
                                                if (!currentRide?.trainNumber.isNullOrBlank()) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = "Number: ",
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                        Text(
                                                            text = currentRide?.trainNumber ?: "",
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                    }
                                                    
                                                    if (!currentRide?.trainName.isNullOrBlank()) {
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Text(
                                                                text = "Name: ",
                                                                style = MaterialTheme.typography.bodyMedium,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                            Text(
                                                                text = currentRide?.trainName ?: "",
                                                                style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    
                                    // Flight information with better styling
                                    if (!currentRide?.flightNumber.isNullOrBlank() || !currentRide?.flightName.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        HorizontalDivider(thickness = 0.5.dp)
                                        Spacer(modifier = Modifier.height(16.dp))
                                        
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(12.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Flight,
                                                        contentDescription = "Flight",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                    
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    
                                                    Text(
                                                        text = "Flight Details",
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                                    
                                                Spacer(modifier = Modifier.height(8.dp))
                                                    
                                                if (!currentRide?.flightNumber.isNullOrBlank()) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = "Number: ",
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                        Text(
                                                            text = currentRide?.flightNumber ?: "",
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                    }
                                                    
                                                    if (!currentRide?.flightName.isNullOrBlank()) {
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Text(
                                                                text = "Name: ",
                                                                style = MaterialTheme.typography.bodyMedium,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                            Text(
                                                                text = currentRide?.flightName ?: "",
                                                                style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    
                                    // Notes section if available
                                    if (!currentRide?.notes.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        HorizontalDivider(thickness = 0.5.dp)
                                        Spacer(modifier = Modifier.height(16.dp))
                                        
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(12.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Filled.Notes,
                                                        contentDescription = "Notes",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                    
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    
                                                    Text(
                                                        text = "Notes",
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                                
                                                Spacer(modifier = Modifier.height(8.dp))
                                                
                                                Text(
                                                    text = currentRide?.notes ?: "",
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                        }
                                    } else if (isCreator) {
                                        // Show add notes button for creator
                                        var showAddNotesDialog by remember { mutableStateOf(false) }
                                        
                                        Spacer(modifier = Modifier.height(16.dp))
                                        HorizontalDivider(thickness = 0.5.dp)
                                        Spacer(modifier = Modifier.height(16.dp))
                                        
                                        OutlinedButton(
                                            onClick = { showAddNotesDialog = true },
                                            modifier = Modifier.fillMaxWidth(),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.Notes,
                                                contentDescription = "Add Notes",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Add Notes", style = MaterialTheme.typography.bodyMedium)
                                        }
                                        
                                        // Add notes dialog
                                        if (showAddNotesDialog) {
                                            var noteText by remember { mutableStateOf("") }
                                            
                                            AlertDialog(
                                                onDismissRequest = { showAddNotesDialog = false },
                                                title = { Text("Add Notes") },
                                                text = {
                                                    OutlinedTextField(
                                                        value = noteText,
                                                        onValueChange = { noteText = it },
                                                        label = { Text("Notes") },
                                                        modifier = Modifier.fillMaxWidth(),
                                                        minLines = 3,
                                                        maxLines = 5
                                                    )
                                                },
                                                confirmButton = {
                                                    Button(
                                                        onClick = {
                                                            // Update notes
                                                            currentRide?.rideId?.let { rideId ->
                                                                rideViewModel.updateNotes(rideId, noteText)
                                                                showAddNotesDialog = false
                                                            }
                                                        }
                                                    ) {
                                                        Text("Save")
                                                    }
                                                },
                                                dismissButton = {
                                                    TextButton(onClick = { showAddNotesDialog = false }) {
                                                        Text("Cancel")
                                                    }
                                                }
                                            )
                                        }
                                    }
                                    
                                    // Seats and rider info in a more balanced format
                                    Spacer(modifier = Modifier.height(16.dp))
                                    HorizontalDivider(thickness = 0.5.dp)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    // Seats info combined with creator info in one row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        // Seats info
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Filled.People,
                                            contentDescription = "Seats",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                        )
                                        
                                            Spacer(modifier = Modifier.width(6.dp))
                                        
                                        Text(
                                                text = "${currentRide?.passengers?.size ?: 0}/${currentRide?.maxPassengers ?: 0}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                        )
                                        
                                            Spacer(modifier = Modifier.width(6.dp))
                                        
                                        val availableSeats = (currentRide?.maxPassengers ?: 0) - (currentRide?.passengers?.size ?: 0)
                                        Surface(
                                                shape = RoundedCornerShape(4.dp),
                                            color = if (availableSeats > 0) 
                                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                                            else 
                                                    MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                                                modifier = Modifier.padding(start = 2.dp)
                                        ) {
                                            Text(
                                                text = if (availableSeats > 0) 
                                                        "$availableSeats seats left" 
                                                else 
                                                        "Full",
                                                    style = MaterialTheme.typography.bodySmall,
                                                color = if (availableSeats > 0) 
                                                    MaterialTheme.colorScheme.tertiary 
                                                else 
                                                    MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        
                                        // Creator info
                                        Column(horizontalAlignment = Alignment.End) { // Align content to the end
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "Created by: ",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                
                                                // Extract name from email (using the first part before _ or @)
                                                val creatorDisplayName = creatorProfile?.displayName ?: currentRide?.creatorEmail?.substringBefore("@")?.substringBefore("_")?.replaceFirstChar { it.uppercase() } ?: "?"
                                            
                                                Text(
                                                    text = creatorDisplayName,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                
                                                Spacer(modifier = Modifier.width(6.dp))
                                                
                                                Box(
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = creatorDisplayName.firstOrNull()?.toString()?.uppercase() ?: "?",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                                    )
                                                }
                                            }
                                            
                                            // Conditionally show creator's phone for accepted passengers
                                            if (isPassenger && !isCreator && creatorProfile?.phoneNumber?.isNotBlank() == true) {
                                                // Get context here where it's needed
                                                val context = LocalContext.current 
                                                // Create a non-nullable local variable inside the safe block
                                                val nonNullCreatorProfile = creatorProfile!! 
                                                
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.clickable {
                                                        try {
                                                            // Use the non-nullable variable here
                                                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${nonNullCreatorProfile.phoneNumber}")) 
                                                            // Use the context variable obtained above
                                                            context.startActivity(intent) 
                                                        } catch (e: Exception) {
                                                            Log.e("RideDetailsScreen", "Failed to dial creator: ${e.message}")
                                                        }
                                                    }
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Call,
                                                        contentDescription = "Creator Phone",
                                                        tint = MaterialTheme.colorScheme.secondary,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        // Use the non-nullable variable here too
                                                        text = nonNullCreatorProfile.phoneNumber, 
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.secondary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    
                                    // Only show edit button for creator - unified design
                                    if (isCreator) {
                                        var showEditRideDialog by remember { mutableStateOf(false) }
                                        
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Button(
                                            onClick = { showEditRideDialog = true },
                                            modifier = Modifier.fillMaxWidth(),
                                            contentPadding = PaddingValues(vertical = 10.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit Ride",
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Edit Ride Details",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                        
                                        // Edit Ride Dialog
                                        EditRideDialog(
                                            showDialog = showEditRideDialog,
                                            ride = currentRide,
                                            onDismiss = { showEditRideDialog = false },
                                            onConfirm = { updatedRide ->
                                                rideViewModel.updateRideDetails(updatedRide)
                                                showEditRideDialog = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        
                        // Join request notification for ride creator
                        if (isCreator && currentRide?.pendingRequests?.any { it.status == RequestStatus.PENDING } == true) {
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                    ),
                                    onClick = { 
                                        // Refresh ride data before showing requests to ensure we have latest data
                                        rideViewModel.observeRideDetails(rideId)
                                        showViewRequestsDialog = true 
                                    }
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "You have ${currentRide?.pendingRequests?.count { it.status == RequestStatus.PENDING }} pending join requests",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = { 
                                                // Refresh ride data before showing requests to ensure we have latest data
                                                rideViewModel.observeRideDetails(rideId)
                                                showViewRequestsDialog = true 
                                            }
                                        ) {
                                            Text("View Requests")
                                        }
                                    }
                                }
                            }
                        }
                        
                        // Passengers
                        item {
                            PassengersSection(
                                passengers = currentRide!!.passengers,
                                refreshedProfiles = refreshedPassengerProfiles,
                                creatorEmail = currentRide!!.creatorEmail,
                                currentUserEmail = userInfo?.email,
                                isCreator = isCreator,
                                onRemoveClick = { passenger ->
                                    showRemoveConfirmationDialog = passenger
                                },
                                canSeePhoneNumbers = canSeePhoneNumbers
                            )
                        }
                        
                        // Leave Ride Button (for participants only)
                        item {
                            if (isPassenger && !isCreator) {
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                    onClick = { showLeaveConfirmationDialog = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Leave Ride", modifier = Modifier.padding(end = 8.dp))
                                    Text("Leave Ride")
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                        
                        // Bottom padding
                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }
        }
    }
    
    // View join requests dialog
    if (showViewRequestsDialog && isCreator) {
        AlertDialog(
            onDismissRequest = { showViewRequestsDialog = false },
            title = { Text("Join Requests") },
            text = { 
                if (currentRide?.pendingRequests?.isEmpty() == true) {
                    Text("No pending requests")
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        currentRide?.pendingRequests?.filter { it.status == RequestStatus.PENDING }?.forEach { request ->
                            JoinRequestItem(
                                request = request,
                                onAccept = {
                                    rideViewModel.acceptJoinRequest(rideId, request.requestId) { success ->
                                        if (!success) {
                                            showViewRequestsDialog = false // Close dialog if request no longer exists
                                        }
                                    }
                                },
                                onReject = {
                                    rideViewModel.rejectJoinRequest(rideId, request.requestId) { success ->
                                        if (!success) {
                                            showViewRequestsDialog = false // Close dialog if request no longer exists
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showViewRequestsDialog = false },
                    modifier = Modifier.padding(bottom = 2.dp)
                ) {
                    Text("Close")
                }
            }
        )
    }

    // Confirmation Dialog for Leaving Ride
    if (showLeaveConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveConfirmationDialog = false },
            title = { Text("Leave Ride") },
            text = { Text("Are you sure you want to leave this ride?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLeaveConfirmationDialog = false
                        userInfo?.email?.let { email ->
                            rideViewModel.removeOrLeavePassenger(rideId, email)
                            // Optionally navigate back or refresh UI
                        }
                    }
                ) {
                    Text("Leave", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveConfirmationDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Confirmation Dialog for Removing Passenger
    if (showRemoveConfirmationDialog != null) {
        val passengerToRemove = showRemoveConfirmationDialog!!
        AlertDialog(
            onDismissRequest = { showRemoveConfirmationDialog = null },
            title = { Text("Remove Passenger") },
            text = { Text("Are you sure you want to remove ${passengerToRemove.displayName} from this ride?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        rideViewModel.removeOrLeavePassenger(rideId, passengerToRemove.email)
                        showRemoveConfirmationDialog = null // Close dialog after action
                    }
                ) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveConfirmationDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun PassengersSection(
    passengers: List<UserProfile>,
    refreshedProfiles: Map<String, UserProfile>,
    creatorEmail: String,
    currentUserEmail: String?,
    isCreator: Boolean,
    onRemoveClick: (UserProfile) -> Unit,
    canSeePhoneNumbers: Boolean
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = "Passengers (${passengers.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        if (passengers.isEmpty()) {
            Text("No passengers yet.", style = MaterialTheme.typography.bodyMedium)
        } else {
            passengers.forEach { passengerSnapshot ->
                // Find the potentially updated profile from the map
                val currentProfile = refreshedProfiles[passengerSnapshot.email] ?: passengerSnapshot
                PassengerItem(
                    passenger = currentProfile,
                    isCreatorItem = currentProfile.email == creatorEmail,
                    showRemoveButton = isCreator && currentProfile.email != currentUserEmail,
                    onRemoveClick = { 
                        // Pass the snapshot email for removal, as ID might change
                        // Or ensure removeOrLeavePassenger uses email consistently 
                        onRemoveClick(passengerSnapshot) 
                    },
                    showPhoneNumber = canSeePhoneNumbers
                )
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
fun PassengerItem(
    passenger: UserProfile,
    isCreatorItem: Boolean,
    showRemoveButton: Boolean,
    onRemoveClick: () -> Unit,
    showPhoneNumber: Boolean
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = passenger.displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            // Name and Role
            Column {
                Text(
                    text = passenger.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                if (isCreatorItem) {
                    Text(
                        text = "Creator",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                // Optional: Display phone number if needed and available
                 if (showPhoneNumber && passenger.phoneNumber.isNotBlank()) {
                     Text(
                         text = passenger.phoneNumber,
                         style = MaterialTheme.typography.bodySmall,
                         color = MaterialTheme.colorScheme.onSurfaceVariant,
                         modifier = Modifier.clickable {
                            try {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${passenger.phoneNumber}"))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Log.e("PassengerItem", "Failed to dial: ${e.message}")
                                // Optionally show a toast message
                            }
                         }
                     )
                 }
            }
        }
        
        // Remove Button (only for creator, not for themselves)
        if (showRemoveButton) {
            IconButton(onClick = onRemoveClick) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Remove Passenger",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun JoinRequestItem(
    request: JoinRequest,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = request.userProfile.displayName.firstOrNull()?.toString()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                
                Spacer(modifier = Modifier.width(10.dp))
                
                // Requester info
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = request.userProfile.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Spacer(modifier = Modifier.height(2.dp))
                    
                    Text(
                        text = request.userProfile.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Info message about phone number sharing
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .background(
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(6.dp)
                    )
                    .padding(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Info",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Accepting will share contact numbers",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Action buttons with improved alignment
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onReject,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("Reject", style = MaterialTheme.typography.bodyMedium)
                }
                
                Button(
                    onClick = onAccept,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("Accept", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
fun EditRideDialog(
    showDialog: Boolean,
    ride: Ride?,
    onDismiss: () -> Unit,
    onConfirm: (Ride) -> Unit
) {
    if (!showDialog || ride == null) return

    val context = LocalContext.current
    
    // Initialize with existing train and flight details
    var trainNumber by remember { mutableStateOf(ride.trainNumber) }
    var trainName by remember { mutableStateOf(ride.trainName) }
    var flightNumber by remember { mutableStateOf(ride.flightNumber) }
    var flightName by remember { mutableStateOf(ride.flightName) }
    var notes by remember { mutableStateOf(ride.notes) }
    var maxPassengers by remember { mutableStateOf(ride.maxPassengers.toString()) }
    
    // Date and time formatting
    val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
    
    // Keep date from the ride (read-only)
    val date = dateFormatter.format(ride.dateTime)
    
    // Convert ride time to formatted string for editing
    var time by remember { mutableStateOf(timeFormatter.format(ride.dateTime)) }
    
    // Calendar for time pickers
    val calendar = Calendar.getInstance()
    calendar.time = ride.dateTime

    // Determine if this is a train or flight ride based on destination
    val isTrainRide = ride.destination == "Patna Station" || ride.destination == "Bihta Station"
    val isFlightRide = ride.destination == "Patna Airport"
    
    // Passenger counter
    var passengerCount by remember { mutableStateOf(ride.maxPassengers) }
    // Keep maxPassengers in sync with passengerCount
    maxPassengers = passengerCount.toString()
    
    // Time picker dialog
    val timePickerDialog = TimePickerDialog(
        context,
        { _, hourOfDay: Int, minute: Int ->
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
            calendar.set(Calendar.MINUTE, minute)
            time = timeFormatter.format(calendar.time)
        },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        false
    )

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Edit Ride Details",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Ride route info (non-editable, just informational)
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        // Route display
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Route",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${ride.source} ↔ ${ride.destination}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Date display - better visual style
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Date",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = date,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Train Details Section - only show for train stations
                if (isTrainRide) {
                    Text(
                        text = "Train Details",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    // Train Number (read-only display)
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Train,
                                contentDescription = "Train Number",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Train Number",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = trainNumber,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Train Name (read-only display)
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Label,
                                contentDescription = "Train Name",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Train Name",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = trainName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Flight Details Section - only show for airport destinations
                if (isFlightRide) {
                    Text(
                        text = "Flight Details",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    // Flight Number (read-only display)
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Flight,
                                contentDescription = "Flight Number",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Flight Number",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = flightNumber,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Flight Name (read-only display)
                     Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Label,
                                contentDescription = "Flight Name",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Flight Name",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = flightName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Time field with time picker
                OutlinedTextField(
                    value = time,
                    onValueChange = { },
                    label = { Text("Time") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Schedule,
                            contentDescription = "Time",
                            modifier = Modifier.clickable { timePickerDialog.show() }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { timePickerDialog.show() },
                    readOnly = true,
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledLeadingIconColor = MaterialTheme.colorScheme.primary
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Notes Section
                Text(
                    text = "Additional Details",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Notes,
                            contentDescription = "Notes"
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Maximum passengers field with counter
                Text(
                    text = "Capacity",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Maximum Passengers",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Decrement button
                        IconButton(
                            onClick = { 
                                if (passengerCount > ride.passengers.size) 
                                    passengerCount-- 
                            },
                            enabled = passengerCount > ride.passengers.size
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(
                                    alpha = if(passengerCount > ride.passengers.size) 1f else 0.5f
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = "Decrease Passengers",
                                    modifier = Modifier.padding(4.dp),
                                    tint = Color.White
                                )
                            }
                        }
                        
                        // Display the count
                        Text(
                            text = passengerCount.toString(),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        
                        // Increment button
                        IconButton(
                            onClick = { if (passengerCount < 20) passengerCount++ }
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Increase Passengers",
                                    modifier = Modifier.padding(4.dp),
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
                
                // Current passengers information
                if (ride.passengers.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Current passengers: ${ride.passengers.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text("Cancel")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = {
                            // Parse the time
                            try {
                                val timeObj = timeFormatter.parse(time)
                                
                                if (timeObj != null) {
                                    // Create a calendar with the original date
                                    val updatedCalendar = Calendar.getInstance()
                                    updatedCalendar.time = ride.dateTime
                                    
                                    // Only update the time part
                                    val timeCalendar = Calendar.getInstance()
                                    timeCalendar.time = timeObj
                                    
                                    updatedCalendar.set(
                                        Calendar.HOUR_OF_DAY, 
                                        timeCalendar.get(Calendar.HOUR_OF_DAY)
                                    )
                                    updatedCalendar.set(
                                        Calendar.MINUTE, 
                                        timeCalendar.get(Calendar.MINUTE)
                                    )
                                    
                                    // Create updated ride object
                                    val updatedRide = ride.copy(
                                        // Update time only
                                        dateTime = updatedCalendar.time,
                                        // Update train details for train rides, otherwise clear them
                                        trainNumber = if (isTrainRide) trainNumber.takeIf { it.isNotBlank() } ?: "" else "",
                                        trainName = if (isTrainRide) trainName.takeIf { it.isNotBlank() } ?: "" else "",
                                        // Update flight details for flight rides, otherwise clear them
                                        flightNumber = if (isFlightRide) flightNumber.takeIf { it.isNotBlank() } ?: "" else "",
                                        flightName = if (isFlightRide) flightName.takeIf { it.isNotBlank() } ?: "" else "",
                                        // Always update notes and maxPassengers - don't replace with empty string
                                        notes = notes,
                                        maxPassengers = passengerCount
                                    )
                                    onConfirm(updatedRide)
                                }
                            } catch (e: Exception) {
                                Log.e("EditRideDialog", "Error parsing time", e)
                            }
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Save Changes")
                    }
                }
            }
        }
    }
}