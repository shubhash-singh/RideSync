package com.ragnar.RideSync.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ragnar.RideSync.domain.model.Result
import com.ragnar.RideSync.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * ViewModel managing the authentication state. Observes Firebase auth state on initialisation and
 * exposes it as a StateFlow for the UI layer to collect.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(private val authRepository: AuthRepository) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)

    /** Current authentication state observable by the UI. */
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

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
                    _authState.value =
                            if (isAuthenticated) {
                                val user = authRepository.currentUser
                                if (user != null) AuthState.Authenticated(user)
                                else AuthState.Unauthenticated
                            } else {
                                AuthState.Unauthenticated
                            }
                }
                .launchIn(viewModelScope)
    }

    /**
     * Signs in with the Google ID token obtained from Credential Manager.
     *
     * @param idToken The Google ID token from the credential response.
     */
    fun signInWithGoogleIdToken(idToken: String) {
        viewModelScope.launch {
            authRepository.signInWithGoogle(idToken).collect { result ->
                _authState.value =
                        when (result) {
                            is Result.Loading -> AuthState.Loading
                            is Result.Success -> AuthState.Authenticated(result.data)
                            is Result.Error ->
                                    AuthState.Error(
                                            result.message ?: "Sign-in failed. Please try again."
                                    )
                        }
            }
        }
    }

    /** Signs out the current user and resets the auth state. */
    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut().collect { result ->
                _authState.value =
                        when (result) {
                            is Result.Loading -> AuthState.Loading
                            is Result.Success -> AuthState.Unauthenticated
                            is Result.Error -> AuthState.Error(result.message ?: "Sign-out failed.")
                        }
            }
        }
    }

    /** Clears error state, returning to Unauthenticated so the user can retry. */
    fun clearError() {
        _authState.value = AuthState.Unauthenticated
    }
}
