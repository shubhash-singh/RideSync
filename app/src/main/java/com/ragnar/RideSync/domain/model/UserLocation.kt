package com.ragnar.RideSync.domain.model

/**
 * Last known user location stored in Firestore.
 *
 * Phase 4 stores this as part of the user profile; later phases will update it in real time.
 */
data class UserLocation(
        val latitude: Double,
        val longitude: Double,
        val updatedAt: Long? = null
)

