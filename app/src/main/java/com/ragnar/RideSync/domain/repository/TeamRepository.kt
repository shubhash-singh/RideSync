package com.ragnar.RideSync.domain.repository

import com.ragnar.RideSync.domain.model.Result
import com.ragnar.RideSync.domain.model.Team
import com.ragnar.RideSync.domain.model.TeamMember
import com.ragnar.RideSync.domain.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for team operations stored in Firestore.
 *
 * Phase 7: Team Creation & Joining System
 *
 * Firestore layout teams/{teamId} → Team document teams/{teamId}/members/{userId} → TeamMember
 * document
 */
interface TeamRepository : BaseRepository {

    /**
     * Creates a new team with a randomly generated 6-char code. Sets [currentUser] as the leader
     * and first member.
     */
    fun createTeam(name: String, currentUser: User): Flow<Result<Team>>

    /**
     * Joins an existing team using the 6-char [code]. Returns [Result.Error] if the code is not
     * found.
     */
    fun joinTeamByCode(code: String, currentUser: User): Flow<Result<Team>>

    /**
     * Removes [userId] from the team's members sub-collection and clears the user's [currentTeamId]
     * in their profile document.
     */
    fun leaveTeam(teamId: String, userId: String): Flow<Result<Unit>>

    /**
     * Deletes the entire team document and its members sub-collection. Should only be called by the
     * team leader.
     */
    fun disbandTeam(teamId: String): Flow<Result<Unit>>

    /** Updates the team's shared destination on the team document. */
    fun setDestination(
            teamId: String,
            latitude: Double,
            longitude: Double,
            address: String?
    ): Flow<Result<Unit>>

    /** Real-time snapshot of the team document. Emits Success(null) when missing. */
    fun observeTeam(teamId: String): Flow<Result<Team?>>

    /** Real-time snapshot of the members sub-collection. */
    fun observeMembers(teamId: String): Flow<Result<List<TeamMember>>>
}
