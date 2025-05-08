package com.project.cabshare.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import android.util.Log

/**
 * Navigation routes for the app
 */
object AppRoutes {
    // Auth related routes
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val USER_PROFILE = "user_profile"
    const val MAIN = "main"
    
    // Ride related routes
    object RideList {
        const val route = "ride_list"
        const val directionArg = "direction"
        fun createRoute(direction: String) = "$route/$direction"
    }
    
    object RideDetails {
        const val route = "ride_details"
        const val rideIdArg = "rideId"
        fun createRoute(rideId: String) = "$route/$rideId"
    }
    
    object MyRides {
        const val route = "my_rides"
    }
}

/**
 * Main navigation component for the app
 */
@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = AppRoutes.SPLASH
) {
    // Ensure the AuthViewModel is created at the top level
    val viewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
    }
    val authViewModel: AuthViewModel = viewModel(viewModelStoreOwner)
    val rideViewModel: RideViewModel = viewModel(viewModelStoreOwner)
    
    NavHost(
        navController = navController,
        startDestination = startDestination
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
                        // Log.d("AppNavHost", "Returning user - skipping profile setup")
                        navController.navigate(AppRoutes.MAIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    } else {
                        // New users go to profile setup first
                        // Log.d("AppNavHost", "New user - going to profile setup")
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
            route = "${AppRoutes.RideDetails.route}/{${AppRoutes.RideDetails.rideIdArg}}",
            arguments = listOf(
                navArgument(AppRoutes.RideDetails.rideIdArg) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val rideId = backStackEntry.arguments?.getString(AppRoutes.RideDetails.rideIdArg) ?: ""
            
            RideDetailsScreen(
                navController = navController,
                rideId = rideId,
                authViewModel = authViewModel,
                rideViewModel = rideViewModel
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
    }
} 