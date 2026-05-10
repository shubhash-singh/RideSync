package com.ragnar.RideSync.domain.model

/**
 * Domain model representing a member within a team.
 *
 * Document: teams/{teamId}/members/{userId}
 *
 * Phase 7: Team Creation & Joining System
 */
data class TeamMember(
        val userId: String,
        val displayName: String? = null,
        val photoUrl: String? = null,
        val lastLocation: UserLocation? = null,
        val joinedAt: Long = 0L
)
