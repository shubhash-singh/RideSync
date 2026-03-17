package com.traveltrip

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * TravelTrip Application class.
 * Annotated with @HiltAndroidApp to trigger Hilt's code generation
 * and serve as the application-level dependency container.
 */
@HiltAndroidApp
class TravelTripApplication : Application()
