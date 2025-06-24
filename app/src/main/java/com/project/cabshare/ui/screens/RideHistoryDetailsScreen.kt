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
    val rideHistory by rideHistoryViewModel.rideHistory.collectAsState()
    val isLoading by rideHistoryViewModel.isLoading.collectAsState()
    val error by rideHistoryViewModel.error.collectAsState()
    
    // Find the specific ride from history
    val ride = rideHistory.find { it.rideId == rideId }
    
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
                ride == null -> {
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
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        // Status chip
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = when (ride.completionStatus) {
                                RideCompletionStatus.COMPLETED -> MaterialTheme.colorScheme.primaryContainer
                                RideCompletionStatus.EXPIRED -> MaterialTheme.colorScheme.errorContainer
                                RideCompletionStatus.CANCELLED -> MaterialTheme.colorScheme.surfaceVariant
                            },
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            Text(
                                text = ride.completionStatus.name,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelLarge,
                                color = when (ride.completionStatus) {
                                    RideCompletionStatus.COMPLETED -> MaterialTheme.colorScheme.onPrimaryContainer
                                    RideCompletionStatus.EXPIRED -> MaterialTheme.colorScheme.onErrorContainer
                                    RideCompletionStatus.CANCELLED -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
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
                                
                                val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                                
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
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = "Completed time"
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Completed on",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = dateFormat.format(ride.completedAt),
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
                                
                                // Creator
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp),
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
                                            text = ride.creatorEmail,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    }
                                }
                                
                                // Passengers
                                if (ride.passengers.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Passengers",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
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
                        }
                        
                        // Additional details (if any)
                        if (!ride.notes.isNullOrBlank() || !ride.trainNumber.isNullOrBlank() || !ride.flightNumber.isNullOrBlank()) {
                            Card(
                                modifier = Modifier.fillMaxWidth()
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
                                    if (!ride.trainNumber.isNullOrBlank() || !ride.trainName.isNullOrBlank()) {
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
                                                if (!ride.trainNumber.isNullOrBlank()) {
                                                    Text(
                                                        text = "Train Number: ${ride.trainNumber}",
                                                        style = MaterialTheme.typography.bodyLarge
                                                    )
                                                }
                                                if (!ride.trainName.isNullOrBlank()) {
                                                    Text(
                                                        text = ride.trainName,
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    
                                    // Flight details
                                    if (!ride.flightNumber.isNullOrBlank() || !ride.flightName.isNullOrBlank()) {
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
                                                if (!ride.flightNumber.isNullOrBlank()) {
                                                    Text(
                                                        text = "Flight Number: ${ride.flightNumber}",
                                                        style = MaterialTheme.typography.bodyLarge
                                                    )
                                                }
                                                if (!ride.flightName.isNullOrBlank()) {
                                                    Text(
                                                        text = ride.flightName,
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    
                                    // Notes
                                    if (!ride.notes.isNullOrBlank()) {
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