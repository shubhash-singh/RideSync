package com.ragnar.RideSync.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ragnar.RideSync.domain.model.Result
import com.ragnar.RideSync.domain.model.User
import com.ragnar.RideSync.domain.repository.AuthRepository
import com.ragnar.RideSync.domain.repository.UserRepository
import com.ragnar.RideSync.utils.DebugLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@HiltViewModel
class UserViewModel @Inject constructor(
        private val authRepository: AuthRepository,
        private val userRepository: UserRepository
) : ViewModel() {

    private companion object {
        private const val TAG = "UserViewModel"
    }

    private val _uiState = MutableStateFlow<UserUiState>(UserUiState.Loading)
    val uiState: StateFlow<UserUiState> = _uiState.asStateFlow()

    private var userJob: Job? = null
    private var lastUserId: String? = null

    init {
        DebugLogger.d(TAG) { "init" }
        authRepository
                .getAuthStateFlow()
                .onEach { isAuthenticated ->
                    DebugLogger.d(TAG) { "Auth observed: isAuthenticated=$isAuthenticated" }
                    if (!isAuthenticated) {
                        userJob?.cancel()
                        userJob = null
                        lastUserId = null
                        _uiState.value = UserUiState.Unauthenticated
                        return@onEach
                    }

                    val uid = authRepository.currentUser?.uid
                    if (uid.isNullOrBlank()) {
                        DebugLogger.w(TAG) { "Authenticated but currentUser uid is blank" }
                        _uiState.value = UserUiState.Unauthenticated
                        return@onEach
                    }

                    if (uid == lastUserId) return@onEach
                    lastUserId = uid

                    DebugLogger.d(TAG) { "Observing Firestore user profile (uid=$uid)" }
                    userJob?.cancel()
                    userJob =
                            userRepository
                                    .observeUser(uid)
                                    .onEach { result ->
                                        _uiState.value = result.toUiState()
                                        when (result) {
                                            is Result.Loading ->
                                                    DebugLogger.d(TAG) {
                                                        "Profile load: Loading"
                                                    }
                                            is Result.Success ->
                                                    DebugLogger.i(TAG) {
                                                        "Profile load: Success (exists=${result.data != null})"
                                                    }
                                            is Result.Error ->
                                                    DebugLogger.e(
                                                            TAG,
                                                            result.exception
                                                                    ?: Exception("Unknown error")
                                                    ) {
                                                        "Profile load: Error (${result.message})"
                                                    }
                                        }
                                    }
                                    .launchIn(viewModelScope)
                }
                .launchIn(viewModelScope)
    }

    private fun Result<User?>.toUiState(): UserUiState =
            when (this) {
                is Result.Loading -> UserUiState.Loading
                is Result.Error -> UserUiState.Error(message ?: "Failed to load profile.")
                is Result.Success ->
                        data?.let { UserUiState.Data(it) } ?: UserUiState.Loading
            }
}

sealed interface UserUiState {
    data object Loading : UserUiState

    data object Unauthenticated : UserUiState

    data class Data(val user: User) : UserUiState

    data class Error(val message: String) : UserUiState
}
