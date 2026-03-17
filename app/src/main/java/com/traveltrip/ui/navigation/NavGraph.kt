package com.traveltrip.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.traveltrip.ui.screens.home.HomeScreen

/**
 * Root navigation graph for TravelTrip. New screen composables will be added here as each phase is
 * implemented.
 */
@Composable
fun TravelTripNavGraph(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = modifier
    ) {
        composable(route = Screen.Home.route) {
            HomeScreen(
                    onNavigateToMap = {
                        // Will be enabled in Phase 5
                    },
                    onNavigateToTeam = {
                        // Will be enabled in Phase 7
                    }
            )
        }

        // Phase 3: Login screen
        // composable(route = Screen.Login.route) { LoginScreen(...) }

        // Phase 5: Map screen
        // composable(route = Screen.Map.route) { MapScreen(...) }

        // Phase 7: Team screens
        // composable(route = Screen.TeamLobby.route) { TeamLobbyScreen(...) }
    }
}
