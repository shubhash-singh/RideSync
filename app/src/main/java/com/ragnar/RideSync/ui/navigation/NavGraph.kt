package com.ragnar.RideSync.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.ragnar.RideSync.BuildConfig
import com.ragnar.RideSync.ui.screens.auth.LoginScreen
import com.ragnar.RideSync.ui.screens.home.HomeScreen
import com.ragnar.RideSync.ui.screens.map.MapScreen
import com.ragnar.RideSync.ui.screens.profile.ProfileScreen
import com.ragnar.RideSync.ui.screens.team.TeamDetailsScreen
import com.ragnar.RideSync.ui.screens.team.TeamLobbyScreen
import com.ragnar.RideSync.utils.DebugLogger

/**
 * Root navigation graph for RideSync. Start destination is determined by the auth state — if a
 * user is signed in, MainActivity navigates to Home; otherwise the Login screen is the entry
 * point.
 *
 * @param navController     The NavHostController managing navigation.
 * @param startDestination  The initial route, set based on authentication state.
 */
@Composable
fun RideSyncNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Login.route,
    modifier: Modifier = Modifier,
    /** Called with true when the Map screen enters composition, false when it leaves. */
    onMapScreenActive: (Boolean) -> Unit = {}
) {
    if (BuildConfig.DEBUG) {
        LaunchedEffect(startDestination) {
            DebugLogger.d("NavGraph") { "NavHost startDestination=$startDestination" }
        }
    }

    DisposableEffect(navController) {
        if (!BuildConfig.DEBUG) return@DisposableEffect onDispose {}

        val listener =
            androidx.navigation.NavController.OnDestinationChangedListener { _, destination, _ ->
                val route = destination.route ?: destination.displayName
                DebugLogger.d("NavController") { "Navigated -> $route" }
            }

        navController.addOnDestinationChangedListener(listener)
        onDispose { navController.removeOnDestinationChangedListener(listener) }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // Phase 3: Login / Authentication screen
        composable(route = Screen.Login.route) {
            LoginScreen(
                onSignInSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        // Home / Landing screen
        composable(route = Screen.Home.route) {
            HomeScreen(
                onNavigateToMap = { navController.navigate(Screen.Map.route) },
                onNavigateToTeam = {
                    // Always navigate to the lobby first. TeamViewModel.init() will
                    // automatically load the team if the user already belongs to one
                    // and transition the state to InTeam.
                    navController.navigate(Screen.TeamLobby.route)
                },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onSignOut = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        // Phase 4: Profile screen (Firestore-backed user profile)
        composable(route = Screen.Profile.route) {
            ProfileScreen(onBack = { navController.popBackStack() })
        }

        // Phase 5, 6 & 11: Map screen with PiP support
        composable(route = Screen.Map.route) {
            // Notify MainActivity whenever this destination is active so it knows
            // whether to enter PiP on home-button press.
            DisposableEffect(Unit) {
                onMapScreenActive(true)
                onDispose { onMapScreenActive(false) }
            }
            MapScreen(onBack = { navController.popBackStack() })
        }

        // Phase 7: Team lobby — create or join a team
        composable(route = Screen.TeamLobby.route) {
            TeamLobbyScreen(
                onTeamJoined = { teamId ->
                    navController.navigate(Screen.TeamDetails.createRoute(teamId)) {
                        popUpTo(Screen.TeamLobby.route) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        // Phase 7: Team details — member list + leave / disband
        composable(
            route = Screen.TeamDetails.route,
            arguments = listOf(navArgument("teamId") { type = NavType.StringType })
        ) { backStackEntry ->
            val teamId = backStackEntry.arguments?.getString("teamId") ?: return@composable
            TeamDetailsScreen(
                teamId = teamId,
                onLeft = {
                    navController.navigate(Screen.TeamLobby.route) {
                        popUpTo(Screen.TeamDetails.route) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
