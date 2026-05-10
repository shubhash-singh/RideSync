package com.ragnar.RideSync.ui.screens.map

import android.content.Context
import android.location.Geocoder
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.ragnar.RideSync.domain.model.Result
import com.ragnar.RideSync.domain.model.Team
import com.ragnar.RideSync.domain.model.TeamMember
import com.ragnar.RideSync.domain.repository.TeamRepository
import com.ragnar.RideSync.domain.repository.UserRepository
import com.ragnar.RideSync.utils.DebugLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface DestinationUiState {
    data object Idle : DestinationUiState
    data object Saving : DestinationUiState
    data class Saved(val address: String) : DestinationUiState
    data class Error(val message: String) : DestinationUiState
}

/**
 * ViewModel for team-aware map state.
 *
 * Phase 8 observes team members. Phase 9 observes and updates the shared team destination.
 */
@HiltViewModel
class TeamMapViewModel
@Inject
constructor(
        savedStateHandle: SavedStateHandle,
        private val teamRepository: TeamRepository,
        private val userRepository: UserRepository,
        private val auth: FirebaseAuth,
        @ApplicationContext private val context: Context
) : ViewModel() {

    private companion object {
        private const val TAG = "TeamMapViewModel"
        const val KEY_TEAM_ID = "teamId"
    }

    private val navTeamId: String? = savedStateHandle[KEY_TEAM_ID]
    val currentUserId: String? = auth.currentUser?.uid

    private val _teamId = MutableStateFlow<String?>(null)
    private val _team = MutableStateFlow<Team?>(null)
    private val _members = MutableStateFlow<List<TeamMember>>(emptyList())
    private val _destinationState = MutableStateFlow<DestinationUiState>(DestinationUiState.Idle)

    val teamId: StateFlow<String?> = _teamId.asStateFlow()
    val team: StateFlow<Team?> = _team.asStateFlow()

    /** Live list of team members (those with a lastLocation will render as markers). */
    val members: StateFlow<List<TeamMember>> = _members.asStateFlow()
    val destinationState: StateFlow<DestinationUiState> = _destinationState.asStateFlow()

    private var observedTeamId: String? = null
    private var teamJob: Job? = null
    private var membersJob: Job? = null

    init {
        if (!navTeamId.isNullOrBlank()) {
            observeTeamContext(navTeamId)
        } else {
            observeCurrentUserTeam()
        }
    }

    fun setDestination(latitude: Double, longitude: Double) {
        val id = _teamId.value
        val activeTeam = _team.value
        if (id.isNullOrBlank() || activeTeam == null) {
            _destinationState.value = DestinationUiState.Error("Join a team before setting a destination.")
            return
        }
        if (currentUserId == null || currentUserId != activeTeam.leaderId) {
            _destinationState.value =
                    DestinationUiState.Error("Only the team leader can set the destination.")
            return
        }

        viewModelScope.launch {
            _destinationState.value = DestinationUiState.Saving
            val address = reverseGeocode(latitude, longitude)
            teamRepository.setDestination(id, latitude, longitude, address).collect { result ->
                when (result) {
                    Result.Loading -> _destinationState.value = DestinationUiState.Saving
                    is Result.Success -> _destinationState.value = DestinationUiState.Saved(address)
                    is Result.Error ->
                            _destinationState.value =
                                    DestinationUiState.Error(
                                            result.message ?: "Failed to set destination."
                                    )
                }
            }
        }
    }

    fun clearDestinationMessage() {
        _destinationState.value = DestinationUiState.Idle
    }

    private fun observeCurrentUserTeam() {
        val uid = currentUserId
        if (uid == null) {
            DebugLogger.w(TAG) { "No signed-in user; team map context unavailable" }
            return
        }
        userRepository
                .observeUser(uid)
                .onEach { result ->
                    if (result is Result.Success) {
                        observeTeamContext(result.data?.currentTeamId)
                    }
                }
                .launchIn(viewModelScope)
    }

    private fun observeTeamContext(teamId: String?) {
        if (observedTeamId == teamId) return
        observedTeamId = teamId
        teamJob?.cancel()
        membersJob?.cancel()

        if (teamId.isNullOrBlank()) {
            _teamId.value = null
            _team.value = null
            _members.value = emptyList()
            return
        }

        _teamId.value = teamId
        DebugLogger.d(TAG) { "Observing map team context for team=$teamId" }

        teamJob =
                teamRepository
                        .observeTeam(teamId)
                        .onEach { result ->
                            when (result) {
                                Result.Loading -> Unit
                                is Result.Success -> _team.value = result.data
                                is Result.Error ->
                                        DebugLogger.e(TAG) {
                                            "Team observe error: ${result.message}"
                                        }
                            }
                        }
                        .launchIn(viewModelScope)

        membersJob =
                teamRepository
                        .observeMembers(teamId)
                        .onEach { result ->
                            when (result) {
                                Result.Loading -> Unit
                                is Result.Success -> _members.value = result.data
                                is Result.Error ->
                                        DebugLogger.e(TAG) {
                                            "Members observe error: ${result.message}"
                                        }
                            }
                        }
                        .launchIn(viewModelScope)
    }

    private suspend fun reverseGeocode(latitude: Double, longitude: Double): String =
            withContext(Dispatchers.IO) {
                runCatching {
                            @Suppress("DEPRECATION")
                            Geocoder(context, Locale.getDefault())
                                    .getFromLocation(latitude, longitude, 1)
                                    ?.firstOrNull()
                                    ?.let { address ->
                                        address.getAddressLine(0)?.takeIf { it.isNotBlank() }
                                                ?: listOfNotNull(
                                                                address.locality,
                                                                address.adminArea,
                                                                address.countryName
                                                        )
                                                        .joinToString(", ")
                                                        .takeIf { it.isNotBlank() }
                                    }
                        }
                        .getOrNull()
                        ?: String.format(Locale.US, "%.5f, %.5f", latitude, longitude)
            }
}
