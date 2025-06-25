package com.project.cabshare.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.project.cabshare.auth.AuthViewModel
import com.project.cabshare.models.RideHistory
import com.project.cabshare.models.RideCompletionStatus
import com.project.cabshare.ui.viewmodels.RideHistoryViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideHistoryDetailsScreen(
    navController: NavController,
    rideId: String,
    authViewModel: AuthViewModel = viewModel(),
    rideHistoryViewModel: RideHistoryViewModel = viewModel()
) {
    val userInfo by authViewModel.userInfo.collectAsState()
    val currentRide by rideHistoryViewModel.currentRideHistory.collectAsState()
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
                title = { Text("Ride Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                currentRide == null -> {
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
                    val ride = currentRide
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
                                            contentDescription = "Status"
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = when (ride.completionStatus) {
                                                RideCompletionStatus.COMPLETED -> "Completed"
                                                RideCompletionStatus.CANCELLED -> "Cancelled"
                                            },
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    }
                                }
                            }
                            
                            // Route information
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
                                        text = "Route Details",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = "Source"
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = ride.source,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    }
                                    
                                    Icon(
                                        imageVector = Icons.Default.ArrowDownward,
                                        contentDescription = "to",
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                    )
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Place,
                                            contentDescription = "Destination"
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = ride.destination,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    }
                                }
                            }
                            
                            // Time information
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
                                        text = "Time Details",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = "Scheduled time"
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "Scheduled for",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                text = dateFormat.format(ride.dateTime),
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                        }
                                    }
                                }
                            }
                            
                            // Participants information
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
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = "Creator"
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "Created by",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                text = ride.creator,
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                        }
                                    }
                                    
                                    // Passengers
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Passengers",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                    // Add creator first in the passengers list
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 32.dp, bottom = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            // Take only the part before underscore and capitalize it
                                            text = ride.creatorEmail.substringBefore("_").capitalize(),
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    }
                                    // Then show other passengers
                                    ride.passengers.forEach { passenger ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 32.dp, bottom = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = passenger.displayName,
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                        }
                                    }
                                }
                            }
                            
                            // Additional details (if any)
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
                                        text = "Additional Details",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    
                                    // Train details
                                    if (!ride.trainNumber.isBlank() || !ride.trainName.isBlank()) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Train,
                                                contentDescription = "Train"
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                if (ride.trainNumber.isNotBlank()) {
                                                    Text(
                                                        text = "Train Number: ${ride.trainNumber}",
                                                        style = MaterialTheme.typography.bodyLarge
                                                    )
                                                }
                                                if (ride.trainName.isNotBlank()) {
                                                    Text(
                                                        text = ride.trainName,
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    
                                    // Flight details
                                    if (!ride.flightNumber.isBlank() || !ride.flightName.isBlank()) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Flight,
                                                contentDescription = "Flight"
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                if (ride.flightNumber.isNotBlank()) {
                                                    Text(
                                                        text = "Flight Number: ${ride.flightNumber}",
                                                        style = MaterialTheme.typography.bodyLarge
                                                    )
                                                }
                                                if (ride.flightName.isNotBlank()) {
                                                    Text(
                                                        text = ride.flightName,
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    
                                    // Notes
                                    if (!ride.notes.isBlank()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Notes,
                                                contentDescription = "Notes"
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = ride.notes,
                                                style = MaterialTheme.typography.bodyLarge
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
} 