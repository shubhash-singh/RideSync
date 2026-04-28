package com.ragnar.RideSync.domain.repository

import android.location.Location
import kotlinx.coroutines.flow.Flow

/**
 * Domain interface for continuous device-location observations.
 *
 * Phase 6: Real-Time Location Tracking
 * Implementations use FusedLocationProviderClient under the hood.
 */
interface LocationRepository : BaseRepository {

    /**
     * Returns a cold [Flow] that emits a new [Location] whenever the device position
     * changes (targeting 5 s interval). The flow stays active until the collector
     * is cancelled, which automatically removes the underlying location callback.
     */
    fun observeLocation(): Flow<Location>
}
