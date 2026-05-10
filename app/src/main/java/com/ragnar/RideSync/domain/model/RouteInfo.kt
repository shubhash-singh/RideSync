package com.ragnar.RideSync.domain.model

/** Lightweight coordinate model used by routing without depending on a map SDK. */
data class RouteCoordinate(val latitude: Double, val longitude: Double)

/** Route summary returned by the directions layer. */
data class RouteInfo(
        val coordinates: List<RouteCoordinate>,
        val distanceMeters: Double,
        val durationSeconds: Double
)
