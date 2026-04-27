package com.ragnar.RideSync.utils

import android.util.Log
import com.ragnar.RideSync.BuildConfig

/**
 * Debug-only logger.
 *
 * Logs are emitted only when [BuildConfig.DEBUG] is true, so release builds stay quiet.
 * Use the lambda overloads to avoid building log messages in release builds.
 */
object DebugLogger {
    const val DEFAULT_TAG = "RideSync"

    inline fun d(tag: String = DEFAULT_TAG, crossinline message: () -> String) {
        if (BuildConfig.DEBUG) Log.d(tag, message())
    }

    inline fun i(tag: String = DEFAULT_TAG, crossinline message: () -> String) {
        if (BuildConfig.DEBUG) Log.i(tag, message())
    }

    inline fun w(tag: String = DEFAULT_TAG, crossinline message: () -> String) {
        if (BuildConfig.DEBUG) Log.w(tag, message())
    }

    inline fun w(tag: String = DEFAULT_TAG, tr: Throwable, crossinline message: () -> String) {
        if (BuildConfig.DEBUG) Log.w(tag, message(), tr)
    }

    inline fun e(tag: String = DEFAULT_TAG, crossinline message: () -> String) {
        if (BuildConfig.DEBUG) Log.e(tag, message())
    }

    inline fun e(tag: String = DEFAULT_TAG, tr: Throwable, crossinline message: () -> String) {
        if (BuildConfig.DEBUG) Log.e(tag, message(), tr)
    }
}
