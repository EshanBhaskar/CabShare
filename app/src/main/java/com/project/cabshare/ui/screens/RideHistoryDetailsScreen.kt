package com.project.cabshare.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.project.cabshare.auth.AuthViewModel
import com.project.cabshare.models.RideHistory
import com.project.cabshare.models.RideCompletionStatus
import com.project.cabshare.models.RideDirection
import com.project.cabshare.ui.viewmodels.RideHistoryViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideHistoryDetailsScreen(
    navController: NavHostController,
    rideId: String,
    authViewModel: AuthViewModel = viewModel(),
    onBackClick: () -> Unit,
    onChatClick: (RideHistory) -> Unit
) {
    val userInfo by authViewModel.userInfo.collectAsState()
    
    // Create RideHistoryViewModel with the user's email
    val rideHistoryViewModel: RideHistoryViewModel = viewModel(
        factory = RideHistoryViewModel.provideFactory(userEmail = userInfo?.email ?: "")
    )
    
    val currentRideHistory by rideHistoryViewModel.currentRideHistory.collectAsState()
    val isLoading by rideHistoryViewModel.isLoading.collectAsState()
    val error by rideHistoryViewModel.error.collectAsState()
    
    // Load ride details when screen is created
    LaunchedEffect(rideId) {
        rideHistoryViewModel.loadRideHistoryDetails(rideId)
    }
    
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault()) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ride History Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { currentRideHistory?.let(onChatClick) }) {
                        Icon(Icons.Default.Chat, contentDescription = "Chat")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                error != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = error ?: "Unknown error occurred",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                currentRideHistory == null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Ride not found",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
                else -> {
                    // Extract currentRide to a local variable that can be smart cast
                    val ride = currentRideHistory
                    if (ride != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp)
                        ) {
                            // Status information
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = "Status",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = when (ride.completionStatus) {
                                                RideCompletionStatus.COMPLETED -> Icons.Default.CheckCircle
                                                RideCompletionStatus.CANCELLED -> Icons.Default.Cancel
                                            },
                                            contentDescription = "Status Icon",
                                            tint = when (ride.completionStatus) {
                                                RideCompletionStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                                                RideCompletionStatus.CANCELLED -> MaterialTheme.colorScheme.error
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = when (ride.completionStatus) {
                                                RideCompletionStatus.COMPLETED -> "Completed"
                                                RideCompletionStatus.CANCELLED -> "Cancelled"
                                            }
                                        )
                                    }
                                    
                                    Text(
                                        text = "Completed at: ${dateFormat.format(ride.completedAt)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                            }
                            
                            // Ride details
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = "Ride Details",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    
                                    // Source and Destination
                                    Text(
                                        text = "From: ${ride.source}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                    Text(
                                        text = "To: ${ride.destination}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                    
                                    // Date and Time
                                    Text(
                                        text = "Date & Time: ${dateFormat.format(ride.dateTime)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                    
                                    // Direction
                                    Text(
                                        text = "Direction: ${
                                            when (ride.direction) {
                                                RideDirection.FROM_IITP -> "From IITP"
                                                RideDirection.TO_IITP -> "To IITP"
                                            }
                                        }",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                    
                                    // Train/Flight details if available
                                    if (ride.trainNumber.isNotEmpty() || ride.trainName.isNotEmpty()) {
                                        Text(
                                            text = "Train: ${ride.trainNumber} ${ride.trainName}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    }
                                    
                                    if (ride.flightNumber.isNotEmpty() || ride.flightName.isNotEmpty()) {
                                        Text(
                                            text = "Flight: ${ride.flightNumber} ${ride.flightName}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    }
                                    
                                    // Notes if available
                                    if (ride.notes.isNotEmpty()) {
                                        Text(
                                            text = "Notes: ${ride.notes}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                            
                            // Participants
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = "Participants",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    
                                    // Creator
                                    Text(
                                        text = "Creator: ${ride.creator} (${ride.creatorEmail})",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                    
                                    // Passengers
                                    if (ride.passengers.isNotEmpty()) {
                                        Text(
                                            text = "Passengers:",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                        )
                                        ride.passengers.forEach { passenger ->
                                            Text(
                                                text = "${passenger.displayName} (${passenger.email})",
                                                style = MaterialTheme.typography.bodyMedium,
                                                modifier = Modifier.padding(vertical = 2.dp)
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = "No passengers joined this ride",
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
} 