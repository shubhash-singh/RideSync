package com.ragnar.RideSync.ui.screens.team

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.ragnar.RideSync.domain.model.Result
import com.ragnar.RideSync.domain.model.Team
import com.ragnar.RideSync.domain.model.TeamMember
import com.ragnar.RideSync.domain.model.User
import com.ragnar.RideSync.domain.repository.TeamRepository
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

/** Sealed UI state for the team screens. */
sealed interface TeamUiState {
    /** No team operation in progress; user has no active team. */
    data object Idle : TeamUiState

    /** A team operation (create/join/leave/disband) is in progress. */
    data object Loading : TeamUiState

    /** User is a member of [team] with the given [members] list. */
    data class InTeam(val team: Team, val members: List<TeamMember>) : TeamUiState

    /** An error occurred. [message] is user-displayable. */
    data class Error(val message: String) : TeamUiState
}

/**
 * ViewModel for the Team screens (Phase 7).
 *
 * On initialisation, checks if the signed-in user already belongs to a team (via Firestore user
 * doc) and, if so, starts observing that team. After each mutation (create/join/leave/disband) it
 * navigates the state machine accordingly.
 */
@HiltViewModel
class TeamViewModel
@Inject
constructor(
        private val teamRepository: TeamRepository,
        private val userRepository: UserRepository,
        private val auth: FirebaseAuth
) : ViewModel() {

    private companion object {
        private const val TAG = "TeamViewModel"
    }

    private val _uiState = MutableStateFlow<TeamUiState>(TeamUiState.Idle)
    val uiState: StateFlow<TeamUiState> = _uiState.asStateFlow()

    init {
        loadExistingTeamIfAny()
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Init — resume existing team session
    // ──────────────────────────────────────────────────────────────────────────

    private fun loadExistingTeamIfAny() {
        val uid = auth.currentUser?.uid ?: return
        DebugLogger.d(TAG) { "Checking for existing team (uid=$uid)" }

        userRepository
                .observeUser(uid)
                .onEach { result ->
                    if (result is Result.Success) {
                        val teamId = result.data?.currentTeamId
                        if (!teamId.isNullOrBlank()) {
                            DebugLogger.d(TAG) {
                                "User belongs to team=$teamId, starting observers"
                            }
                            startObservingTeam(teamId)
                        } else if (_uiState.value is TeamUiState.Idle ||
                                        _uiState.value is TeamUiState.Loading
                        ) {
                            // No team yet — make sure we're Idle (not loading)
                        }
                    }
                }
                .launchIn(viewModelScope)
    }

    /** Subscribes to both the team doc and its members sub-collection. */
    private fun startObservingTeam(teamId: String) {
        // Observe team document
        teamRepository
                .observeTeam(teamId)
                .onEach { result ->
                    when (result) {
                        is Result.Loading -> Unit // keep current state while loading
                        is Result.Success -> {
                            val team = result.data
                            if (team == null) {
                                // Team was disbanded by someone else
                                DebugLogger.i(TAG) {
                                    "Team $teamId deleted remotely, resetting to Idle"
                                }
                                _uiState.value = TeamUiState.Idle
                            } else {
                                val currentMembers =
                                        (_uiState.value as? TeamUiState.InTeam)?.members
                                                ?: emptyList()
                                _uiState.value = TeamUiState.InTeam(team, currentMembers)
                            }
                        }
                        is Result.Error ->
                                _uiState.value =
                                        TeamUiState.Error(result.message ?: "Failed to load team")
                    }
                }
                .launchIn(viewModelScope)

        // Observe members sub-collection
        teamRepository
                .observeMembers(teamId)
                .onEach { result ->
                    when (result) {
                        is Result.Loading -> Unit
                        is Result.Success -> {
                            val members = result.data
                            val currentTeam = (_uiState.value as? TeamUiState.InTeam)?.team
                            if (currentTeam != null) {
                                _uiState.value = TeamUiState.InTeam(currentTeam, members)
                            }
                        }
                        is Result.Error ->
                                DebugLogger.e(TAG) { "Members observe error: ${result.message}" }
                    }
                }
                .launchIn(viewModelScope)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Actions
    // ──────────────────────────────────────────────────────────────────────────

    fun createTeam(name: String) {
        if (name.isBlank()) {
            _uiState.value = TeamUiState.Error("Team name cannot be empty.")
            return
        }
        val user = buildCurrentUser() ?: return
        viewModelScope.launch {
            teamRepository.createTeam(name.trim(), user).collect { result ->
                when (result) {
                    is Result.Loading -> _uiState.value = TeamUiState.Loading
                    is Result.Success -> {
                        DebugLogger.i(TAG) { "Team created: ${result.data.id}" }
                        startObservingTeam(result.data.id)
                        _uiState.value = TeamUiState.InTeam(result.data, emptyList())
                    }
                    is Result.Error ->
                            _uiState.value =
                                    TeamUiState.Error(result.message ?: "Failed to create team.")
                }
            }
        }
    }

    fun joinTeam(code: String) {
        if (code.isBlank()) {
            _uiState.value = TeamUiState.Error("Please enter a team code.")
            return
        }
        val user = buildCurrentUser() ?: return
        viewModelScope.launch {
            teamRepository.joinTeamByCode(code.trim().uppercase(), user).collect { result ->
                when (result) {
                    is Result.Loading -> _uiState.value = TeamUiState.Loading
                    is Result.Success -> {
                        DebugLogger.i(TAG) { "Joined team: ${result.data.id}" }
                        startObservingTeam(result.data.id)
                        _uiState.value = TeamUiState.InTeam(result.data, emptyList())
                    }
                    is Result.Error ->
                            _uiState.value =
                                    TeamUiState.Error(result.message ?: "Failed to join team.")
                }
            }
        }
    }

    fun leaveTeam() {
        val state = _uiState.value as? TeamUiState.InTeam ?: return
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            teamRepository.leaveTeam(state.team.id, uid).collect { result ->
                when (result) {
                    is Result.Loading -> _uiState.value = TeamUiState.Loading
                    is Result.Success -> {
                        DebugLogger.i(TAG) { "Left team: ${state.team.id}" }
                        _uiState.value = TeamUiState.Idle
                    }
                    is Result.Error ->
                            _uiState.value =
                                    TeamUiState.Error(result.message ?: "Failed to leave team.")
                }
            }
        }
    }

    fun disbandTeam() {
        val state = _uiState.value as? TeamUiState.InTeam ?: return
        viewModelScope.launch {
            teamRepository.disbandTeam(state.team.id).collect { result ->
                when (result) {
                    is Result.Loading -> _uiState.value = TeamUiState.Loading
                    is Result.Success -> {
                        DebugLogger.i(TAG) { "Disbanded team: ${state.team.id}" }
                        _uiState.value = TeamUiState.Idle
                    }
                    is Result.Error ->
                            _uiState.value =
                                    TeamUiState.Error(result.message ?: "Failed to disband team.")
                }
            }
        }
    }

    /** Clears an error state back to Idle so the user can retry. */
    fun clearError() {
        _uiState.value = TeamUiState.Idle
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ──────────────────────────────────────────────────────────────────────────

    private fun buildCurrentUser(): User? {
        val firebaseUser = auth.currentUser
        if (firebaseUser == null) {
            DebugLogger.w(TAG) { "No signed-in user" }
            _uiState.value = TeamUiState.Error("Not signed in.")
            return null
        }
        return User(
                id = firebaseUser.uid,
                displayName = firebaseUser.displayName,
                photoUrl = firebaseUser.photoUrl?.toString()
        )
    }
}
