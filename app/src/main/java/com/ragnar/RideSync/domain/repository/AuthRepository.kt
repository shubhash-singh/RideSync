package com.ragnar.RideSync.domain.repository

import com.google.firebase.auth.FirebaseUser
import com.ragnar.RideSync.domain.model.Result
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for authentication operations.
 * Defines the contract for sign-in, sign-out, and auth state observation.
 */
interface AuthRepository : BaseRepository {

    /** Returns the currently signed-in Firebase user, or null if no user is signed in. */
    val currentUser: FirebaseUser?

    /** Returns true if a user is currently authenticated. */
    val isUserAuthenticated: Boolean

    /**
     * Signs in with a Google ID token obtained from Credential Manager.
     * Emits Loading → Success(FirebaseUser) or Error.
     *
     * @param idToken The Google ID token from the credential response.
     */
    fun signInWithGoogle(idToken: String): Flow<Result<FirebaseUser>>

    /**
     * Signs the current user out of Firebase.
     * Emits Loading → Success(Unit) or Error.
     */
    fun signOut(): Flow<Result<Unit>>

    /**
     * Observes the Firebase authentication state.
     * Emits true when a user is signed in, false otherwise.
     */
    fun getAuthStateFlow(): Flow<Boolean>
}
