package com.ragnar.RideSync

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * RideSync Application class.
 * Annotated with @HiltAndroidApp to trigger Hilt's code generation
 * and serve as the application-level dependency container.
 */
@HiltAndroidApp
class RideSyncApplication : Application()
