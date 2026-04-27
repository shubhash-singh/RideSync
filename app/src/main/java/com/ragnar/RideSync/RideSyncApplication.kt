package com.ragnar.RideSync

import android.app.Application
import com.ragnar.RideSync.utils.DebugLogger
import dagger.hilt.android.HiltAndroidApp

/**
 * RideSync Application class.
 * Annotated with @HiltAndroidApp to trigger Hilt's code generation
 * and serve as the application-level dependency container.
 */
@HiltAndroidApp
class RideSyncApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DebugLogger.d("RideSyncApplication") { "onCreate" }
    }
}
