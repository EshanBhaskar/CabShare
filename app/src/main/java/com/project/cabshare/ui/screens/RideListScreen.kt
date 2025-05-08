package com.project.cabshare.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.util.Log
import android.widget.DatePicker
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.project.cabshare.auth.AuthViewModel
import com.project.cabshare.models.Ride
import com.project.cabshare.models.RideDirection
import com.project.cabshare.ui.navigation.AppRoutes
import com.project.cabshare.ui.viewmodels.RideViewModel
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.DirectionsTransit
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Notes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.Notes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideListScreen(
    navController: NavController,
    direction: RideDirection,
    authViewModel: AuthViewModel = viewModel(),
    rideViewModel: RideViewModel = viewModel()
) {
    val rides by rideViewModel.filteredRides.collectAsState()
    val userInfo by authViewModel.userInfo.collectAsState()
    val destinationFilter by rideViewModel.destinationFilter.collectAsState()
    val dateFilter by rideViewModel.dateFilter.collectAsState()
    val isLoading by rideViewModel.isLoading.collectAsState()
    val error by rideViewModel.error.collectAsState()
    
    // State for create ride dialog
    var showCreateRideDialog by remember { mutableStateOf(false) }
    
    // State for date picker dialog
    var showDatePicker by remember { mutableStateOf(false) }
    
    // State to show ride limit error dialog
    var showRideLimitErrorDialog by remember { mutableStateOf(false) }
    
    // Destinations based on direction
    val destinations = listOf("Patna Station", "Patna Airport", "Bihta Station")
    
    // Load rides by direction
    LaunchedEffect(direction, destinationFilter, dateFilter) {
        rideViewModel.observeRidesByDirection(direction)
    }
    
    // Date picker dialog
    if (showDatePicker) {
        val context = LocalContext.current
        val calendar = Calendar.getInstance()
        
        val datePickerDialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                rideViewModel.setDateFilter(calendar.time)
                showDatePicker = false
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        
        // Add clear button
        datePickerDialog.setButton(
            DatePickerDialog.BUTTON_NEUTRAL,
            "Clear Filter"
        ) { _, _ ->
            rideViewModel.clearDateFilter()
            showDatePicker = false
        }
        
        LaunchedEffect(showDatePicker) {
            datePickerDialog.show()
            showDatePicker = false
        }
    }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        text = when(direction) {
                            RideDirection.FROM_IITP -> "Rides From IITP"
                            RideDirection.TO_IITP -> "Rides To IITP"
                        },
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.5.sp
                        )
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { 
                            // If a destination is selected, go back to destination selection
                            // Otherwise, go back to previous screen
                            if (destinationFilter != null) {
                                rideViewModel.clearDestinationFilter()
                            } else {
                                navController.popBackStack()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = { },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
                        )
                    )
                )
        ) {
            if (destinationFilter == null) {
                // Modern destination selection UI
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    // Header with elegant styling
                    Text(
                        text = when(direction) {
                            RideDirection.FROM_IITP -> "Where are you heading?"
                            RideDirection.TO_IITP -> "Where are you coming from?"
                        },
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.25.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .padding(vertical = 32.dp, horizontal = 8.dp)
                    )
                    
                    // Grid layout for destinations
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Destination tiles in a grid-like arrangement
                        destinations.forEach { dest ->
                            val (bgColor, textColor, icon) = getDestinationStyle(dest)
                            
                            // Simple card matching home page style
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(80.dp)
                                    .clickable { 
                                        rideViewModel.setDestinationFilter(dest)
                                    },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = bgColor
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Circle with icon
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .background(
                                                color = MaterialTheme.colorScheme.surface,
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = dest,
                                            tint = textColor,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.width(16.dp))
                                    
                                    // Destination information with consistent typography
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = dest,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                letterSpacing = 0.25.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        
                                        Text(
                                            text = when {
                                                direction == RideDirection.FROM_IITP && dest == "Patna Station" -> "Cabs to city's railway station"
                                                direction == RideDirection.FROM_IITP && dest == "Patna Airport" -> "Cabs to airport"
                                                direction == RideDirection.FROM_IITP && dest == "Bihta Station" -> "Cabs to local station"
                                                direction == RideDirection.TO_IITP && dest == "Patna Station" -> "Cabs from city's railway station"
                                                direction == RideDirection.TO_IITP && dest == "Patna Airport" -> "Cabs from airport"
                                                direction == RideDirection.TO_IITP && dest == "Bihta Station" -> "Cabs from local station"
                                                else -> "All rides available"
                                            },
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                letterSpacing = 0.15.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                        )
                                    }
                                    
                                    // Arrow icon
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "Go",
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        // "View All Destinations" button matching the destination card style - removed from here
                    }
                }
            } else {
                // Intercept back press when showing the ride list
                BackHandler(enabled = true) {
                    rideViewModel.clearDestinationFilter()
                }
                
                // Show rides filtered by destination
                Column {
                    // Selected destination header - simplified and more compact
                    val (bgColor, textColor, icon) = getDestinationStyle(destinationFilter ?: "")
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(bgColor.copy(alpha = 0.05f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = textColor,
                                modifier = Modifier.size(28.dp)
                            )
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            Text(
                                text = destinationFilter ?: "",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                        }
                    }
                    
                    // Date filter controls
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        // Calendar button
                        FilledTonalButton(
                            onClick = { showDatePicker = true },
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Filter by date",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Filter by date")
                        }
                    }
                    
                    // Date filter indicator if active
                    if (dateFilter != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Date pill with clear option
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                Text(
                                    text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(dateFilter!!),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear date filter",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clickable { rideViewModel.clearDateFilter() }
                                )
                            }
                            
                            // Clear all filters button
                            TextButton(
                                onClick = { rideViewModel.clearDateFilter() }
                            ) {
                                Text(
                                    text = "Clear Filter",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    
                    // Rides list
                    Box(modifier = Modifier.fillMaxSize()) {
                        // "View All Destinations" button moved to top of the rides list
                        if (rides.isEmpty() && !isLoading && error == null) {
                            // Show the button prominently when no rides are available
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "No Rides Available",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    text = "Be the first to create a ride to ${destinationFilter}!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Button(
                                    onClick = { showCreateRideDialog = true },
                                    modifier = Modifier.fillMaxWidth(0.7f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add",
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    Text("Create a Ride")
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        } else {
                            when {
                                isLoading && rides.isEmpty() -> {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }
                                // Only show general error if the ride limit dialog is NOT being shown
                                error != null && !showRideLimitErrorDialog -> {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "Error Loading Rides",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        Text(
                                            text = error ?: "Unknown error",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                        
                                        Spacer(modifier = Modifier.height(16.dp))
                                        
                                        // Don't show "Try Again" for ride limit errors (redundant check, but safe)
                                        if (error?.contains("You cannot create more than") == false) {
                                            Button(
                                                onClick = { 
                                                    // Trigger observer again? Or maybe just clear error?
                                                    // For now, let's assume observer handles refresh.
                                                    // If retry is needed, consider a specific ViewModel function.
                                                }
                                            ) {
                                                Text("Try Again")
                                            }
                                        }
                                    }
                                }
                                rides.isNotEmpty() -> {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Sort rides by dateTime (nearest future rides first, then past rides)
                                        val now = Date()
                                        val sortedRides = rides.sortedWith(compareBy<Ride> { ride ->
                                            val rideDate = ride.dateTime
                                            // If ride is in the past, add a large number to push it to the bottom
                                            if (rideDate.before(now)) {
                                                Long.MAX_VALUE + rideDate.time
                                            } else {
                                                // For future rides, sort by their actual time
                                                rideDate.time
                                            }
                                        })
                                        
                                        if (sortedRides.isEmpty()) {
                                            item {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(16.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Column(
                                                        horizontalAlignment = Alignment.CenterHorizontally
                                                    ) {
                                                        Text(
                                                            text = "No rides found",
                                                            style = MaterialTheme.typography.titleMedium,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        
                                        items(sortedRides) { ride ->
                                            RideCard(
                                                ride = ride,
                                                onClick = {
                                                    navController.navigate("${AppRoutes.RideDetails.route}/${ride.rideId}")
                                                }
                                            )
                                        }
                                        
                                        // Add Create button at the bottom
                                        item {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Button(
                                                onClick = {
                                                    // Check ride limit before showing dialog
                                                    userInfo?.email?.let {
                                                        rideViewModel.checkRideLimitAndShowDialog(it) { canCreate ->
                                                            if (canCreate) {
                                                                showCreateRideDialog = true
                                                            } else {
                                                                showRideLimitErrorDialog = true
                                                            }
                                                        }
                                                    } ?: run {
                                                        // Handle case where user info is not available
                                                        Log.e("RideListScreen", "Cannot check ride limit: User info not available.")
                                                    }
                                                },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(56.dp)
                                                    .padding(horizontal = 16.dp),
                                                shape = RoundedCornerShape(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = "Add",
                                                    modifier = Modifier.padding(end = 8.dp)
                                                )
                                                Text(
                                                    text = "Create a Ride",
                                                    style = MaterialTheme.typography.titleMedium
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(24.dp))
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
    
    // Ride Limit Error Dialog
    if (showRideLimitErrorDialog) {
        AlertDialog(
            onDismissRequest = { 
                showRideLimitErrorDialog = false
                rideViewModel.clearError() // Clear the error state when dialog is dismissed
            },
            title = { Text("Ride Limit Reached") },
            text = { Text(error ?: "You cannot create more than the maximum allowed rides. Please delete some of your existing rides first.") },
            confirmButton = {
                Button(
                    onClick = { 
                        showRideLimitErrorDialog = false 
                        rideViewModel.clearError()
                        navController.navigate(AppRoutes.MyRides.route) // Navigate to My Rides to delete
                    }
                ) {
                    Text("View My Rides")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showRideLimitErrorDialog = false
                    rideViewModel.clearError()
                }) {
                    Text("Close")
                }
            }
        )
    }

    // Create Ride Dialog (Only show if not blocked by limit)
    if (showCreateRideDialog) {
        // No need to check limit here again, already checked before setting showCreateRideDialog
        CreateRideDialog(
            direction = direction,
            preselectedDestination = destinationFilter,
            onDismiss = { showCreateRideDialog = false },
            onCreateRide = { source, destination, date, time, maxPassengers, notes, trainNumber, trainName, flightNumber, flightName ->
                userInfo?.let { user ->
                    // Combine date and time for dateTime
                    val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
                    val calendar = Calendar.getInstance()
                    
                    try {
                        // Parse the date
                        val dateObj = dateFormatter.parse(date)
                        dateObj?.let {
                            calendar.time = it
                        }
                        
                        // Parse the time and add to calendar
                        val timeObj = timeFormatter.parse(time)
                        timeObj?.let {
                            val timeCalendar = Calendar.getInstance()
                            timeCalendar.time = it
                            calendar.set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY))
                            calendar.set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE))
                        }
                        
                        // Set source and destination in the ViewModel
                        rideViewModel.rideSource.value = source
                        rideViewModel.rideDestination.value = destination
                        rideViewModel.rideDate.value = calendar.time
                        rideViewModel.rideTime.value = calendar.time
                        rideViewModel.rideMaxPassengers.value = maxPassengers.toString()
                        rideViewModel.rideNotes.value = notes
                        rideViewModel.rideDirection.value = direction
                        
                        // EXPLICITLY set the train/flight details from parameters
                        rideViewModel.rideTrainNumber.value = trainNumber
                        rideViewModel.rideTrainName.value = trainName
                        rideViewModel.rideFlightNumber.value = flightNumber
                        rideViewModel.rideFlightName.value = flightName
                        
                        // Determine ride type based on source/destination
                        val isTrainRide = destination == "Patna Station" || destination == "Bihta Station" || 
                                          source == "Patna Station" || source == "Bihta Station"
                        val isFlightRide = destination == "Patna Airport" || source == "Patna Airport"
                        
                        Log.d("RideDebug", "RIDELIST - Determining ride type during creation: isTrainRide=$isTrainRide, isFlightRide=$isFlightRide")
                        Log.d("RideDebug", "RIDELIST - Based on: source='$source', destination='$destination'")
                        
                        // Debug log - final values being used for ride creation
                        Log.d("RideDebug", "RIDELIST - Final train details from ViewModel before creating ride: '${rideViewModel.rideTrainNumber.value}', '${rideViewModel.rideTrainName.value}'")
                        Log.d("RideDebug", "RIDELIST - Final flight details from ViewModel before creating ride: '${rideViewModel.rideFlightNumber.value}', '${rideViewModel.rideFlightName.value}'")
                        Log.d("RideDebug", "RIDELIST - Final notes from ViewModel before creating ride: '${rideViewModel.rideNotes.value}'")
                        
                        // *** DIRECT VALUE INSERTION TEST ***
                        if (isTrainRide) {
                            // Force set test values to see if direct setting works
                            //rideViewModel.rideTrainNumber.value = "TEST-7896"
                            //rideViewModel.rideTrainName.value = "TEST-Express"
                            Log.d("RideDebug", "RIDELIST - *** FINAL train details: '${rideViewModel.rideTrainNumber.value}', '${rideViewModel.rideTrainName.value}' ***")
                        } else if (isFlightRide) {
                            // Force set test values to see if direct setting works
                            //rideViewModel.rideFlightNumber.value = "TEST-FL123"
                            //rideViewModel.rideFlightName.value = "TEST-Airways"
                            Log.d("RideDebug", "RIDELIST - *** FINAL flight details: '${rideViewModel.rideFlightNumber.value}', '${rideViewModel.rideFlightName.value}' ***")
                        }
                        
                        // Call the repository method to create the ride
                        rideViewModel.createRide(user.email) { rideId ->
                            // IMPORTANT: Re-check the values directly from ViewModel instead of using local variables
                            // that might have been stored before ride creation completed
                            val currentTrainNumber = rideViewModel.rideTrainNumber.value
                            val currentTrainName = rideViewModel.rideTrainName.value
                            val currentFlightNumber = rideViewModel.rideFlightNumber.value
                            val currentFlightName = rideViewModel.rideFlightName.value
                            
                            Log.d("RideDebug", "RIDELIST - Train details AFTER RIDE CREATION: '${currentTrainNumber}', '${currentTrainName}'")
                            Log.d("RideDebug", "RIDELIST - Flight details AFTER RIDE CREATION: '${currentFlightNumber}', '${currentFlightName}'")
                            
                            // After ride creation, update train/flight details if needed as a separate operation
                            if (currentTrainNumber.isNotBlank() || currentTrainName.isNotBlank()) {
                                try {
                                    Log.d("RideDebug", "RIDELIST - Force updating train details from UI: ${currentTrainNumber} - ${currentTrainName}")
                                    rideViewModel.updateTrainDetails(rideId, currentTrainNumber, currentTrainName)
                                } catch (e: Exception) {
                                    Log.e("RideDebug", "RIDELIST - Error updating train details from UI", e)
                                }
                            }
                            
                            if (currentFlightNumber.isNotBlank() || currentFlightName.isNotBlank()) {
                                try {
                                    Log.d("RideDebug", "RIDELIST - Force updating flight details from UI: ${currentFlightNumber} - ${currentFlightName}")
                                    rideViewModel.updateFlightDetails(rideId, currentFlightNumber, currentFlightName)
                                } catch (e: Exception) {
                                    Log.e("RideDebug", "RIDELIST - Error updating flight details from UI", e)
                                }
                            }
                            
                            // Navigate to ride details on success
                            navController.navigate("${AppRoutes.RideDetails.route}/$rideId")
                        }
                    } catch (e: Exception) {
                        // Handle date/time parsing errors
                        Log.e("RideListScreen", "Error parsing date/time: ${e.message}")
                    }
                }
                showCreateRideDialog = false
            }
        )
    }
}

@Composable
fun RideCard(
    ride: Ride,
    onClick: () -> Unit
) {
    // Format date and time from ride's dateTime
    val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
    val timeFormat = SimpleDateFormat("h:mm", Locale.getDefault()) // Remove AM/PM from main time format
    val amPmFormat = SimpleDateFormat("a", Locale.getDefault()) // Separate format for AM/PM
    
    val formattedDate = dateFormat.format(ride.dateTime)
    val formattedTime = timeFormat.format(ride.dateTime)
    val amPm = amPmFormat.format(ride.dateTime)
    
    // Get location and icon based on ride direction - keeping this for internal use but not displaying
    val (_, _) = when (ride.direction) {
        RideDirection.FROM_IITP -> Pair(ride.destination, getDestinationStyle(ride.destination).third)
        RideDirection.TO_IITP -> Pair(ride.source, getDestinationStyle(ride.source).third)
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Time Badge - Fixed to separate time and AM/PM
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    shape = CircleShape,
                    modifier = Modifier.size(48.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = formattedTime,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = amPm,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Normal,
                            modifier = Modifier.offset(y = (-2).dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Ride info
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                    
                    // Show train/flight info instead of destination
                    if (ride.trainNumber.isNotBlank() || ride.trainName.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Train,
                                contentDescription = "Train",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = buildString {
                                    if (ride.trainNumber.isNotBlank()) append(ride.trainNumber)
                                    if (ride.trainNumber.isNotBlank() && ride.trainName.isNotBlank()) append(" - ")
                                    if (ride.trainName.isNotBlank()) append(ride.trainName)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else if (ride.flightNumber.isNotBlank() || ride.flightName.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Flight,
                                contentDescription = "Flight",
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = buildString {
                                    if (ride.flightNumber.isNotBlank()) append(ride.flightNumber)
                                    if (ride.flightNumber.isNotBlank() && ride.flightName.isNotBlank()) append(" - ")
                                    if (ride.flightName.isNotBlank()) append(ride.flightName)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    
                    // Show notes if available
                    if (ride.notes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Notes",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = ride.notes.take(30) + if (ride.notes.length > 30) "..." else "",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                
                // Available seats
                Surface(
                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    val availableSeats = ride.maxPassengers - ride.passengers.size
                    val seatColor = when {
                        availableSeats <= 0 -> MaterialTheme.colorScheme.error
                        availableSeats == 1 -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.tertiary
                    }
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.People,
                                contentDescription = "Passengers",
                                tint = seatColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${ride.passengers.size}/${ride.maxPassengers}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = seatColor
                            )
                        }
                        
                        Text(
                            text = "seats",
                            style = MaterialTheme.typography.labelSmall,
                            color = seatColor.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LocationText(
    title: String, 
    subtitle: String
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Text(
            text = subtitle,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRideDialog(
    direction: RideDirection,
    preselectedDestination: String?,
    onDismiss: () -> Unit,
    onCreateRide: (String, String, String, String, Int, String, String, String, String, String) -> Unit,
    rideViewModel: RideViewModel = viewModel()
) {
    val context = LocalContext.current
    
    // Form state
    var source by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var maxPassengers by remember { mutableStateOf("4") }
    
    // Snackbar state
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    // Validation error message - Now used only to trigger snackbar
    var validationError by remember { mutableStateOf<String?>(null) }
    
    // Pre-fill IITP as source or destination based on direction
    // Also pre-select destination if filtering is active
    LaunchedEffect(direction, preselectedDestination) {
        if (direction == RideDirection.FROM_IITP) {
            source = "IIT Patna"
            destination = preselectedDestination ?: ""
        } else {
            destination = "IIT Patna"
            source = preselectedDestination ?: ""
        }
        
        // Reset train and flight details and notes when dialog opens
        rideViewModel.rideTrainNumber.value = ""
        rideViewModel.rideTrainName.value = ""
        rideViewModel.rideFlightNumber.value = ""
        rideViewModel.rideFlightName.value = ""
        rideViewModel.rideNotes.value = "" // Ensure notes are reset too
        validationError = null // Clear any previous validation error
    }
    
    // Determine if this is a train or flight ride based on destination
    val isTrainRide = destination == "Patna Station" || destination == "Bihta Station" || 
                      source == "Patna Station" || source == "Bihta Station"
    val isFlightRide = destination == "Patna Airport" || source == "Patna Airport"
    
    // Log the ride type determination
    Log.d("RideDebug", "DIALOG - Determining ride type: isTrainRide=$isTrainRide, isFlightRide=$isFlightRide")
    Log.d("RideDebug", "DIALOG - Based on: source='$source', destination='$destination'")
    
    // Calendar for date picker
    val calendar = Calendar.getInstance()
    
    // Date picker dialog
    val datePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
            calendar.set(year, month, dayOfMonth)
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            date = dateFormat.format(calendar.time)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )
    
    // Time picker dialog
    val timePickerDialog = TimePickerDialog(
        context,
        { _, hourOfDay: Int, minute: Int ->
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
            calendar.set(Calendar.MINUTE, minute)
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            time = timeFormat.format(calendar.time)
        },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        false
    )
    
    // Dropdown menu expanded states
    var destinationExpanded by remember { mutableStateOf(false) }
    var sourceExpanded by remember { mutableStateOf(false) }
    
    // Declare a passenger counter
    var passengerCount by remember { mutableStateOf(4) }
    // Make sure maxPassengers is kept in sync with passengerCount
    maxPassengers = passengerCount.toString()
    
    // Effect to show Snackbar when validationError changes
    LaunchedEffect(validationError) {
        validationError?.let {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = it,
                    duration = SnackbarDuration.Short
                )
                validationError = null // Clear error after showing snackbar
            }
        }
    }
    
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        // Wrap Card in Scaffold to host Snackbar
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = Color.Transparent // Make Scaffold background transparent
        ) { padding -> // Use padding provided by Scaffold
             Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(padding) // Apply Scaffold padding here
                    .padding(16.dp), // Add original Card padding
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
                        text = "Create a New Ride",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Always show both From and To fields, but make the appropriate one a dropdown based on direction
                    // From (Source) field
                    if (direction == RideDirection.FROM_IITP) {
                        // Source field for FROM_IITP (always IIT Patna)
                        OutlinedTextField(
                            value = source,
                            onValueChange = { source = it },
                            label = { Text("From") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "Source"
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false,
                            readOnly = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    } else {
                        // Source dropdown for TO_IITP
                        ExposedDropdownMenuBox(
                            expanded = sourceExpanded,
                            onExpandedChange = { sourceExpanded = it },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = source,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("From") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = "Source"
                                    )
                                },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = sourceExpanded)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            
                            ExposedDropdownMenu(
                                expanded = sourceExpanded,
                                onDismissRequest = { sourceExpanded = false },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val sourceOptions = listOf("Patna Station", "Patna Airport", "Bihta Station")
                                sourceOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option) },
                                        onClick = {
                                            source = option
                                            sourceExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // To (Destination) field
                    if (direction == RideDirection.FROM_IITP) {
                        // Destination dropdown for FROM_IITP
                        ExposedDropdownMenuBox(
                            expanded = destinationExpanded,
                            onExpandedChange = { destinationExpanded = it },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = destination,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("To") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = "Destination"
                                    )
                                },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = destinationExpanded)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            
                            ExposedDropdownMenu(
                                expanded = destinationExpanded,
                                onDismissRequest = { destinationExpanded = false },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val destinationOptions = listOf("Patna Station", "Patna Airport", "Bihta Station")
                                destinationOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option) },
                                        onClick = {
                                            destination = option
                                            destinationExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        // Destination field for TO_IITP (always IIT Patna)
                        OutlinedTextField(
                            value = destination,
                            onValueChange = { destination = it },
                            label = { Text("To") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "Destination"
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false,
                            readOnly = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Train Details Section - only show for train destinations
                    if (isTrainRide) {
                        Text(
                            text = "Train Details",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        OutlinedTextField(
                            value = rideViewModel.rideTrainNumber.value,
                            onValueChange = { newValue -> 
                                rideViewModel.rideTrainNumber.value = newValue
                                Log.d("RideDebug", "DIALOG - Train number changed [FIXED]: '${rideViewModel.rideTrainNumber.value}'")
                            },
                            label = { Text("Train Number") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Train,
                                    contentDescription = "Train Number"
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedTextField(
                            value = rideViewModel.rideTrainName.value,
                            onValueChange = { newValue -> 
                                rideViewModel.rideTrainName.value = newValue
                                Log.d("RideDebug", "DIALOG - Train name changed [FIXED]: '${rideViewModel.rideTrainName.value}'")
                            },
                            label = { Text("Train Name") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Label,
                                    contentDescription = "Train Name"
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        
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
                        
                        OutlinedTextField(
                            value = rideViewModel.rideFlightNumber.value,
                            onValueChange = { newValue -> 
                                rideViewModel.rideFlightNumber.value = newValue
                                Log.d("RideDebug", "DIALOG - Flight number changed [FIXED]: '${rideViewModel.rideFlightNumber.value}'")
                            },
                            label = { Text("Flight Number") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Flight,
                                    contentDescription = "Flight Number"
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedTextField(
                            value = rideViewModel.rideFlightName.value,
                            onValueChange = { newValue -> 
                                rideViewModel.rideFlightName.value = newValue
                                Log.d("RideDebug", "DIALOG - Flight name changed [FIXED]: '${rideViewModel.rideFlightName.value}'")
                            },
                            label = { Text("Flight Name") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Label,
                                    contentDescription = "Flight Name"
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    // Date field with date picker
                    OutlinedTextField(
                        value = date,
                        onValueChange = { },
                        label = { Text("Date") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Date",
                                modifier = Modifier.clickable { datePickerDialog.show() }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { datePickerDialog.show() },
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
                    
                    // Maximum passengers field
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Passengers Required",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Decrement button
                            IconButton(
                                onClick = { if (passengerCount > 1) passengerCount-- },
                                enabled = passengerCount > 1
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = if(passengerCount > 1) 1f else 0.5f)
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
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Notes field
                    OutlinedTextField(
                        value = rideViewModel.rideNotes.value,
                        onValueChange = { newValue -> 
                            rideViewModel.rideNotes.value = newValue 
                            Log.d("RideDebug", "DIALOG - Notes changed: '${rideViewModel.rideNotes.value}'")
                        },
                        label = { Text("Notes (Optional)") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Notes,
                                contentDescription = "Notes"
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                    
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
                                // Check validation first
                                val error = validateRideInputs(source, destination, date, time)
                                if (error == null) {
                                    // No need to set values in ViewModel here since we're updating directly during text changes
                                    // Just log final values for debugging
                                    Log.d("RideDebug", "DIALOG - Final train details before create: '${rideViewModel.rideTrainNumber.value}', '${rideViewModel.rideTrainName.value}'")
                                    Log.d("RideDebug", "DIALOG - Final flight details before create: '${rideViewModel.rideFlightNumber.value}', '${rideViewModel.rideFlightName.value}'")
                                    
                                    // Verify values stored in ViewModel will be used
                                    Log.d("RideDebug", "DIALOG - Final notes: '${rideViewModel.rideNotes.value}'")
                                    
                                    onCreateRide(
                                        source,
                                        destination,
                                        date,
                                        time,
                                        passengerCount,
                                        rideViewModel.rideNotes.value,
                                        rideViewModel.rideTrainNumber.value,
                                        rideViewModel.rideTrainName.value,
                                        rideViewModel.rideFlightNumber.value,
                                        rideViewModel.rideFlightName.value
                                    )
                                } else {
                                    // Set validation error to trigger Snackbar effect
                                    validationError = error
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Create Ride")
                        }
                    }
                }
            }
        }
    }
}

// Updated validation function that returns an error message or null if valid
private fun validateRideInputs(
    source: String,
    destination: String,
    date: String,
    time: String
): String? {
    return when {
        source.isBlank() -> "Please select a source location"
        destination.isBlank() -> "Please select a destination"
        date.isBlank() -> "Please select a date for your ride"
        time.isBlank() -> "Please select a time for your ride"
        else -> null // All inputs are valid
    }
}

// Define a function to get destination-specific styling
@Composable
private fun getDestinationStyle(destination: String): Triple<Color, Color, ImageVector> {
    val isDark = isSystemInDarkTheme()
    val colors = MaterialTheme.colorScheme

    return when {
        destination.contains("Patna Station", ignoreCase = true) -> {
            val bgColor = if (isDark) Color(0xFF004D40) else colors.primaryContainer
            val textColor = if (isDark) Color(0xFF80DEEA) else colors.onPrimaryContainer
            Triple(
                bgColor,
                textColor,
                Icons.Default.DirectionsTransit // Transit icon
            )
        }
        destination.contains("Patna Airport", ignoreCase = true) -> {
            val bgColor = if (isDark) Color(0xFFBF360C) else colors.secondaryContainer
            val textColor = if (isDark) Color(0xFFFFCCBC) else colors.onSecondaryContainer
            Triple(
                bgColor,
                textColor,
                Icons.Default.Flight // Flight icon
            )
        }
        destination.contains("Bihta Station", ignoreCase = true) -> {
            val bgColor = if (isDark) Color(0xFF5D4037) else colors.tertiaryContainer
            val textColor = if (isDark) Color(0xFFFFF59D) else colors.onTertiaryContainer
            Triple(
                bgColor,
                textColor,
                Icons.Default.Train // Train icon
            )
        }
        else -> {
            // Default remains based on theme
            Triple(
                colors.surfaceVariant,
                colors.onSurfaceVariant,
                Icons.Default.LocationOn // Default location icon
            )
        }
    }
} 