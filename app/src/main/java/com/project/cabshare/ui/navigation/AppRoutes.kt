package com.project.cabshare.ui.navigation

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
    
    object RideHistory {
        const val route = "ride_history"
    }
    
    object RideHistoryDetails {
        const val route = "ride_history_details"
        const val rideIdArg = "rideId"
        fun createRoute(rideId: String) = "$route/$rideId"
    }

    object Chat {
        const val route = "chat/{rideId}"
        const val rideIdArg = "rideId"
        fun createRoute(rideId: String) = "chat/$rideId"
    }
} 