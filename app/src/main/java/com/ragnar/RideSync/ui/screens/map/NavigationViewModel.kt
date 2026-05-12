package com.ragnar.RideSync.ui.screens.map

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ragnar.RideSync.domain.model.Result
import com.ragnar.RideSync.domain.model.RouteCoordinate
import com.ragnar.RideSync.domain.model.RouteInfo
import com.ragnar.RideSync.domain.repository.DirectionsRepository
import com.ragnar.RideSync.utils.DebugLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface NavigationUiState {
    data object Idle : NavigationUiState
    data object Loading : NavigationUiState
    data class RouteReady(val route: RouteInfo) : NavigationUiState
    data class Error(val message: String) : NavigationUiState
}

/** ViewModel for Phase 10 route fetching and re-routing. */
@HiltViewModel
class NavigationViewModel @Inject constructor(private val directionsRepository: DirectionsRepository) :
        ViewModel() {

    private companion object {
        private const val TAG = "NavigationViewModel"
        private const val REROUTE_DISTANCE_METERS = 50f
        private const val DESTINATION_CHANGE_METERS = 20f
    }

    private val _routeState = MutableStateFlow<NavigationUiState>(NavigationUiState.Idle)
    val routeState: StateFlow<NavigationUiState> = _routeState.asStateFlow()

    private var routeJob: Job? = null
    private var lastOrigin: RouteCoordinate? = null
    private var lastDestination: RouteCoordinate? = null

    fun updateRoute(
            originLatitude: Double,
            originLongitude: Double,
            destinationLatitude: Double,
            destinationLongitude: Double
    ) {
        val origin = RouteCoordinate(originLatitude, originLongitude)
        val destination = RouteCoordinate(destinationLatitude, destinationLongitude)
        if (!shouldRequestRoute(origin, destination)) return

        routeJob?.cancel()
        lastOrigin = origin
        lastDestination = destination
        routeJob =
                viewModelScope.launch {
                    _routeState.value = NavigationUiState.Loading
                    when (val result = directionsRepository.getRoute(origin, destination)) {
                        is Result.Success -> {
                            _routeState.value = NavigationUiState.RouteReady(result.data)
                        }
                        is Result.Error -> {
                            _routeState.value =
                                    NavigationUiState.Error(
                                            result.message ?: "Route unavailable."
                                    )
                        }
                        Result.Loading -> Unit
                    }
                }
    }

    fun clearRoute() {
        routeJob?.cancel()
        routeJob = null
        lastOrigin = null
        lastDestination = null
        _routeState.value = NavigationUiState.Idle
    }

    private fun shouldRequestRoute(
            origin: RouteCoordinate,
            destination: RouteCoordinate
    ): Boolean {
        val previousOrigin = lastOrigin ?: return true
        val previousDestination = lastDestination ?: return true

        val destinationMoved = distanceBetween(previousDestination, destination)
        if (destinationMoved >= DESTINATION_CHANGE_METERS) return true

        val originMoved = distanceBetween(previousOrigin, origin)
        if (originMoved >= REROUTE_DISTANCE_METERS) return true

        return false
    }

    private fun distanceBetween(start: RouteCoordinate, end: RouteCoordinate): Float {
        val result = FloatArray(1)
        Location.distanceBetween(
                start.latitude,
                start.longitude,
                end.latitude,
                end.longitude,
                result
        )
        DebugLogger.d(TAG) { "Route distance delta=${result[0]}m" }
        return result[0]
    }
}
