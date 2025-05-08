package com.project.cabshare.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.project.cabshare.auth.AuthViewModel
import com.project.cabshare.data.Notification
import com.project.cabshare.data.NotificationType
import com.project.cabshare.ui.navigation.AppRoutes
import com.project.cabshare.ui.viewmodels.RideViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.google.firebase.Timestamp
import com.project.cabshare.models.Ride
import com.project.cabshare.models.UserProfile as User
import androidx.compose.foundation.clickable
import androidx.compose.material3.HorizontalDivider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyRidesScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel(),
    rideViewModel: RideViewModel = viewModel()
) {
    val userInfo by authViewModel.userInfo.collectAsState()
    val rides by rideViewModel.rides.collectAsState()
    val notifications by rideViewModel.notifications.collectAsState()
    val isLoading by rideViewModel.isLoading.collectAsState()
    val error by rideViewModel.error.collectAsState()
    var selectedTabIndex by remember { mutableStateOf(0) }

    val tabs = listOf("My Rides", "Notifications")

    LaunchedEffect(userInfo) {
        userInfo?.email?.let { email ->
            rideViewModel.observeUserRides(email)
            rideViewModel.observeUserNotifications(email)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Activity") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Your Activity",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Overview of your rides and notifications",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTabIndex) {
                0 -> MyRidesContent(
                    rides = rides,
                    isLoading = isLoading,
                    error = error,
                    userEmail = userInfo?.email ?: "",
                    navController = navController,
                    modifier = Modifier.padding(16.dp)
                )
                1 -> NotificationLog(
                    notifications = notifications,
                    isLoading = isLoading,
                    error = error,
                    navController = navController,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun MyRidesContent(
    rides: List<Ride>,
    isLoading: Boolean,
    error: String?,
    userEmail: String,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        if (error != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (rides.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.NoLuggage,
                        contentDescription = "No rides",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "You don't have any rides yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Create a new ride or join an existing one",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn {
                val createdRides = rides.filter { it.creatorEmail == userEmail }
                val joinedRides = rides.filter {
                    it.creatorEmail != userEmail &&
                            it.passengers.any { passenger -> passenger.email == userEmail }
                }
                val pendingRides = rides.filter {
                    it.creatorEmail != userEmail &&
                            it.pendingRequests.any { request ->
                                request.userId == userEmail
                            }
                }

                if (createdRides.isNotEmpty()) {
                    item {
                        Text(
                            text = "Rides Created",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        )
                    }
                    items(createdRides) { ride ->
                        RideCard(
                            ride = ride,
                            onClick = {
                                navController.navigate("${AppRoutes.RideDetails.route}/${ride.rideId}")
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                if (joinedRides.isNotEmpty() || pendingRides.isNotEmpty()) {
                    item {
                        Text(
                            text = "Rides Joined / Pending",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        )
                    }
                    items(joinedRides) { ride ->
                        RideCard(
                            ride = ride,
                            onClick = {
                                navController.navigate("${AppRoutes.RideDetails.route}/${ride.rideId}")
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    items(pendingRides) { ride ->
                        RideCard(
                            ride = ride,
                            onClick = {
                                navController.navigate("${AppRoutes.RideDetails.route}/${ride.rideId}")
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationLog(
    notifications: List<Notification>,
    isLoading: Boolean,
    error: String?,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        if (error != null && error.contains("index")) {
            // Special case for index errors
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Setting up notifications...",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "The notification system is being set up. Please check back later or refresh the app.",
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        } else if (error != null) {
            // Normal error handling
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = "Error loading notifications: $error",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (notifications.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsNone,
                        contentDescription = "No notifications",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "No Notifications Yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "You don't have any notifications. When someone requests to join your ride, or your request to join a ride is accepted or rejected, you'll see updates here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { 
                            // Navigate to ride list with FROM_IITP direction
                            navController.navigate(AppRoutes.RideList.createRoute("FROM_IITP")) 
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Find rides"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Browse Available Rides")
                    }
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "${notifications.size} Notifications",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(notifications.sortedByDescending { it.timestamp.toDate() }) { notification ->
                        NotificationItem(notification = notification) {
                            notification.relatedRideId?.let { rideId ->
                                navController.navigate("${AppRoutes.RideDetails.route}/$rideId")
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationItem(
    notification: Notification,
    onClick: () -> Unit
) {
    val icon = when (notification.type) {
        NotificationType.REQUEST_RECEIVED -> Icons.Default.PersonAdd
        NotificationType.REQUEST_ACCEPTED -> Icons.Default.CheckCircle
        NotificationType.REQUEST_REJECTED -> Icons.Default.Cancel
        NotificationType.PASSENGER_JOINED -> Icons.Default.GroupAdd
        NotificationType.RIDE_CANCELLED -> Icons.Default.EventBusy
        NotificationType.OTHER -> Icons.Default.Info
    }
    val iconColor = when (notification.type) {
         NotificationType.REQUEST_ACCEPTED -> MaterialTheme.colorScheme.primary
         NotificationType.REQUEST_REJECTED -> MaterialTheme.colorScheme.error
         else -> LocalContentColor.current.copy(alpha = 0.6f)
    }

    val formattedDate = remember(notification.timestamp) {
        SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(notification.timestamp.toDate())
    }

    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = {
            Text(
                text = notification.message,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (!notification.isRead) FontWeight.Bold else FontWeight.Normal
            )
        },
        supportingContent = {
             Text(
                text = formattedDate,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = notification.type.name,
                tint = iconColor
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

// Need to import the actual RideCard composable if it's defined elsewhere
// For now, adding a placeholder
// @Composable
// fun RideCard(ride: com.project.cabshare.data.Ride, isPending: Boolean = false, onClick: () -> Unit) {
//     // Placeholder implementation
//     Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
//         Text(text = "Ride from ${ride.source} to ${ride.destination} on ${ride.date}", modifier = Modifier.padding(16.dp))
//         if (isPending) {
//             Text(" (Pending Request)", color = Color.Gray, modifier = Modifier.padding(start = 16.dp, bottom = 16.dp))
//         }
//     }
// }

// Placeholder for User data class if not imported correctly
// package com.project.cabshare.data
// data class User(val email: String = "")

// Placeholder for Ride data class if not imported correctly
// package com.project.cabshare.data
// data class Ride(
//    val rideId: String = "",
//    val creatorEmail: String = "",
//    val source: String = "",
//    val destination: String = "",
//    val date: String = "",
//    val passengers: List<Passenger> = emptyList(),
//    val pendingRequests: List<RideRequest> = emptyList()
// )
// data class Passenger(val email: String = "")
// data class RideRequest(val userId: String = "")

// Make sure RequestStatus is defined or imported
// enum class RequestStatus { PENDING, ACCEPTED, REJECTED } 