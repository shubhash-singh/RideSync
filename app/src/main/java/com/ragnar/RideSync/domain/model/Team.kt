package com.ragnar.RideSync.domain.model

/**
 * Domain model representing a collaborative ride team stored in Firestore.
 *
 * Document: teams/{teamId}
 *
 * Phase 7: Team Creation & Joining System
 */
data class Team(
        val id: String,
        val name: String,
        val code: String, // 6-char unique join code
        val leaderId: String,
        val createdAt: Long = 0L,
        val destinationLat: Double? = null,
        val destinationLng: Double? = null,
        val destinationAddress: String? = null
)
