package com.ragnar.RideSync.data.repository

import android.location.Location
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.ragnar.RideSync.domain.repository.LocationRepository
import com.ragnar.RideSync.utils.Constants
import com.ragnar.RideSync.utils.DebugLogger
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Implementation of [LocationRepository] backed by [FusedLocationProviderClient].
 *
 * Phase 6: Real-Time Location Tracking
 *
 * Emits a new [Location] every ~5 seconds (fastest: 2 s) while the flow is collected. Location
 * updates are removed automatically when the flow is cancelled (e.g. ViewModel is cleared or user
 * navigates away).
 *
 * IMPORTANT: The caller MUST hold ACCESS_FINE_LOCATION or ACCESS_COARSE_LOCATION before collecting
 * this flow. A [SecurityException] is caught and the flow is closed with the exception rather than
 * crashing the app.
 */
@Singleton
class FusedLocationRepository
@Inject
constructor(private val fusedClient: FusedLocationProviderClient) : LocationRepository {

    private companion object {
        private const val TAG = "FusedLocationRepo"
    }

    @Suppress("MissingPermission") // Permission checked by MapScreen before collection starts.
    override fun observeLocation(): Flow<Location> = callbackFlow {
        DebugLogger.d(TAG) { "observeLocation() – registering callback" }

        val request =
                LocationRequest.Builder(
                                Priority.PRIORITY_HIGH_ACCURACY,
                                Constants.LOCATION_UPDATE_INTERVAL
                        )
                        .setMinUpdateIntervalMillis(Constants.LOCATION_FASTEST_INTERVAL)
                        .setWaitForAccurateLocation(false)
                        .build()

        val callback =
                object : LocationCallback() {
                    override fun onLocationResult(result: LocationResult) {
                        val location = result.lastLocation ?: return
                        DebugLogger.d(TAG) {
                            "Location update: lat=${location.latitude}, lng=${location.longitude}"
                        }
                        trySend(location)
                    }
                }

        try {
            fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            DebugLogger.e(TAG, e) {
                "Missing location permission – closing flow: ${e.localizedMessage}"
            }
            close(e)
        }

        awaitClose {
            DebugLogger.d(TAG) { "observeLocation() – removing callback" }
            fusedClient.removeLocationUpdates(callback)
        }
    }
}
