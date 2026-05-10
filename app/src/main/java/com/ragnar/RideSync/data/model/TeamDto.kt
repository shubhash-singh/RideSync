package com.ragnar.RideSync.data.model

import androidx.annotation.Keep
import com.ragnar.RideSync.domain.model.Team
import com.ragnar.RideSync.domain.model.TeamMember
import com.ragnar.RideSync.domain.model.UserLocation

// ─── Team DTO ────────────────────────────────────────────────────────────────

/**
 * Firestore DTO for a team document at teams/{teamId}.
 *
 * Phase 7: Team Creation & Joining System
 */
@Keep
data class TeamDto(
        val name: String? = null,
        val code: String? = null,
        val leaderId: String? = null,
        val createdAt: Long? = null,
        val destinationLat: Double? = null,
        val destinationLng: Double? = null,
        val destinationAddress: String? = null
)

fun TeamDto.toDomain(teamId: String): Team =
        Team(
                id = teamId,
                name = name.orEmpty(),
                code = code.orEmpty(),
                leaderId = leaderId.orEmpty(),
                createdAt = createdAt ?: 0L,
                destinationLat = destinationLat,
                destinationLng = destinationLng,
                destinationAddress = destinationAddress
        )

fun Team.toDto(): TeamDto =
        TeamDto(
                name = name,
                code = code,
                leaderId = leaderId,
                createdAt = createdAt,
                destinationLat = destinationLat,
                destinationLng = destinationLng,
                destinationAddress = destinationAddress
        )

// ─── TeamMember DTO ───────────────────────────────────────────────────────────

/** Firestore DTO for a member document at teams/{teamId}/members/{userId}. */
@Keep
data class TeamMemberDto(
        val displayName: String? = null,
        val photoUrl: String? = null,
        val lastLocation: UserLocationDto? = null,
        val joinedAt: Long? = null
)

fun TeamMemberDto.toDomain(userId: String): TeamMember =
        TeamMember(
                userId = userId,
                displayName = displayName,
                photoUrl = photoUrl,
                lastLocation =
                        lastLocation?.let { loc ->
                            val lat = loc.lat
                            val lng = loc.lng
                            if (lat != null && lng != null) UserLocation(lat, lng, loc.updatedAt)
                            else null
                        },
                joinedAt = joinedAt ?: 0L
        )
