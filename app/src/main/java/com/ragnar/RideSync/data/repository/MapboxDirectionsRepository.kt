package com.ragnar.RideSync.data.repository

import com.ragnar.RideSync.BuildConfig
import com.ragnar.RideSync.domain.model.Result
import com.ragnar.RideSync.domain.model.RouteCoordinate
import com.ragnar.RideSync.domain.model.RouteInfo
import com.ragnar.RideSync.domain.repository.DirectionsRepository
import com.ragnar.RideSync.utils.DebugLogger
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** Mapbox Directions API implementation used for Phase 10 route drawing. */
@Singleton
class MapboxDirectionsRepository @Inject constructor() : DirectionsRepository {

    private companion object {
        private const val TAG = "MapboxDirectionsRepo"
        private const val BASE_URL = "https://api.mapbox.com/directions/v5/mapbox"
        private const val CONNECT_TIMEOUT_MS = 10_000
        private const val READ_TIMEOUT_MS = 10_000
    }

    override suspend fun getRoute(
            origin: RouteCoordinate,
            destination: RouteCoordinate,
            profile: String
    ): Result<RouteInfo> =
            withContext(Dispatchers.IO) {
                val token = BuildConfig.MAPBOX_ACCESS_TOKEN
                if (token.isBlank()) {
                    return@withContext Result.Error(
                            message = "Mapbox access token is missing. Add MAPBOX_ACCESS_TOKEN to local.properties."
                    )
                }

                val connection =
                        (URL(buildUrl(origin, destination, profile, token)).openConnection()
                                as HttpURLConnection)
                                .apply {
                                    requestMethod = "GET"
                                    connectTimeout = CONNECT_TIMEOUT_MS
                                    readTimeout = READ_TIMEOUT_MS
                                }

                try {
                    val code = connection.responseCode
                    val body = connection.readBody(code)
                    if (code !in 200..299) {
                        val message = parseErrorMessage(body) ?: "Directions request failed ($code)."
                        DebugLogger.w(TAG) { message }
                        return@withContext Result.Error(message = message)
                    }
                    parseRoute(body)
                } catch (e: Exception) {
                    DebugLogger.e(TAG, e) { "Directions request failed: ${e.localizedMessage}" }
                    Result.Error(exception = e, message = e.localizedMessage ?: "Route unavailable.")
                } finally {
                    connection.disconnect()
                }
            }

    private fun buildUrl(
            origin: RouteCoordinate,
            destination: RouteCoordinate,
            profile: String,
            token: String
    ): String {
        val safeProfile = if (profile.isBlank()) "driving" else profile
        val encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8.name())
        return "$BASE_URL/$safeProfile/" +
                "${origin.longitude},${origin.latitude};" +
                "${destination.longitude},${destination.latitude}" +
                "?geometries=geojson&overview=full&steps=false&access_token=$encodedToken"
    }

    private fun HttpURLConnection.readBody(code: Int): String {
        val stream = if (code in 200..299) inputStream else errorStream ?: inputStream
        return stream.bufferedReader().use { it.readText() }
    }

    private fun parseRoute(body: String): Result<RouteInfo> {
        val root = JSONObject(body)
        val apiCode = root.optString("code")
        if (apiCode.isNotBlank() && apiCode != "Ok") {
            return Result.Error(message = root.optString("message", "Directions request failed."))
        }

        val route =
                root.optJSONArray("routes")?.optJSONObject(0)
                        ?: return Result.Error(message = "No route found.")
        val geometry =
                route.optJSONObject("geometry")
                        ?: return Result.Error(message = "Route geometry missing.")
        val coordinatesJson = geometry.optJSONArray("coordinates")
                ?: return Result.Error(message = "Route coordinates missing.")

        val coordinates =
                buildList {
                    for (index in 0 until coordinatesJson.length()) {
                        val pair = coordinatesJson.optJSONArray(index) ?: continue
                        if (pair.length() >= 2) {
                            add(
                                    RouteCoordinate(
                                            latitude = pair.getDouble(1),
                                            longitude = pair.getDouble(0)
                                    )
                            )
                        }
                    }
                }

        if (coordinates.size < 2) {
            return Result.Error(message = "Route has too few points to draw.")
        }

        return Result.Success(
                RouteInfo(
                        coordinates = coordinates,
                        distanceMeters = route.optDouble("distance", 0.0),
                        durationSeconds = route.optDouble("duration", 0.0)
                )
        )
    }

    private fun parseErrorMessage(body: String): String? =
            runCatching {
                        val root = JSONObject(body)
                        root.optString("message").takeIf { it.isNotBlank() }
                    }
                    .getOrNull()
}
