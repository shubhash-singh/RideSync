package com.ragnar.RideSync.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.messaging.FirebaseMessaging
import com.ragnar.RideSync.domain.model.Result
import com.ragnar.RideSync.domain.model.User
import com.ragnar.RideSync.domain.repository.AuthRepository
import com.ragnar.RideSync.domain.repository.UserRepository
import com.ragnar.RideSync.utils.DebugLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * ViewModel managing the authentication state. Observes Firebase auth state on initialisation and
 * exposes it as a StateFlow for the UI layer to collect.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
        private val authRepository: AuthRepository,
        private val userRepository: UserRepository
) : ViewModel() {

    private companion object {
        private const val TAG = "AuthViewModel"
    }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)

    /** Current authentication state observable by the UI. */
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private var lastProfileSyncUserId: String? = null

    init {
        observeAuthState()
    }

    /**
     * Listens to Firebase auth state changes and maps them to AuthState. If a user is already
     * signed in (e.g. session persistence), the UI will skip the login screen automatically.
     */
    private fun observeAuthState() {
        authRepository
                .getAuthStateFlow()
                .onEach { isAuthenticated ->
                    DebugLogger.d(TAG) { "Auth state changed: isAuthenticated=$isAuthenticated" }
                    _authState.value =
                            if (isAuthenticated) {
                                val user = authRepository.currentUser
                                if (user != null) {
                                    DebugLogger.d(TAG) { "Firebase user available (uid=${user.uid}). Ensuring profile..." }
                                    ensureUserProfile(user)
                                    AuthState.Authenticated(user)
                                } else {
                                    DebugLogger.w(TAG) { "isAuthenticated=true but currentUser is null" }
                                    AuthState.Unauthenticated
                                }
                            } else {
                                lastProfileSyncUserId = null
                                AuthState.Unauthenticated
                            }
                }
                .launchIn(viewModelScope)
    }

    /**
     * Best-effort sync of the authenticated user's profile into Firestore.
     *
     * This is triggered on auth state changes so it also runs for returning users (session
     * persistence). Merge semantics avoid overwriting fields owned by later phases (e.g. team id).
     */
    private fun ensureUserProfile(firebaseUser: FirebaseUser) {
        val uid = firebaseUser.uid
        if (lastProfileSyncUserId == uid) return
        lastProfileSyncUserId = uid

        viewModelScope.launch {
            DebugLogger.d(TAG) { "Fetching FCM token (best-effort)..." }
            val fcmToken =
                    try {
                        FirebaseMessaging.getInstance().token.await()
                    } catch (_: Exception) {
                        null
                    }

            val user =
                    User(
                            id = uid,
                            displayName = firebaseUser.displayName,
                            email = firebaseUser.email,
                            photoUrl = firebaseUser.photoUrl?.toString(),
                            fcmToken = fcmToken
                    )

            userRepository.upsertUser(user).collect { result ->
                when (result) {
                    is Result.Loading -> DebugLogger.d(TAG) { "Upserting user profile to Firestore..." }
                    is Result.Success -> DebugLogger.i(TAG) { "User profile upserted successfully." }
                    is Result.Error ->
                            DebugLogger.e(TAG, result.exception ?: Exception("Unknown error")) {
                                "User profile upsert failed: ${result.message}"
                            }
                }
            }
        }
    }

    /**
     * Signs in with the Google ID token obtained from Credential Manager.
     *
     * @param idToken The Google ID token from the credential response.
     */
    fun signInWithGoogleIdToken(idToken: String) {
        viewModelScope.launch {
            DebugLogger.d(TAG) { "Starting Google sign-in with Firebase..." }
            authRepository.signInWithGoogle(idToken).collect { result ->
                _authState.value =
                        when (result) {
                            is Result.Loading -> AuthState.Loading
                            is Result.Success -> {
                                DebugLogger.i(TAG) { "Firebase sign-in success (uid=${result.data.uid})." }
                                AuthState.Authenticated(result.data)
                            }
                            is Result.Error ->
                                    AuthState.Error(result.message ?: "Sign-in failed. Please try again.")
                        }
            }
        }
    }

    /** Signs out the current user and resets the auth state. */
    fun signOut() {
        viewModelScope.launch {
            DebugLogger.d(TAG) { "Signing out..." }
            authRepository.signOut().collect { result ->
                _authState.value =
                        when (result) {
                            is Result.Loading -> AuthState.Loading
                            is Result.Success -> {
                                DebugLogger.i(TAG) { "Sign-out success." }
                                AuthState.Unauthenticated
                            }
                            is Result.Error -> {
                                DebugLogger.e(TAG) { "Sign-out failed: ${result.message}" }
                                AuthState.Error(result.message ?: "Sign-out failed.")
                            }
                        }
            }
        }
    }

    /** Clears error state, returning to Unauthenticated so the user can retry. */
    fun clearError() {
        _authState.value = AuthState.Unauthenticated
    }
}
