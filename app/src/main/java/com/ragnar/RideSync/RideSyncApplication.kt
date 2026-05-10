package com.ragnar.RideSync

import android.app.Application
import com.mapbox.common.MapboxOptions
import com.ragnar.RideSync.utils.DebugLogger
import dagger.hilt.android.HiltAndroidApp

/**
 * RideSync Application class.
 * Annotated with @HiltAndroidApp to trigger Hilt's code generation
 * and serve as the application-level dependency container.
 *
 * Mapbox SDK v11 requires the access token to be set programmatically
 * before the first MapView is inflated. Application.onCreate() is the
 * earliest safe point for this — it runs before any Activity/Compose code.
 */
@HiltAndroidApp
class RideSyncApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialise Mapbox with the public access token from BuildConfig.
        // The token value is injected at build time from local.properties → MAPBOX_ACCESS_TOKEN.
        MapboxOptions.accessToken = BuildConfig.MAPBOX_ACCESS_TOKEN

        DebugLogger.d("RideSyncApplication") { "onCreate — Mapbox token configured" }
    }
}
