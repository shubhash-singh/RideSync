package com.ragnar.RideSync.domain.repository

import com.ragnar.RideSync.domain.model.Result
import com.ragnar.RideSync.domain.model.RouteCoordinate
import com.ragnar.RideSync.domain.model.RouteInfo

/** Retrieves point-to-point routes from the configured directions provider. */
interface DirectionsRepository : BaseRepository {
    suspend fun getRoute(
            origin: RouteCoordinate,
            destination: RouteCoordinate,
            profile: String = "driving"
    ): Result<RouteInfo>
}
