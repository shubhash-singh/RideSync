package com.ragnar.RideSync.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.ragnar.RideSync.BuildConfig
import com.ragnar.RideSync.ui.screens.auth.LoginScreen
import com.ragnar.RideSync.ui.screens.home.HomeScreen
import com.ragnar.RideSync.ui.screens.map.MapScreen
import com.ragnar.RideSync.ui.screens.profile.ProfileScreen
import com.ragnar.RideSync.utils.DebugLogger

/**
 * Root navigation graph for RideSync. Start destination is determined by the auth state — if a user
 * is signed in, MainActivity navigates to Home; otherwise the Login screen is the entry point.
 *
 * @param navController The NavHostController managing navigation.
 * @param startDestination The initial route, set based on authentication state.
 */
@Composable
fun RideSyncNavGraph(
        navController: NavHostController,
        startDestination: String = Screen.Login.route,
        modifier: Modifier = Modifier
) {
    if (BuildConfig.DEBUG) {
        LaunchedEffect(startDestination) {
            DebugLogger.d("NavGraph") { "NavHost startDestination=$startDestination" }
        }
    }

    DisposableEffect(navController) {
        if (!BuildConfig.DEBUG) return@DisposableEffect onDispose { }

        val listener =
                androidx.navigation.NavController.OnDestinationChangedListener { _, destination, _ ->
                    val route =
                            destination.route
                                    ?: destination.displayName
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
        // Login / Authentication screen (Phase 3)
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
                    onNavigateToMap = {
                        navController.navigate(Screen.Map.route)
                    },
                    onNavigateToTeam = {
                        // Will be enabled in Phase 7
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

        // Phase 5: Map screen
        composable(route = Screen.Map.route) {
            MapScreen(onBack = { navController.popBackStack() })
        }

        // Phase 7: Team screens
        // composable(route = Screen.TeamLobby.route) { TeamLobbyScreen(...) }
    }
}
