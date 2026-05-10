package com.ragnar.RideSync.data.model

import androidx.annotation.Keep
import com.ragnar.RideSync.domain.model.User
import com.ragnar.RideSync.domain.model.UserLocation

/**
 * Firestore DTO for user profiles.
 *
 * Keep property names stable for Firestore (reflection) + ProGuard/R8.
 */
@Keep
data class UserDto(
        val displayName: String? = null,
        val email: String? = null,
        val photoUrl: String? = null,
        val lastLocation: UserLocationDto? = null,
        val currentTeamId: String? = null,
        val fcmToken: String? = null
)

@Keep
data class UserLocationDto(
        val lat: Double? = null,
        val lng: Double? = null,
        val updatedAt: Long? = null
)

fun UserDto.toDomain(userId: String): User =
        User(
                id = userId,
                displayName = displayName,
                email = email,
                photoUrl = photoUrl,
                lastLocation =
                        lastLocation?.let { loc ->
                                val lat = loc.lat
                                val lng = loc.lng
                                if (lat != null && lng != null) {
                                        UserLocation(
                                                latitude = lat,
                                                longitude = lng,
                                                updatedAt = loc.updatedAt
                                        )
                                } else {
                                        null
                                }
                        },
                currentTeamId = currentTeamId,
                fcmToken = fcmToken
        )

fun User.toDto(): UserDto =
        UserDto(
                displayName = displayName,
                email = email,
                photoUrl = photoUrl,
                lastLocation =
                        lastLocation?.let { loc ->
                                UserLocationDto(
                                        lat = loc.latitude,
                                        lng = loc.longitude,
                                        updatedAt = loc.updatedAt
                                )
                        },
                currentTeamId = currentTeamId,
                fcmToken = fcmToken
        )

/** Converts a domain [UserLocation] to its Firestore [UserLocationDto]. */
fun UserLocation.toDto(): UserLocationDto =
        UserLocationDto(lat = latitude, lng = longitude, updatedAt = updatedAt)
