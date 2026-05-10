package com.ragnar.RideSync.ui.screens.map

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ragnar.RideSync.domain.model.Result
import com.ragnar.RideSync.domain.model.TeamMember
import com.ragnar.RideSync.domain.repository.TeamRepository
import com.ragnar.RideSync.utils.DebugLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * ViewModel for Phase 8 — real-time team member location sync on the map.
 *
 * Reads the optional [teamId] argument from [SavedStateHandle] (injected by Navigation when the map
 * is opened from a team context). Subscribes to [TeamRepository.observeMembers] and exposes the
 * member list as a [StateFlow].
 */
@HiltViewModel
class TeamMapViewModel
@Inject
constructor(savedStateHandle: SavedStateHandle, private val teamRepository: TeamRepository) :
        ViewModel() {

    private companion object {
        private const val TAG = "TeamMapViewModel"
        const val KEY_TEAM_ID = "teamId"
    }

    /** Optional team id; null when the map is opened without a team context. */
    val teamId: String? = savedStateHandle[KEY_TEAM_ID]

    private val _members = MutableStateFlow<List<TeamMember>>(emptyList())

    /** Live list of team members (those with a lastLocation will render as markers). */
    val members: StateFlow<List<TeamMember>> = _members.asStateFlow()

    init {
        val id = teamId
        if (!id.isNullOrBlank()) {
            DebugLogger.d(TAG) { "Observing members for team=$id" }
            teamRepository
                    .observeMembers(id)
                    .onEach { result ->
                        if (result is Result.Success) {
                            _members.value = result.data
                        }
                    }
                    .launchIn(viewModelScope)
        }
    }
}
