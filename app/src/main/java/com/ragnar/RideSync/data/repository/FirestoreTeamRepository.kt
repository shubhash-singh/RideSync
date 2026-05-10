package com.ragnar.RideSync.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.ragnar.RideSync.data.model.TeamDto
import com.ragnar.RideSync.data.model.TeamMemberDto
import com.ragnar.RideSync.data.model.toDomain
import com.ragnar.RideSync.data.model.toDto
import com.ragnar.RideSync.domain.model.Result
import com.ragnar.RideSync.domain.model.Team
import com.ragnar.RideSync.domain.model.TeamMember
import com.ragnar.RideSync.domain.model.User
import com.ragnar.RideSync.domain.repository.TeamRepository
import com.ragnar.RideSync.utils.Constants
import com.ragnar.RideSync.utils.DebugLogger
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

/**
 * Firestore implementation of [TeamRepository].
 *
 * Firestore layout: teams/{teamId} → TeamDto teams/{teamId}/members/{userId} → TeamMemberDto
 * users/{userId}.currentTeamId → updated on create/join/leave/disband
 *
 * Phase 7: Team Creation & Joining System
 */
@Singleton
class FirestoreTeamRepository @Inject constructor(private val firestore: FirebaseFirestore) :
        TeamRepository {

    private companion object {
        private const val TAG = "FirestoreTeamRepo"
        private val CODE_CHARS = ('A'..'Z') + ('0'..'9')
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Mutation operations
    // ──────────────────────────────────────────────────────────────────────────

    override fun createTeam(name: String, currentUser: User): Flow<Result<Team>> = flow {
        emit(Result.Loading)
        DebugLogger.d(TAG) { "createTeam(name=$name, userId=${currentUser.id})" }
        try {
            val code = generateCode()
            val teamRef = firestore.collection(Constants.COLLECTION_TEAMS).document()
            val teamId = teamRef.id
            val now = System.currentTimeMillis()

            val teamDto =
                    TeamDto(
                            name = name.trim(),
                            code = code,
                            leaderId = currentUser.id,
                            createdAt = now
                    )

            val memberDto = buildMemberDto(currentUser, now)

            firestore
                    .runBatch { batch ->
                        // Write team document
                        batch.set(teamRef, teamDto)
                        // Add creator as first member
                        batch.set(
                                teamRef.collection(Constants.COLLECTION_MEMBERS)
                                        .document(currentUser.id),
                                memberDto
                        )
                        // Mark user as belonging to this team
                        batch.update(
                                firestore
                                        .collection(Constants.COLLECTION_USERS)
                                        .document(currentUser.id),
                                "currentTeamId",
                                teamId
                        )
                    }
                    .await()

            val team = teamDto.toDomain(teamId)
            DebugLogger.i(TAG) { "createTeam success: teamId=$teamId code=$code" }
            emit(Result.Success(team))
        } catch (e: Exception) {
            DebugLogger.e(TAG, e) { "createTeam failed: ${e.localizedMessage}" }
            emit(Result.Error(exception = e, message = e.localizedMessage))
        }
    }

    override fun joinTeamByCode(code: String, currentUser: User): Flow<Result<Team>> = flow {
        emit(Result.Loading)
        DebugLogger.d(TAG) { "joinTeamByCode(code=$code userId=${currentUser.id})" }
        try {
            // Query team by code
            val querySnapshot =
                    firestore
                            .collection(Constants.COLLECTION_TEAMS)
                            .whereEqualTo("code", code.trim().uppercase())
                            .limit(1)
                            .get()
                            .await()

            if (querySnapshot.isEmpty) {
                DebugLogger.w(TAG) { "joinTeamByCode: code=$code not found" }
                emit(Result.Error(message = "Team not found. Double-check the code and try again."))
                return@flow
            }

            val teamDoc = querySnapshot.documents.first()
            val teamId = teamDoc.id
            val teamDto =
                    teamDoc.toObject(TeamDto::class.java)
                            ?: run {
                                emit(Result.Error(message = "Failed to parse team data."))
                                return@flow
                            }

            val now = System.currentTimeMillis()
            val memberDto = buildMemberDto(currentUser, now)

            firestore
                    .runBatch { batch ->
                        batch.set(
                                firestore
                                        .collection(Constants.COLLECTION_TEAMS)
                                        .document(teamId)
                                        .collection(Constants.COLLECTION_MEMBERS)
                                        .document(currentUser.id),
                                memberDto
                        )
                        batch.update(
                                firestore
                                        .collection(Constants.COLLECTION_USERS)
                                        .document(currentUser.id),
                                "currentTeamId",
                                teamId
                        )
                    }
                    .await()

            val team = teamDto.toDomain(teamId)
            DebugLogger.i(TAG) { "joinTeamByCode success: teamId=$teamId" }
            emit(Result.Success(team))
        } catch (e: Exception) {
            DebugLogger.e(TAG, e) { "joinTeamByCode failed: ${e.localizedMessage}" }
            emit(Result.Error(exception = e, message = e.localizedMessage))
        }
    }

    override fun leaveTeam(teamId: String, userId: String): Flow<Result<Unit>> = flow {
        emit(Result.Loading)
        DebugLogger.d(TAG) { "leaveTeam(teamId=$teamId userId=$userId)" }
        try {
            firestore
                    .runBatch { batch ->
                        batch.delete(
                                firestore
                                        .collection(Constants.COLLECTION_TEAMS)
                                        .document(teamId)
                                        .collection(Constants.COLLECTION_MEMBERS)
                                        .document(userId)
                        )
                        batch.update(
                                firestore.collection(Constants.COLLECTION_USERS).document(userId),
                                "currentTeamId",
                                null
                        )
                    }
                    .await()
            DebugLogger.i(TAG) { "leaveTeam success: userId=$userId left teamId=$teamId" }
            emit(Result.Success(Unit))
        } catch (e: Exception) {
            DebugLogger.e(TAG, e) { "leaveTeam failed: ${e.localizedMessage}" }
            emit(Result.Error(exception = e, message = e.localizedMessage))
        }
    }

    override fun disbandTeam(teamId: String): Flow<Result<Unit>> = flow {
        emit(Result.Loading)
        DebugLogger.d(TAG) { "disbandTeam(teamId=$teamId)" }
        try {
            val teamRef = firestore.collection(Constants.COLLECTION_TEAMS).document(teamId)

            // Fetch all member IDs so we can clear their currentTeamId
            val membersSnapshot = teamRef.collection(Constants.COLLECTION_MEMBERS).get().await()

            firestore
                    .runBatch { batch ->
                        // Delete each member document and clear their currentTeamId
                        membersSnapshot.documents.forEach { memberDoc ->
                            batch.delete(memberDoc.reference)
                            batch.update(
                                    firestore
                                            .collection(Constants.COLLECTION_USERS)
                                            .document(memberDoc.id),
                                    "currentTeamId",
                                    null
                            )
                        }
                        // Delete the team document itself
                        batch.delete(teamRef)
                    }
                    .await()
            DebugLogger.i(TAG) { "disbandTeam success: teamId=$teamId" }
            emit(Result.Success(Unit))
        } catch (e: Exception) {
            DebugLogger.e(TAG, e) { "disbandTeam failed: ${e.localizedMessage}" }
            emit(Result.Error(exception = e, message = e.localizedMessage))
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Real-time observers
    // ──────────────────────────────────────────────────────────────────────────

    override fun observeTeam(teamId: String): Flow<Result<Team?>> = callbackFlow {
        DebugLogger.d(TAG) { "observeTeam(teamId=$teamId) start" }
        trySend(Result.Loading)

        val registration =
                firestore
                        .collection(Constants.COLLECTION_TEAMS)
                        .document(teamId)
                        .addSnapshotListener { snapshot, error ->
                            if (error != null) {
                                DebugLogger.e(TAG, error) {
                                    "observeTeam error: ${error.localizedMessage}"
                                }
                                trySend(
                                        Result.Error(
                                                exception = error,
                                                message = error.localizedMessage
                                        )
                                )
                                return@addSnapshotListener
                            }
                            if (snapshot == null || !snapshot.exists()) {
                                DebugLogger.d(TAG) { "observeTeam: team $teamId missing/deleted" }
                                trySend(Result.Success(null))
                                return@addSnapshotListener
                            }
                            val dto = snapshot.toObject(TeamDto::class.java)
                            val team = dto?.toDomain(teamId)
                            DebugLogger.d(TAG) { "observeTeam update: team=$team" }
                            trySend(Result.Success(team))
                        }

        awaitClose { registration.remove() }
    }

    override fun observeMembers(teamId: String): Flow<Result<List<TeamMember>>> = callbackFlow {
        DebugLogger.d(TAG) { "observeMembers(teamId=$teamId) start" }
        trySend(Result.Loading)

        val registration =
                firestore
                        .collection(Constants.COLLECTION_TEAMS)
                        .document(teamId)
                        .collection(Constants.COLLECTION_MEMBERS)
                        .addSnapshotListener { snapshot, error ->
                            if (error != null) {
                                DebugLogger.e(TAG, error) {
                                    "observeMembers error: ${error.localizedMessage}"
                                }
                                trySend(
                                        Result.Error(
                                                exception = error,
                                                message = error.localizedMessage
                                        )
                                )
                                return@addSnapshotListener
                            }
                            val members =
                                    snapshot?.documents?.mapNotNull { doc ->
                                        doc.toObject(TeamMemberDto::class.java)?.toDomain(doc.id)
                                    }
                                            ?: emptyList()
                            DebugLogger.d(TAG) { "observeMembers update: ${members.size} members" }
                            trySend(Result.Success(members))
                        }

        awaitClose { registration.remove() }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ──────────────────────────────────────────────────────────────────────────

    private fun generateCode(): String =
            (1..Constants.TEAM_CODE_LENGTH).map { CODE_CHARS.random() }.joinToString("")

    private fun buildMemberDto(user: User, joinedAt: Long): TeamMemberDto =
            TeamMemberDto(
                    displayName = user.displayName,
                    photoUrl = user.photoUrl,
                    lastLocation = user.lastLocation?.toDto(),
                    joinedAt = joinedAt
            )
}
