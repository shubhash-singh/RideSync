package com.ragnar.RideSync.domain.model

/**
 * Domain model representing a user profile stored in Firestore.
 *
 * Document: users/{userId}
 */
data class User(
        val id: String,
        val displayName: String? = null,
        val email: String? = null,
        val photoUrl: String? = null,
        val lastLocation: UserLocation? = null,
        val currentTeamId: String? = null,
        val fcmToken: String? = null
)

