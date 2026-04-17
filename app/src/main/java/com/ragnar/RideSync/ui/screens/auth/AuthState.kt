package com.ragnar.RideSync.ui.screens.auth

import com.google.firebase.auth.FirebaseUser

/**
 * Sealed interface representing the different states of the authentication flow. Observed by the UI
 * to render the appropriate screen content.
 */
sealed interface AuthState {

    /** Initial state — auth status has not been determined yet. */
    data object Idle : AuthState

    /** An authentication operation is in progress. */
    data object Loading : AuthState

    /** User is successfully authenticated. */
    data class Authenticated(val user: FirebaseUser) : AuthState

    /** No user is signed in. */
    data object Unauthenticated : AuthState

    /** An error occurred during authentication. */
    data class Error(val message: String) : AuthState
}
