package com.ragnar.RideSync.ui.navigation

/**
 * Sealed class representing all navigation destinations in the app. Each screen has a unique
 * [route] string used by the NavHost.
 */
sealed class Screen(val route: String) {

    /** Home / landing screen */
    data object Home : Screen("home")

    /** Login / authentication screen (Phase 3) */
    data object Login : Screen("login")

    /** User profile screen (Phase 4) */
    data object Profile : Screen("profile")

    /** Map screen showing real-time tracking (Phase 5+) */
    data object Map : Screen("map")

    /** Team lobby — create or join a team (Phase 7) */
    data object TeamLobby : Screen("team_lobby")

    /** Team details — member list and status (Phase 7) */
    data object TeamDetails : Screen("team_details/{teamId}") {
        fun createRoute(teamId: String) = "team_details/$teamId"
    }
}
