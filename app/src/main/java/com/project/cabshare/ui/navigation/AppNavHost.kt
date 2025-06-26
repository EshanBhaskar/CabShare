package com.project.cabshare.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.project.cabshare.auth.AuthViewModel
import com.project.cabshare.models.RideDirection
import com.project.cabshare.ui.screens.*
import com.project.cabshare.ui.viewmodels.RideViewModel
import com.project.cabshare.ui.viewmodels.RideHistoryViewModel
import android.util.Log
import com.project.cabshare.ui.screens.ChatScreen
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavController
import com.project.cabshare.models.Ride

/**
 * Main navigation component for the app
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    authViewModel: AuthViewModel = viewModel()
) {
    // Ensure the AuthViewModel is created at the top level
    val viewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
    }
    val authViewModel: AuthViewModel = viewModel(viewModelStoreOwner)
    val rideViewModel: RideViewModel = viewModel(viewModelStoreOwner)
    
    // Get current user info from AuthViewModel
    val userInfo by authViewModel.userInfo.collectAsState(initial = null)
    
    // Create RideHistoryViewModel with the user's email
    val rideHistoryViewModel: RideHistoryViewModel = viewModel(
        viewModelStoreOwner,
        factory = RideHistoryViewModel.provideFactory(
            userEmail = userInfo?.email ?: ""
        )
    )
    
    NavHost(
        navController = navController,
        startDestination = AppRoutes.SPLASH
    ) {
        // Splash screen
        composable(AppRoutes.SPLASH) {
            SplashScreen(
                navController = navController,
                authViewModel = authViewModel
            )
        }
        
        // Login screen
        composable(AppRoutes.LOGIN) {
            LoginScreen(
                onLoginSuccess = { isReturningUser ->
                    // Navigate based on whether this is a returning user
                    if (isReturningUser) {
                        // Returning users go directly to main screen
                        navController.navigate(AppRoutes.MAIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    } else {
                        // New users go to profile setup first
                        navController.navigate(AppRoutes.USER_PROFILE) {
                            popUpTo(AppRoutes.LOGIN) { inclusive = true }
                        }
                    }
                },
                authViewModel = authViewModel
            )
        }
        
        // User Profile screen - Make sure it handles both first-time setup and profile editing
        composable(AppRoutes.USER_PROFILE) {
            UserProfileScreen(
                onProfileComplete = { 
                    navController.navigate(AppRoutes.MAIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                authViewModel = authViewModel
            )
        }
        
        // Main screen
        composable(AppRoutes.MAIN) {
            MainScreen(
                navController = navController,
                authViewModel = authViewModel
            )
        }
        
        // Ride List Screen - Includes direction as an argument
        composable(
            route = "${AppRoutes.RideList.route}/{${AppRoutes.RideList.directionArg}}",
            arguments = listOf(
                navArgument(AppRoutes.RideList.directionArg) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val directionString = backStackEntry.arguments?.getString(AppRoutes.RideList.directionArg) ?: RideDirection.FROM_IITP.name
            val direction = try {
                RideDirection.valueOf(directionString)
            } catch (e: IllegalArgumentException) {
                RideDirection.FROM_IITP
            }
            
            RideListScreen(
                navController = navController,
                direction = direction,
                authViewModel = authViewModel,
                rideViewModel = rideViewModel
            )
        }
        
        // Ride Details Screen - Includes ride ID as an argument
        composable(
            route = AppRoutes.RideDetails.route + "/{${AppRoutes.RideDetails.rideIdArg}}",
            arguments = listOf(navArgument(AppRoutes.RideDetails.rideIdArg) { type = NavType.StringType })
        ) { backStackEntry ->
            val rideId = backStackEntry.arguments?.getString(AppRoutes.RideDetails.rideIdArg) ?: return@composable
            RideDetailsScreen(
                navController = navController,
                rideId = rideId,
                authViewModel = authViewModel,
                onBackClick = { navController.popBackStack() },
                onChatClick = { ride ->
                    navController.navigate(AppRoutes.Chat.createRoute(ride.rideId))
                }
            )
        }
        
        // My Rides Screen
        composable(AppRoutes.MyRides.route) {
            MyRidesScreen(
                navController = navController,
                authViewModel = authViewModel,
                rideViewModel = rideViewModel
            )
        }
        
        // Ride History Screen
        composable(AppRoutes.RideHistory.route) {
            RideHistoryScreen(
                navController = navController,
                authViewModel = authViewModel
            )
        }
        
        // Ride History Details Screen
        composable(
            route = AppRoutes.RideHistoryDetails.route + "/{${AppRoutes.RideHistoryDetails.rideIdArg}}",
            arguments = listOf(navArgument(AppRoutes.RideHistoryDetails.rideIdArg) { type = NavType.StringType })
        ) { backStackEntry ->
            val rideId = backStackEntry.arguments?.getString(AppRoutes.RideHistoryDetails.rideIdArg) ?: return@composable
            RideHistoryDetailsScreen(
                navController = navController,
                rideId = rideId,
                authViewModel = authViewModel,
                onBackClick = { navController.popBackStack() },
                onChatClick = { rideHistory ->
                    navController.navigate(AppRoutes.Chat.createRoute(rideHistory.rideId))
                }
            )
        }
        
        composable(
            route = AppRoutes.Chat.route,
            arguments = listOf(navArgument(AppRoutes.Chat.rideIdArg) { type = NavType.StringType })
        ) { backStackEntry ->
            val rideId = backStackEntry.arguments?.getString(AppRoutes.Chat.rideIdArg) ?: return@composable
            val userInfo by authViewModel.userInfo.collectAsState()
            
            // Create both view models
            val rideViewModel: RideViewModel = viewModel()
            val rideHistoryViewModel: RideHistoryViewModel = viewModel(
                factory = RideHistoryViewModel.provideFactory(userEmail = userInfo?.email ?: "")
            )
            
            // Observe both active ride and ride history
            val currentRide by rideViewModel.currentRide.collectAsState()
            val currentRideHistory by rideHistoryViewModel.currentRideHistory.collectAsState()
            
            LaunchedEffect(rideId) {
                rideViewModel.observeRideDetails(rideId)
                rideHistoryViewModel.loadRideHistoryDetails(rideId)
            }
            
            // Convert RideHistory to Ride if needed
            val ride = currentRide ?: currentRideHistory?.let { history ->
                Ride(
                    rideId = history.rideId,
                    source = history.source,
                    destination = history.destination,
                    dateTime = history.dateTime,
                    maxPassengers = history.maxPassengers,
                    creator = history.creator,
                    creatorEmail = history.creatorEmail,
                    direction = history.direction,
                    notes = history.notes,
                    passengers = history.passengers,
                    trainNumber = history.trainNumber,
                    trainName = history.trainName,
                    flightNumber = history.flightNumber,
                    flightName = history.flightName,
                    status = "COMPLETED"
                )
            }
            
            ride?.let { r ->
                ChatScreen(
                    ride = r,
                    userEmail = userInfo?.email ?: "",
                    userName = userInfo?.displayName ?: "",
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
} 