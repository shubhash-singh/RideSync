package com.ragnar.RideSync.ui.screens.map

import android.content.Context
import android.location.Location
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.ragnar.RideSync.domain.model.User
import com.ragnar.RideSync.domain.model.UserLocation
import com.ragnar.RideSync.domain.repository.LocationRepository
import com.ragnar.RideSync.domain.repository.UserRepository
import com.ragnar.RideSync.service.LocationForegroundService
import com.ragnar.RideSync.utils.DebugLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * ViewModel responsible for real-time location tracking (Phase 6).
 *
 * – Collects [LocationRepository.observeLocation] and exposes the latest [Location] as a
 * [StateFlow]. – On every location emission writes the updated [UserLocation] to Firestore via
 * [UserRepository.upsertUser] so all future phases have fresh data.
 *
 * Phase 11: Also manages the [LocationForegroundService] lifecycle so background tracking
 * continues when the app is minimised.
 */
@HiltViewModel
class LocationViewModel
@Inject
constructor(
        private val locationRepository: LocationRepository,
        private val userRepository: UserRepository,
        private val auth: FirebaseAuth
) : ViewModel() {

    private companion object {
        private const val TAG = "LocationViewModel"
    }

    private val _locationState = MutableStateFlow<Location?>(null)

    /** The most recently received device location, or null before first fix. */
    val locationState: StateFlow<Location?> = _locationState.asStateFlow()

    /**
     * Starts collecting location updates and writing them to Firestore. Must be called only after
     * location permission is granted.
     */
    fun startTracking() {
        DebugLogger.d(TAG) { "startTracking() called" }

        locationRepository
                .observeLocation()
                .onEach { location ->
                    _locationState.value = location
                    pushToFirestore(location)
                }
                .catch { e ->
                    DebugLogger.e(TAG, e) { "Location flow error: ${e.localizedMessage}" }
                }
                .launchIn(viewModelScope)
    }

    // ── Phase 11: Foreground service management ───────────────────────────────

    /**
     * Starts [LocationForegroundService] so location tracking continues when the app is
     * backgrounded. Safe to call multiple times — the service ignores repeated ACTION_START.
     * Must be called only after location permission is granted.
     */
    fun startForegroundService(context: Context) {
        DebugLogger.d(TAG) { "startForegroundService()" }
        val intent = LocationForegroundService.startIntent(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(context, intent)
        } else {
            context.startService(intent)
        }
    }

    /**
     * Sends [LocationForegroundService.ACTION_STOP] to gracefully shut down the background
     * tracking service (e.g. when the user leaves the team).
     */
    fun stopForegroundService(context: Context) {
        DebugLogger.d(TAG) { "stopForegroundService()" }
        context.startService(LocationForegroundService.stopIntent(context))
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun pushToFirestore(location: Location) {
        val uid =
                auth.currentUser?.uid
                        ?: run {
                            DebugLogger.w(TAG) { "pushToFirestore – no signed-in user, skipping" }
                            return
                        }
        val userLocation =
                UserLocation(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        updatedAt = System.currentTimeMillis()
                )
        val userShell = User(id = uid, lastLocation = userLocation)

        viewModelScope.launch {
            userRepository.upsertUser(userShell).collect { result ->
                DebugLogger.d(TAG) { "Firestore location write result: $result" }
            }
        }
    }
}

